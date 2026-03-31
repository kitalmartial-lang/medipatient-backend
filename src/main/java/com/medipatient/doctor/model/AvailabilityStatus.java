package com.medipatient.doctor.model;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonCreator;

public enum AvailabilityStatus {
    AVAILABLE("available"),
    BUSY("busy"),
    OFFLINE("offline");

    private final String value;

    AvailabilityStatus(String value) {
        this.value = value;
    }

    @JsonValue // Pour Jackson (Envoi/Réception JSON)
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    @JsonCreator // Pour accepter les deux versions dans le JSON
    public static AvailabilityStatus fromString(String value) {
        for (AvailabilityStatus status : AvailabilityStatus.values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        return null;
    }
}