package com.navigator.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class WithdrawalCalculationResponse {
    private BigDecimal grossAmount;
    private BigDecimal earlyWithdrawalPenalty;
    private BigDecimal federalTax;
    private BigDecimal stateTax;
    private BigDecimal totalDeductions;
    private BigDecimal netAmount;
    private BigDecimal effectiveTaxRate;
    private BigDecimal mandatoryWithholding;
    private BigDecimal estimatedRefundOrOwed; // positive = refund, negative = owe more
    private String stateNote;
    private String disclaimer;
}
