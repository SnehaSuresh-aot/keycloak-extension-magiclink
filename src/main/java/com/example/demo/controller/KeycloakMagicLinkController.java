package com.example.demo.controller;

import com.example.demo.service.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Controller
@RequestMapping("/auth")
public class KeycloakMagicLinkController {

    @Value("${keycloak.server-url}")
    private String keycloakServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    @Value("${app.magic-link-base-url}")
    private String magicLinkBaseUrl;

    @Autowired
    private MailService mailService;

    private final Map<String, String> tokenStore = new ConcurrentHashMap<>(); // Replace with DB in production

        @GetMapping("/login")
        public String showLoginPage() {
            return "login"; // Renders `login.html` in Thymeleaf
        }

        @PostMapping("/generate-magic-link")
        public String sendMagicLink(@RequestParam String email, Model model) {
            String magicToken = UUID.randomUUID().toString();
            tokenStore.put(magicToken, email); // Store token temporarily
            String token = authenticateWithKeycloak(email);
            String magicLink = magicLinkBaseUrl + "/auth/magic-login?token=" + token;

            mailService.sendSimpleEmail(email, "Your Magic Login Link", "Click here to log in: " + magicLink);
            model.addAttribute("successMessage", "Magic link has been sent to your email: " + email);
            return "login";
        }

        @GetMapping("/magic-login")
        public String handleMagicLogin(@RequestParam String token, Model model) {
            String email = tokenStore.get(token);
            if (email == null) {
                model.addAttribute("error", "Invalid or expired magic link.");
                return "error";
            }

            String accessToken = authenticateWithKeycloak(email);
            if (accessToken == null) {
                model.addAttribute("error", "Authentication failed.");
                return "error";
            }

            model.addAttribute("message", "Login successful!");
            model.addAttribute("accessToken", accessToken);
            return "dashboard"; // Renders `dashboard.html` in Thymeleaf
        }

        private String authenticateWithKeycloak(String email) {
            RestTemplate restTemplate = new RestTemplate();

            String tokenEndpoint = keycloakServerUrl + "/realms/" + realm + "/protocol/openid-connect/token";

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "client_credentials");
            params.add("client_id", clientId);
            params.add("client_secret", clientSecret);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(tokenEndpoint, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody().get("access_token").toString();
            } else {
                return null;
            }
        }

    }
