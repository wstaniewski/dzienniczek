package com.mobilernd.dzienniczek.service;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class KidsviewMenuFetcher {

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://app.kidsview.pl")
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Pobiera czysty tekst jadłospisu z KidsView (bez logowania).
     */
    public String fetchMenuText() {

        // KidsView zmienia endpointy — próbujemy oba
        String[] endpoints = {
                "/api/parent/current-menu",
                "/api/menu/current"
        };

        for (String ep : endpoints) {
            try {
                byte[] response = webClient.get()
                        .uri(ep)
                        .retrieve()
                        .bodyToMono(byte[].class)
                        .block();

                JsonNode root = objectMapper.readTree(response);

                JsonNode menuTextNode = root.get("menuText");
                if (menuTextNode != null && !menuTextNode.isNull()) {
                    return menuTextNode.asString();
                }

            } catch (Exception ignored) {
                // próbujemy kolejny endpoint
            }
        }

        throw new IllegalStateException("KidsView: nie znaleziono jadłospisu w żadnym endpointzie");
    }
}