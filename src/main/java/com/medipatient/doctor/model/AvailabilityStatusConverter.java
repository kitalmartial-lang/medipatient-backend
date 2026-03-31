package com.medipatient.doctor.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AvailabilityStatusConverter implements AttributeConverter<AvailabilityStatus, String> {

    @Override
    public String convertToDatabaseColumn(AvailabilityStatus attribute) {
        if (attribute == null) return null;
        return attribute.name().toLowerCase(); // Transforme AVAILABLE en available
    }

    @Override
    public AvailabilityStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return AvailabilityStatus.valueOf(dbData.toUpperCase()); // Transforme available en AVAILABLE
    }
}