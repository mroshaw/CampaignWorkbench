package com.campaignworkbench.adobecampaignapi;

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

    public String sendQueryRequest(String queryXml) throws IOException, InterruptedException {
        String soapBody = """
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

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endPoint))
                .header("Content-Type", "text/xml;charset=UTF-8")
                .header("Authorization", "Bearer " + authToken)
                .header("SOAPAction", "xtk:queryDef#ExecuteQuery")
                .POST(HttpRequest.BodyPublishers.ofString(soapBody))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // System.out.println("Status code: " + response.statusCode());
        // System.out.println("Response body:");
        // System.out.println(response.body());

        return response.body();
    }
}
