package com.campaignworkbench.adobecampaignapi;

import com.campaignworkbench.adobecampaignapi.schemas.*;
import com.campaignworkbench.ide.IdeException;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
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
import java.net.URI;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Optional;

public class CampaignServerManager {
    private final AuthClient authClient = new AuthClient();
    private SoapClient soapClient;
    private CampaignInstance campaignInstance;
    private final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    private final XmlMapper mapper = new XmlMapper();

    // Maintain a single list of static blocks and JavaScript templates
    private PersoBlockSchema allPersonalizationBlocks = new PersoBlockSchema();
    private EtmModuleSchema allJavaScriptTemplates = new EtmModuleSchema();
    private FolderSchema allBlockFolders = new FolderSchema();
    private EntitySchemasSchema allSchemas = new EntitySchemasSchema();

    private final ObjectProperty<ConnectedStatus> connectedStatusProperty =
            new SimpleObjectProperty<>(new ConnectedStatus(false, null));

    public void setCampaignInstance(CampaignInstance instance) {
        this.campaignInstance = instance;
    }

    public ObjectProperty<ConnectedStatus> getConnectedStatusObservable() {
        return connectedStatusProperty;
    }

    public boolean connect(char[] credentialsPassword) throws ApiException, KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        if (campaignInstance == null) {
            throw new ApiException("No Campaign instance is configured for this workspace.", null);
        }
        campaignInstance.getCredentialStore().unlock(credentialsPassword);
        CredentialStore credentials = campaignInstance.getCredentialStore();
        String endPointUrl = campaignInstance.getEndpointUrl();
        Optional<String> clientId = credentials.getClientId();
        Optional<String> clientSecret = credentials.getClientSecret();

        // Get authentication token
        String accessToken;
        try {
            if (clientId.isEmpty() || clientSecret.isEmpty() || endPointUrl.isEmpty()) {
                throw new ApiException("Could not authenticate with the provided credentials! Endpoint URL, client_id or client_secret are empty!", null);
            }
            accessToken = authClient.getAccessToken(clientId.get(), clientSecret.get());
        } catch (IOException | InterruptedException | IdeException e) {
            throw new ApiException("Could not authenticate with the provided credentials!", e);
        }
        // Connect to the Campaign SOAP instance
        soapClient = new SoapClient(endPointUrl, accessToken);

        // Refresh the object cache
        refreshAll();
        Platform.runLater(() -> connectedStatusProperty.set(new ConnectedStatus(true, campaignInstance)));
        return true;
    }

    public void disconnect() throws ApiException {
        if (soapClient == null) {
            return;
        }
        try {
            soapClient.client.close();
            Platform.runLater(() -> connectedStatusProperty.set(new ConnectedStatus(false, campaignInstance)));

        } catch (Exception exception) {
            throw new ApiException("Could not disconnect from the Campaign SOAP instance!", exception);
        }
    }

    public static String getHostName(String endPointUrl) {
        return URI.create(endPointUrl).getHost().split("\\.")[0];
    }

    public PersoBlockSchema getAllPersoBlocks(boolean refresh) throws ApiException {
        if (!allPersonalizationBlocks.isInitialized() || refresh) {
            refreshBlocks();
        }
        return allPersonalizationBlocks;
    }

    public EtmModuleSchema getAllJavaScriptTemplates(boolean refresh) throws ApiException {
        if (!allJavaScriptTemplates.isInitialized() || refresh) {
            refreshJavaScriptTemplates();
        }
        return allJavaScriptTemplates;
    }

    public FolderSchema getAllBlockFolders(boolean refresh) throws ApiException {
        if (!allBlockFolders.isInitialized() || refresh) {
            refreshBlockFolders();
        }
        return allBlockFolders;
    }

    public EntitySchemasSchema getAllSchemas(boolean refresh) throws ApiException {
        if (!allSchemas.isInitialized() || refresh) {
            refreshSchemas();
        }
        return allSchemas;
    }

    public void refreshBlocks() throws ApiException {
        String innerResultXml = querySchema(PersoBlockSchema.getQueryXml());
        allPersonalizationBlocks = mapper.readValue(innerResultXml, PersoBlockSchema.class);
    }

    public void refreshJavaScriptTemplates() throws ApiException {
        String innerResultXml = querySchema(EtmModuleSchema.getQueryXml());
        allJavaScriptTemplates = mapper.readValue(innerResultXml, EtmModuleSchema.class);
    }

    public void refreshBlockFolders() throws ApiException {
        String innerResultXml = querySchema(FolderSchema.getQueryXml());
        allBlockFolders = mapper.readValue(innerResultXml, FolderSchema.class);
    }

    public void refreshSchemas() throws ApiException {
        String innerResultXml = querySchema(EntitySchemasSchema.getQueryXml());
        allSchemas = mapper.readValue(innerResultXml, EntitySchemasSchema.class);
    }

    public void refreshAll() throws ApiException {
        refreshBlocks();
        refreshJavaScriptTemplates();
        refreshBlockFolders();
        refreshSchemas();
    }

    public Optional<PersoBlockRecord> getPersonalizationBlock(PersoBlockSchemaKey blockKey) {
        return getPersonalizationBlock(blockKey.getId());
    }

    public Optional<PersoBlockRecord> getPersonalizationBlock(long id) {
        if (allPersonalizationBlocks == null || allPersonalizationBlocks.getPersonalisationBlocks() == null) {
            return Optional.empty();
        }
        return allPersonalizationBlocks.getPersonalisationBlocks().stream()
                .filter(iv -> iv.getId() == id)
                .findFirst();
    }

    public Optional<EtmModuleRecord> getJavaScriptTemplate(EtmModuleSchemaKey templateKey) {
        return getJavaScriptTemplate(templateKey.getNamespace(), templateKey.getName());
    }

    private Optional<EtmModuleRecord> getJavaScriptTemplate(String namespace, String name) {
        if (allJavaScriptTemplates == null) {
            return Optional.empty();
        }
        return allJavaScriptTemplates.getJavaScriptTemplates().stream()
                .filter(t -> t.getNamespace().equals(namespace) && t.getName().equals(name))
                .findFirst();
    }

    private String querySchema(String queryXml) {
        String queryResultXml;
        try {
            queryResultXml = soapClient.sendQueryRequest(queryXml);
        } catch (ApiException apiException) {
            throw new ApiException("An error occurred while running a query:\n\n" + queryXml, apiException);
        }

        try {
            factory.setNamespaceAware(true);
            Document doc = factory
                    .newDocumentBuilder()
                    .parse(new ByteArrayInputStream(queryResultXml.getBytes()));

            NodeList nodes = doc.getElementsByTagName("pdomOutput");
            Element pdomOutput = (Element) nodes.item(0);
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

    /**
     * Checks whether a personalization block with the given namespace and name already exists
     * in the cached block list. Requires a prior refresh to be current.
     */
    public boolean personalizationBlockExists(String name) {
        if (allPersonalizationBlocks == null || allPersonalizationBlocks.getPersonalisationBlocks() == null) {
            return false;
        }
        return allPersonalizationBlocks.getPersonalisationBlocks().stream()
                .anyMatch(b -> name.equals(b.getName()));
    }

    /**
     * Checks whether a JavaScript template with the given namespace and name already exists
     * in the cached template list. Requires a prior refresh to be current.
     */
    public boolean javaScriptTemplateExists(String namespace, String name) {
        return getJavaScriptTemplate(namespace, name).isPresent();
    }

    /**
     * Creates a new personalization block on the server, then refreshes the local cache
     * and returns the newly created record.
     */
    public PersoBlockRecord createPersonalizationBlock(String name, String label,
                                                       long folderId, String code) throws ApiException {
        String createXml = PersoBlockSchema.getCreateXml(name, label, folderId, code);
        try {
            String response = soapClient.sendUpdateRequest(createXml);
        } catch (ApiException apiException) {
            throw new ApiException("An error occurred while creating personalization block '" + name + "'", apiException);
        }

        // Refresh cache and locate the newly created record by name
        refreshBlocks();
        return allPersonalizationBlocks.getPersonalisationBlocks().stream()
                .filter(b -> name.equals(b.getName()))
                .findFirst()
                .orElseThrow(() -> new ApiException(
                        "Block '" + name + "' was sent to the server but could not be found after refresh", null));
    }

    /**
     * Creates a new JavaScript template on the server, then refreshes the local cache
     * and returns the newly created record.
     */
    public EtmModuleRecord createJavaScriptTemplate(String namespace, String name, String label,
                                                    String schema, String code) throws ApiException {
        String createXml = EtmModuleSchema.getCreateXml(label, name, namespace, schema, code);
        try {
            soapClient.sendUpdateRequest(createXml);
        } catch (ApiException apiException) {
            throw new ApiException("An error occurred while creating JavaScript template '" + namespace + ":" + name + "'", apiException);
        }

        // Refresh cache and locate the newly created record by namespace + name
        refreshJavaScriptTemplates();
        return getJavaScriptTemplate(namespace, name)
                .orElseThrow(() -> new ApiException(
                        "Module '" + namespace + ":" + name + "' was sent to the server but could not be found after refresh", null));
    }


    public void updatePersonalizationBlock(PersoBlockSchemaKey key, String code) throws ApiException {
        String updateXml = PersoBlockSchema.getUpdateXml(key, code);
        try {
            soapClient.sendUpdateRequest(updateXml);
        } catch (ApiException apiException) {
            throw new ApiException("An error occurred while updating Personalisation Block with key: " + key.getId(), apiException);
        }
    }

    public void updateJavascriptTemplate(EtmModuleSchemaKey key, String code) throws ApiException {
        String updateXml = EtmModuleSchema.getUpdateXml(key, code);
        try {
            soapClient.sendUpdateRequest(updateXml);
        } catch (ApiException apiException) {
            throw new ApiException("An error occurred while updating JavaScriptTemplate with key: " + key.getNamespace() + key.getName(), apiException);
        }
    }
}