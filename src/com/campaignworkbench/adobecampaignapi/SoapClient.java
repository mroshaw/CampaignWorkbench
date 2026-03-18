package com.campaignworkbench.adobecampaignapi;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class SoapClient {
    HttpClient client;
    String authToken;
    String endPoint;

    public SoapClient(String endPoint, String authToken) {
        this.client = HttpClient.newHttpClient();
        this.authToken = authToken;
        this.endPoint = endPoint;
    }

    public String sendUpdateRequest(String updateXml) {
        String soapBodyXml = """
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                                  xmlns:urn="urn:xtk:session">
                   <soapenv:Header/>
                   <soapenv:Body>
                      <urn:Write>
                        <sessiontoken></sessiontoken>
                        <urn:domDoc>
                            %s
                        </urn:domDoc>
                        </urn:Write>
                   </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(updateXml);
        return sendRequest(soapBodyXml, "xtk:persist#Write");
    }

    public String sendQueryRequest(String queryXml) {
        String soapBodyXml = """
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                                  xmlns:urn="urn:xtk:queryDef">
                   <soapenv:Header/>
                   <soapenv:Body>
                      <urn:ExecuteQuery>
                         <sessiontoken></sessiontoken>
                         <entity>
                            %s
                         </entity>
                      </urn:ExecuteQuery>
                   </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(queryXml);

        return sendRequest(soapBodyXml, "xtk:queryDef#ExecuteQuery");
    }

    private String sendRequest(String soapBodyXml, String soapAction) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endPoint))
                    .header("Content-Type", "text/xml;charset=UTF-8")
                    .header("Authorization", "Bearer " + authToken)
                    .header("SOAPAction", soapAction)
                    .POST(HttpRequest.BodyPublishers.ofString(soapBodyXml))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if(response.statusCode() != 200) {
                throw new ApiException("An error occurred while sending the SOAP request. StatusCode: " + response.statusCode() + ". Response: " + response.body(), null);
            }

            checkForSoapFault(response.body());

            return response.body();
        } catch (InterruptedException | IOException sendException) {
            throw new ApiException("An error occurred while sending the SOAP request!", sendException);
        }
    }

    /**
     * Checks the response body for a SOAP fault element.
     * If found, throws an ApiException with the fault detail text.
     * If the XML cannot be parsed, the response is passed through unchanged.
     */
    private void checkForSoapFault(String responseBody) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            Document doc = factory
                    .newDocumentBuilder()
                    .parse(new ByteArrayInputStream(responseBody.getBytes()));

            NodeList faultNodes = doc.getElementsByTagName("SOAP-ENV:Fault");
            if (faultNodes.getLength() == 0) {
                return;
            }

            // Extract the detail text — fall back to faultstring if detail is absent
            NodeList detailNodes = doc.getElementsByTagName("detail");
            NodeList faultStringNodes = doc.getElementsByTagName("faultstring");

            String message;
            if (detailNodes.getLength() > 0 && detailNodes.item(0).getTextContent() != null) {
                message = detailNodes.item(0).getTextContent().trim();
            } else if (faultStringNodes.getLength() > 0) {
                message = faultStringNodes.item(0).getTextContent().trim();
            } else {
                message = "An unknown SOAP fault occurred.";
            }

            throw new ApiException(message, null);

        } catch (ApiException apiException) {
            throw apiException;
        } catch (Exception parseException) {
            // If we can't parse the response, pass through without throwing
        }
    }

}
