package com.campaignworkbench.adobecampaignapi;

import com.campaignworkbench.adobecampaignapi.schemas.PersonalizationBlock;
import com.campaignworkbench.adobecampaignapi.schemas.PersonalizationBlockCollection;
import com.campaignworkbench.adobecampaignapi.schemas.JavaScriptTemplate;
import com.campaignworkbench.adobecampaignapi.schemas.JavaScriptTemplateCollection;
import com.campaignworkbench.ide.IdeException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import tools.jackson.dataformat.xml.XmlMapper;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Optional;

public class CampaignServerManager {
    private static final AuthClient authClient = new AuthClient();
    private static SoapClient soapClient;
    private static final CredentialStore credentials = new CredentialStore();
    private static final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    private static final XmlMapper mapper = new XmlMapper();

    // Maintain a single list of static blocks and JavaScript templates
    private static PersonalizationBlockCollection allPersonalizationBlocks = new PersonalizationBlockCollection();
    private static JavaScriptTemplateCollection allJavaScriptTemplates = new JavaScriptTemplateCollection();

    public static boolean connect() throws IdeException {
        Optional<String> endPointUrl = credentials.getEndpointUrl();
        Optional<String> clientId = credentials.getClientId();
        Optional<String> clientSecret = credentials.getClientSecret();

        // Get authentication token
        String accessToken;
        try {
            if (clientId.isEmpty() || clientSecret.isEmpty() || endPointUrl.isEmpty()) {
                throw new IdeException("Could not authenticate with the provided credentials! Endpoint URL, client_id or client_secret are empty!", null);
            }
            accessToken = authClient.getAccessToken(clientId.get(), clientSecret.get());
        } catch (IOException | InterruptedException e) {
            throw new IdeException("Could not authenticate with the provided credentials!", e);
        }

        // Connect to the Campaign SOAP instance
        soapClient = new SoapClient(endPointUrl.get(), accessToken);
        return true;
    }

    public static boolean disconnect() {
        soapClient.client.close();
        return true;
    }

    public static String getEndpointUrl() {
        Optional<String> endPointUrl = credentials.getEndpointUrl();
        return endPointUrl.orElse("");
    }

    public static void updateCredentials(String clientId, String clientSecret, String endpointUrl) throws IdeException {
        try {
            credentials.save(clientId, clientSecret, endpointUrl);
        } catch (RuntimeException e) {
            throw new IdeException("Could not save credentials!", e);
        }
    }

    public static PersonalizationBlockCollection getAllPersoBlocks(boolean refresh) throws IdeException {
        if (!allPersonalizationBlocks.isInitialized() || refresh) {
            refreshBlocks();

        }
        return allPersonalizationBlocks;
    }

    public static JavaScriptTemplateCollection getAllJavaScriptTemplates(boolean refresh) throws IdeException {
        if(!allJavaScriptTemplates.isInitialized() || refresh) {
            refreshJavaScriptTemplates();
        }
        return allJavaScriptTemplates;
    }

    public static void refreshBlocks() throws IdeException {
        String innerResultXml = querySchema(PersonalizationBlockCollection.getQueryXml());
        allPersonalizationBlocks = mapper.readValue(innerResultXml, PersonalizationBlockCollection.class);
    }

    public static void refreshJavaScriptTemplates() throws IdeException {
        String innerResultXml = querySchema(JavaScriptTemplateCollection.getQueryXml());
        allJavaScriptTemplates = mapper.readValue(innerResultXml, JavaScriptTemplateCollection.class);
    }

    public static void refreshAll() throws IdeException {
        refreshBlocks();
        refreshJavaScriptTemplates();
    }

    public static Optional<PersonalizationBlock> getPersonalizationBlock(long id) {
        if (allPersonalizationBlocks == null || allPersonalizationBlocks.getPersonalisationBlocks() == null) {
            return Optional.empty();
        }

        return allPersonalizationBlocks.getPersonalisationBlocks().stream()
                .filter(iv -> iv.getId() == id)
                .findFirst();
    }

    public static Optional<JavaScriptTemplate> getJavaScriptTemplate(String namespace, String name) {
        if (allJavaScriptTemplates == null) {
            return Optional.empty();
        }

        return allJavaScriptTemplates.getJavaScriptTemplates().stream()
                .filter(javascriptTemplate -> javascriptTemplate.getNamespace().equals(namespace) && javascriptTemplate.getName().equals(name))
                .findFirst();
    }



    private static String querySchema(String queryXml) throws IdeException {
        // Run a queryDef against the Campaign server
        String queryResultXml;
        try {
            queryResultXml = soapClient.sendQueryRequest(queryXml);
        } catch (IOException | InterruptedException exception) {
            throw new IdeException("An error occurred while querying Personalisation Blocks!", exception);
        }

        // Deserialize the result to object instances
        try {
            factory.setNamespaceAware(true);
            Document doc = factory
                    .newDocumentBuilder()
                    .parse(new ByteArrayInputStream(queryResultXml.getBytes()));

            NodeList nodes = doc.getElementsByTagName("pdomOutput");
            Element pdomOutput = (Element) nodes.item(0);

            // Get first child (includeView-collection)
            Node collectionNode = pdomOutput.getFirstChild();

            return nodeToString(collectionNode);

        } catch (Exception exception) {
            throw new IdeException("An error occurred while processing the results of the query: " + queryResultXml, exception);
        }
    }

    private static String nodeToString(Node node) throws Exception {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(node), new StreamResult(writer));
        return writer.toString();
    }

}
