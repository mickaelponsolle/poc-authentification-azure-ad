package io.poc.azureadbackresource.cache;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestTemplate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoleCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoleCache.class);
    private static final String GRAPH_API_URL = "https://graph.microsoft.com/v1.0/me/memberOf";

    private static Map<String, List<String>> rolesMap = new HashMap<>();

    @Scheduled(fixedDelay = 3600000)
    private void flushCache() {
        rolesMap.clear();
    }

    public static List<String> getRoles(String accessToken) {
        String oid = null;
        oid = getOidFromAccessToken(accessToken);

        if (!rolesMap.keySet().contains(oid)) {
            LOGGER.info("Retrieve roles from Azure Graph API for " + oid);
            rolesMap.put(oid, retrieveRolesFromAzureGraphAPI(accessToken));
        } else {
            LOGGER.info("Retrieve roles from cache for " + oid);
        }
        return rolesMap.get(oid);
    }

    private static String getOidFromAccessToken(String accessToken) {
        String oid;
        try {
            DecodedJWT jwt = JWT.decode(accessToken);
            oid = jwt.getClaim("oid").asString();
            LOGGER.info("access token oid : " + oid);

            Date expireDate = jwt.getExpiresAt();
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            LOGGER.info("access token expire at " + simpleDateFormat.format(expireDate));

        } catch (JWTDecodeException exception) {
            throw new RuntimeException(exception);
        }
        return oid;
    }

    private static List<String> retrieveRolesFromAzureGraphAPI(String accessToken) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("authorization", accessToken);
        HttpEntity request = new HttpEntity(headers);
        ResponseEntity<JsonNode> response = restTemplate.exchange(GRAPH_API_URL, HttpMethod.GET, request, JsonNode.class);

        List<String> roles = new ArrayList<>();
        JsonNode values = response.getBody().get("value");
        for (JsonNode value : values) {
            if (value.get("@odata.type").textValue().equals("#microsoft.graph.directoryRole")) {
                roles.add(value.get("displayName").textValue());
            }
        }
        return roles;
    }
}
