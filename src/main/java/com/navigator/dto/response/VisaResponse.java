package com.navigator.dto.response;

import com.navigator.entity.Visa;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class VisaResponse {
    private UUID id;
    private Visa.VisaType visaType;
    private LocalDate startDate;
    private LocalDate expiryDate;
    private long daysUntilExpiry;
    private String caseNumber;
    private String sponsorEmployer;
    private Visa.VisaStatus status;
    private String notes;
    private LocalDateTime createdAt;
}
