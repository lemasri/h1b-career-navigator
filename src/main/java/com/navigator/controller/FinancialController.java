package com.navigator.controller;

import com.navigator.dto.request.WithdrawalCalculationRequest;
import com.navigator.dto.response.WithdrawalCalculationResponse;
import com.navigator.service.FinancialCalculatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/financial")
@RequiredArgsConstructor
@Tag(name = "Financial Calculator",
     description = "401k withdrawal, NRE/NRO FD comparison, US-India transfer calculations")
public class FinancialController {

    private final FinancialCalculatorService financialCalculatorService;

    @PostMapping("/401k/withdrawal")
    @Operation(
        summary = "Calculate 401k early withdrawal net amount",
        description = """
            Calculates the net amount you receive after federal taxes and 10% early 
            withdrawal penalty. Accounts for married filing jointly with spouse income,
            which significantly affects your tax bracket.
            Washington state residents benefit from 0% state income tax.
            """
    )
    public ResponseEntity<WithdrawalCalculationResponse> calculate401kWithdrawal(
            @Valid @RequestBody WithdrawalCalculationRequest request) {
        return ResponseEntity.ok(
                financialCalculatorService.calculate401kWithdrawal(request)
        );
    }
}
