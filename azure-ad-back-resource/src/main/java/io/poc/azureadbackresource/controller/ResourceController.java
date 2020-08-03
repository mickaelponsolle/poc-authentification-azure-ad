package io.poc.azureadbackresource.controller;

import io.poc.azureadbackresource.cache.RoleCache;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ResourceController {

    @GetMapping("/")
    public String getResource(@RequestHeader("authorization") String accessToken) {
        List<String> roles = RoleCache.getRoles(accessToken);
        if (roles.contains("Application Developer")) {
            return "Back resource access";
        }
        return "No access";
    }


}
