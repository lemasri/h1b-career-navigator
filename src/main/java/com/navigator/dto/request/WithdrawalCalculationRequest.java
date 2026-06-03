package com.navigator.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WithdrawalCalculationRequest {

    @NotNull(message = "Gross withdrawal amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal grossAmount;

    @NotNull(message = "Please specify if you are under 59.5 years old")
    private boolean under59Half;

    @NotNull(message = "Please specify filing status")
    private boolean marriedFilingJointly;

    // Spouse income affects your tax bracket significantly
    // e.g. $150k spouse income pushes $70k withdrawal into 24% bracket
    private BigDecimal spouseIncome;
}
