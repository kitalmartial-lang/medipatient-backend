package com.medipatient.voice.controller;

import com.medipatient.voice.dto.VoiceRequest;
import com.medipatient.voice.dto.VoiceResponse;
import com.medipatient.voice.service.VoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/voice")
@CrossOrigin(origins = "http://localhost:8080")
@RequiredArgsConstructor
@Slf4j
public class VoiceController {

    private final VoiceService voiceService;

    @PostMapping("/chat")
    public ResponseEntity<VoiceResponse> processVoiceCommand(@RequestBody VoiceRequest request) {
        log.info("Message vocal reçu : {}", request.getText());

        if (request.getText() == null || request.getText().trim().isEmpty()) {
            VoiceResponse emptyResponse = new VoiceResponse();
            emptyResponse.setMessageVocal("Je n'ai rien entendu, pouvez-vous répéter ?");
            emptyResponse.setActionNavigation("none");
            return ResponseEntity.badRequest().body(emptyResponse);
        }

        VoiceResponse response = voiceService.analyzeWithAI(request.getText());
        return ResponseEntity.ok(response);
    }
}