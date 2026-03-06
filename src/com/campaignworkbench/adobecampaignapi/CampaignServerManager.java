package com.campaignworkbench.adobecampaignapi;

import com.campaignworkbench.adobecampaignapi.schemas.*;
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
    private final AuthClient authClient = new AuthClient();
    private SoapClient soapClient;
    private final CredentialStore credentials = new CredentialStore();
    private final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    private final XmlMapper mapper = new XmlMapper();

    // Maintain a single list of static blocks and JavaScript templates
    private PersonalizationBlockCollection allPersonalizationBlocks = new PersonalizationBlockCollection();
    private JavaScriptTemplateCollection allJavaScriptTemplates = new JavaScriptTemplateCollection();

    public boolean connect() throws ApiException {
        Optional<String> endPointUrl = credentials.getEndpointUrl();
        Optional<String> clientId = credentials.getClientId();
        Optional<String> clientSecret = credentials.getClientSecret();

        // Get authentication token
        String accessToken;
        try {
            if (clientId.isEmpty() || clientSecret.isEmpty() || endPointUrl.isEmpty()) {
                throw new ApiException("Could not authenticate with the provided credentials! Endpoint URL, client_id or client_secret are empty!", null);
            }
            accessToken = authClient.getAccessToken(clientId.get(), clientSecret.get());
        } catch (IOException | InterruptedException e) {
            throw new ApiException("Could not authenticate with the provided credentials!", e);
        }

        // Connect to the Campaign SOAP instance
        soapClient = new SoapClient(endPointUrl.get(), accessToken);
        return true;
    }

    public void disconnect() throws ApiException {
        try {
            soapClient.client.close();
          } catch (Exception exception) {
            throw new ApiException("Could not disconnect from the Campaign SOAP instance!", exception);
        }
    }

    public String getEndpointUrl() {
        Optional<String> endPointUrl = credentials.getEndpointUrl();
        return endPointUrl.orElse("");
    }

    public PersonalizationBlockCollection getAllPersoBlocks(boolean refresh) throws ApiException {
        if (!allPersonalizationBlocks.isInitialized() || refresh) {
            refreshBlocks();

        }
        return allPersonalizationBlocks;
    }

    public JavaScriptTemplateCollection getAllJavaScriptTemplates(boolean refresh) throws ApiException {
        if(!allJavaScriptTemplates.isInitialized() || refresh) {
            refreshJavaScriptTemplates();
        }
        return allJavaScriptTemplates;
    }

    public void refreshBlocks() throws ApiException {
        String innerResultXml = querySchema(PersonalizationBlockCollection.getQueryXml());
        allPersonalizationBlocks = mapper.readValue(innerResultXml, PersonalizationBlockCollection.class);
    }

    public void refreshJavaScriptTemplates() throws ApiException {
        String innerResultXml = querySchema(JavaScriptTemplateCollection.getQueryXml());
        allJavaScriptTemplates = mapper.readValue(innerResultXml, JavaScriptTemplateCollection.class);
    }

    public void refreshAll() throws ApiException {
        refreshBlocks();
        refreshJavaScriptTemplates();
    }

    public Optional<PersonalizationBlock> getPersonalizationBlock(long id) {
        if (allPersonalizationBlocks == null || allPersonalizationBlocks.getPersonalisationBlocks() == null) {
            return Optional.empty();
        }

        return allPersonalizationBlocks.getPersonalisationBlocks().stream()
                .filter(iv -> iv.getId() == id)
                .findFirst();
    }

    public Optional<JavaScriptTemplate> getJavaScriptTemplate(String namespace, String name) {
        if (allJavaScriptTemplates == null) {
            return Optional.empty();
        }

        return allJavaScriptTemplates.getJavaScriptTemplates().stream()
                .filter(javascriptTemplate -> javascriptTemplate.getNamespace().equals(namespace) && javascriptTemplate.getName().equals(name))
                .findFirst();
    }

    private String querySchema(String queryXml) {
        // Run a queryDef against the Campaign server
        String queryResultXml;
        try {
            queryResultXml = soapClient.sendQueryRequest(queryXml);
        } catch (ApiException apiException) {
            throw new ApiException("An error occurred while querying Personalisation Blocks!", apiException);
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
            throw new ApiException("An error occurred while processing the results of the query: " + queryResultXml, exception);
        }
    }

    private String nodeToString(Node node) throws Exception {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(node), new StreamResult(writer));
        return writer.toString();
    }

    public void updatePersonalizationBlock(PersonalizationBlockKey key, String code) throws ApiException {
        String updateXml = PersonalizationBlockCollection.getUpdateXml(key, code);
        String updateResultXml;
        try {
            updateResultXml = soapClient.sendUpdateRequest(updateXml);
        } catch (ApiException apiException) {
            throw new ApiException("An error occurred while updating Personalisation Block with key: " + key.getId(), apiException);
        }
    }

    public void updateJavascriptTemplate(JavaScriptTemplateKey key, String code) throws ApiException {
        String updateXml = JavaScriptTemplateCollection.getUpdateXml(key, code);
        String updateResultXml;
        try {
            updateResultXml = soapClient.sendUpdateRequest(updateXml);
        } catch (ApiException apiException) {
            throw new ApiException("An error occurred while updating JavaScriptTemplate with key: " + key.getNamespace() + key.getName(), apiException);
        }
    }
}
