package com.navigator.dto.request;

import com.navigator.entity.Visa;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class VisaRequest {

    @NotNull(message = "Visa type is required")
    private Visa.VisaType visaType;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "Expiry date is required")
    private LocalDate expiryDate;

    private String caseNumber;
    private String sponsorEmployer;
    private String notes;
}
