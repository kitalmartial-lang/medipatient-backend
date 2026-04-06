package com.medipatient.voice.dto;
import lombok.Data;

@Data
public class VoiceRequest {
    private String text;
    private String language;
}
