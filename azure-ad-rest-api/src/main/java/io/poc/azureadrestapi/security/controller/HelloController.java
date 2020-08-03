package io.poc.azureadrestapi.security.controller;

import io.poc.azureadrestapi.security.model.AccessTokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class HelloController {
    private static final Logger LOGGER = LoggerFactory.getLogger(HelloController.class);
    public static final String BACK_RESOURCE_API = "http://localhost:8090";

    private RestTemplate restTemplate = new RestTemplate();

    @Value("${azure.activedirectory.tenant-id}")
    private String tenantId;
    @Value("${spring.security.oauth2.client.registration.azure.client-id}")
    private String clientId;
    @Value("${spring.security.oauth2.client.registration.azure.client-secret}")
    private String clientSecret;


    @PreAuthorize("hasRole('usergrouppocsso')")
    @GetMapping("/")
    public String helloWorld(Model model, OAuth2AuthenticationToken authentication) {
        DefaultOidcUser user = (DefaultOidcUser) authentication.getPrincipal();
        String accessToken = user.getIdToken().getTokenValue();

        LOGGER.info("Try to read Microsoft Graph API with authorization : " + accessToken);
        AccessTokenResponse accessTokenResponse = retrieveAccessTokenForNextApi(accessToken);

        LOGGER.info("Access token to send : " + accessTokenResponse.getAccessToken());
        LOGGER.info("Access token expiration date : " + accessTokenResponse.getExpiresIn());
        LOGGER.info("Refresh token : " + accessTokenResponse.getRefreshToken());
        LOGGER.info("Access token scope : " + accessTokenResponse.getScope());
        LOGGER.info("Access token type : " + accessTokenResponse.getTokenType());

        return retrieveResponseFromNextApi(accessTokenResponse);
    }

    private String retrieveResponseFromNextApi(AccessTokenResponse accessTokenResponse) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("authorization", accessTokenResponse.getAccessToken());
        HttpEntity request = new HttpEntity(headers);
        ResponseEntity<String> response = restTemplate.exchange(BACK_RESOURCE_API, HttpMethod.GET, request, String.class);
        return response.getBody();
    }

    private AccessTokenResponse retrieveAccessTokenForNextApi(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("assertion", accessToken);
        form.add("scope", "https://graph.microsoft.com/user.read");
        form.add("requested_token_use", "on_behalf_of");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

        String tokenUrl = "https://login.microsoftonline.com/" + tenantId + "/oauth2/v2.0/token";
        ResponseEntity<AccessTokenResponse> response = restTemplate.postForEntity(tokenUrl, request, AccessTokenResponse.class);
        return response.getBody();
    }
}