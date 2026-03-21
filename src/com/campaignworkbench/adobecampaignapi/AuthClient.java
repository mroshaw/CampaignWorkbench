package com.campaignworkbench.adobecampaignapi;
import com.campaignworkbench.ide.IdeException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class AuthClient {

    private static final String TOKEN_URL = "https://ims-na1.adobelogin.com/ims/token/v3";
    private static final String SCOPE = "AdobeID,openid,read_organizations,additional_info.projectedProductContext," +
            "additional_info.roles,adobeio_api,read_client_secret,manage_client_secrets," +
            "campaign_sdk,campaign_config_server_general,deliverability_service_general";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public AuthClient() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public String getAccessToken(String clientId, String clientSecret) throws IOException, InterruptedException, IdeException {
        // Build form parameters
        String form = "grant_type=client_credentials" +
                "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8) +
                "&scope=" + URLEncoder.encode(SCOPE, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IdeException("Token request failed: HTTP " + response.statusCode() + ", body=" + response.body(), null);
        }

        AuthResponse authResponse = objectMapper.readValue(response.body(), AuthResponse.class);
        return authResponse.getAccessToken();
    }
}