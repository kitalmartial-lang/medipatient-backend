package com.medipatient.voice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medipatient.voice.dto.VoiceResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class VoiceService {

    @Value("${groq.api-key}") // On récupère la clé depuis application.properties
    private String openAiApiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public VoiceResponse analyzeWithAI(String patientText) {
        // 1. SÉCURITÉ DÉMO : Si pas de clé API, on utilise le mode dégradé (Mock)
        if (openAiApiKey == null || openAiApiKey.isEmpty() || openAiApiKey.equals("TA_CLE_ICI")) {
            log.warn("Aucune clé OpenAI trouvée. Utilisation du mode de secours.");
            return getFallbackResponse(patientText);
        }

        try {
            // 2. Préparation du Prompt Système (Le comportement de l'IA)
            String systemPrompt = "Tu es l'assistant vocal intelligent de Medipatient au Sénégal. " +
                    "Réponds en 1 ou 2 phrases maximum avec empathie. Si le texte contient des mots en wolof, réponds avec des salutations en wolof. " +
                    "Tu dois renvoyer STRICTEMENT un objet JSON avec deux clés : " +
                    "'messageVocal' (ce que tu dis au patient) et " +
                    "'actionNavigation' (choisis parmi : 'appointments', 'prescriptions', 'emergency', 'records', ou 'none').";

            // 3. Construction de la requête pour OpenAI (API ChatGPT)
            String url = "https://api.groq.com/openai/v1/chat/completions";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openAiApiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "llama3-8b-8192"); // Modèle rapide et pas cher
            requestBody.put("response_format", Map.of("type", "json_object")); // Force l'IA à renvoyer du JSON

            requestBody.put("messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", patientText)
            ));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // 4. Appel à l'API OpenAI
            String responseStr = restTemplate.postForObject(url, entity, String.class);

            // 5. Lecture et formatage de la réponse
            JsonNode rootNode = objectMapper.readTree(responseStr);
            String content = rootNode.path("choices").get(0).path("message").path("content").asText();

            JsonNode contentNode = objectMapper.readTree(content);

            VoiceResponse voiceResponse = new VoiceResponse();
            voiceResponse.setMessageVocal(contentNode.path("messageVocal").asText());
            voiceResponse.setActionNavigation(contentNode.path("actionNavigation").asText());

            return voiceResponse;

        } catch (Exception e) {
            log.error("Erreur lors de l'appel à l'IA OpenAI : ", e);
            // En cas de crash réseau de l'IA, on bascule sur le secours !
            return getFallbackResponse(patientText);
        }
    }

    // Le mode de secours (Mock) pour sauver ta présentation si l'IA est hors ligne
    private VoiceResponse getFallbackResponse(String text) {
        VoiceResponse response = new VoiceResponse();
        String lowerText = text.toLowerCase();

        if (lowerText.contains("rendez-vous") || lowerText.contains("rdv")) {
            response.setMessageVocal("Bien sûr, je vous redirige vers la page de prise de rendez-vous.");
            response.setActionNavigation("appointments");
        } else if (lowerText.contains("ordonnance") || lowerText.contains("médicament")) {
            response.setMessageVocal("Très bien, voici la liste de vos ordonnances récentes.");
            response.setActionNavigation("prescriptions");
        } else if (lowerText.contains("urgence") || lowerText.contains("mal")) {
            response.setMessageVocal("Si c'est une urgence grave, veuillez contacter le 15 immédiatement. Je vous affiche les contacts d'urgence.");
            response.setActionNavigation("emergency");
        } else {
            response.setMessageVocal("J'ai bien compris votre message, mais je suis en mode maintenance. Comment puis-je vous aider autrement ?");
            response.setActionNavigation("none");
        }
        return response;
    }
}