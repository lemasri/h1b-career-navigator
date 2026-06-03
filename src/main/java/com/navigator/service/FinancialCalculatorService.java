package com.navigator.service;

import com.navigator.dto.request.WithdrawalCalculationRequest;
import com.navigator.dto.response.WithdrawalCalculationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Financial calculator for immigrant professionals.
 *
 * Built from real personal experience calculating 401k early
 * withdrawal penalties, NRE/NRO FD comparisons, and US-India
 * transfer costs.
 *
 * Note: These are estimates only. Tax situations vary.
 * Always consult a CPA for actual tax advice.
 */
@Service
@Slf4j
public class FinancialCalculatorService {

    private static final BigDecimal EARLY_WITHDRAWAL_PENALTY = new BigDecimal("0.10");
    private static final BigDecimal STANDARD_DEDUCTION_SINGLE = new BigDecimal("14600");
    private static final BigDecimal STANDARD_DEDUCTION_MFJ = new BigDecimal("29200");

    /**
     * Calculates net amount after 401k early withdrawal penalties and taxes.
     *
     * This calculator was born from a real question:
     * "I have $70k in 401k — how much do I actually get if I withdraw now?"
     * Most online calculators don't account for married filing jointly
     * with spouse income pushing you into a higher bracket.
     */
    public WithdrawalCalculationResponse calculate401kWithdrawal(
            WithdrawalCalculationRequest request) {

        BigDecimal grossAmount = request.getGrossAmount();
        BigDecimal spouseIncome = request.getSpouseIncome() != null
                ? request.getSpouseIncome()
                : BigDecimal.ZERO;
        boolean isMarriedFilingJointly = request.isMarriedFilingJointly();
        boolean isUnder59Half = request.isUnder59Half();

        // Step 1: Early withdrawal penalty (10% if under 59.5)
        BigDecimal penalty = isUnder59Half
                ? grossAmount.multiply(EARLY_WITHDRAWAL_PENALTY)
                : BigDecimal.ZERO;

        // Step 2: Calculate federal income tax
        BigDecimal standardDeduction = isMarriedFilingJointly
                ? STANDARD_DEDUCTION_MFJ
                : STANDARD_DEDUCTION_SINGLE;

        BigDecimal totalIncome = grossAmount.add(spouseIncome);
        BigDecimal taxableIncome = totalIncome.subtract(standardDeduction)
                .max(BigDecimal.ZERO);

        BigDecimal federalTax = calculateFederalTax(taxableIncome, isMarriedFilingJointly);

        // Step 3: Apportion tax to withdrawal only (not spouse income)
        // Uses marginal rate on the withdrawal portion
        BigDecimal spouseTaxableIncome = spouseIncome.subtract(standardDeduction)
                .max(BigDecimal.ZERO);
        BigDecimal taxWithoutWithdrawal = calculateFederalTax(spouseTaxableIncome, isMarriedFilingJointly);
        BigDecimal taxAttributedToWithdrawal = federalTax.subtract(taxWithoutWithdrawal)
                .max(BigDecimal.ZERO);

        // Washington state has NO income tax — great for WA residents!
        BigDecimal stateTax = BigDecimal.ZERO;

        BigDecimal totalDeductions = penalty.add(taxAttributedToWithdrawal).add(stateTax);
        BigDecimal netAmount = grossAmount.subtract(totalDeductions);

        // Employer typically withholds 20% upfront
        BigDecimal mandatoryWithholding = grossAmount.multiply(new BigDecimal("0.20"));
        BigDecimal refundOrOwed = mandatoryWithholding.subtract(taxAttributedToWithdrawal);

        return WithdrawalCalculationResponse.builder()
                .grossAmount(grossAmount)
                .earlyWithdrawalPenalty(penalty.setScale(2, RoundingMode.HALF_UP))
                .federalTax(taxAttributedToWithdrawal.setScale(2, RoundingMode.HALF_UP))
                .stateTax(stateTax)
                .totalDeductions(totalDeductions.setScale(2, RoundingMode.HALF_UP))
                .netAmount(netAmount.setScale(2, RoundingMode.HALF_UP))
                .effectiveTaxRate(totalDeductions.divide(grossAmount, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP))
                .mandatoryWithholding(mandatoryWithholding.setScale(2, RoundingMode.HALF_UP))
                .estimatedRefundOrOwed(refundOrOwed.setScale(2, RoundingMode.HALF_UP))
                .stateNote("Washington state has no income tax — 0% state tax applied.")
                .disclaimer("This is an estimate only. Consult a CPA for accurate tax advice.")
                .build();
    }

    /**
     * 2024 Federal Tax Brackets — Married Filing Jointly
     * Bracket calculation uses progressive (marginal) rate system.
     */
    private BigDecimal calculateFederalTax(BigDecimal taxableIncome, boolean mfj) {
        if (taxableIncome.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;

        BigDecimal tax = BigDecimal.ZERO;

        if (mfj) {
            // MFJ 2024 brackets
            tax = tax.add(calculateBracket(taxableIncome, 0, 23200, 0.10));
            tax = tax.add(calculateBracket(taxableIncome, 23200, 94300, 0.12));
            tax = tax.add(calculateBracket(taxableIncome, 94300, 201050, 0.22));
            tax = tax.add(calculateBracket(taxableIncome, 201050, 383900, 0.24));
            tax = tax.add(calculateBracket(taxableIncome, 383900, 487450, 0.32));
            tax = tax.add(calculateBracket(taxableIncome, 487450, 731200, 0.35));
            tax = tax.add(calculateBracket(taxableIncome, 731200, Integer.MAX_VALUE, 0.37));
        } else {
            // Single 2024 brackets
            tax = tax.add(calculateBracket(taxableIncome, 0, 11600, 0.10));
            tax = tax.add(calculateBracket(taxableIncome, 11600, 47150, 0.12));
            tax = tax.add(calculateBracket(taxableIncome, 47150, 100525, 0.22));
            tax = tax.add(calculateBracket(taxableIncome, 100525, 191950, 0.24));
            tax = tax.add(calculateBracket(taxableIncome, 191950, 243725, 0.32));
            tax = tax.add(calculateBracket(taxableIncome, 243725, 609350, 0.35));
            tax = tax.add(calculateBracket(taxableIncome, 609350, Integer.MAX_VALUE, 0.37));
        }

        return tax;
    }

    private BigDecimal calculateBracket(BigDecimal income, long low, long high, double rate) {
        BigDecimal lowBd = BigDecimal.valueOf(low);
        BigDecimal highBd = high == Integer.MAX_VALUE
                ? income
                : BigDecimal.valueOf(high);

        if (income.compareTo(lowBd) <= 0) return BigDecimal.ZERO;

        BigDecimal taxableInBracket = income.min(highBd).subtract(lowBd);
        return taxableInBracket.multiply(BigDecimal.valueOf(rate));
    }
}
