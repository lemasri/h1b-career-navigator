package com.navigator.controller;

import com.navigator.dto.request.VisaRequest;
import com.navigator.dto.response.VisaResponse;
import com.navigator.service.VisaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/visas")
@RequiredArgsConstructor
@Tag(name = "Visa Tracker", description = "Track H1B/H4/EAD deadlines and get automated alerts")
public class VisaController {

    private final VisaService visaService;

    @GetMapping
    @Operation(summary = "Get all visas for the authenticated user")
    public ResponseEntity<List<VisaResponse>> getMyVisas(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        return ResponseEntity.ok(visaService.getVisasByUser(userId));
    }

    @GetMapping("/{visaId}")
    @Operation(summary = "Get a specific visa by ID")
    public ResponseEntity<VisaResponse> getVisa(
            @PathVariable UUID visaId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        return ResponseEntity.ok(visaService.getVisaById(visaId, userId));
    }

    @PostMapping
    @Operation(summary = "Add a new visa to track")
    public ResponseEntity<VisaResponse> createVisa(
            @Valid @RequestBody VisaRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        VisaResponse response = visaService.createVisa(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{visaId}")
    @Operation(summary = "Update visa details")
    public ResponseEntity<VisaResponse> updateVisa(
            @PathVariable UUID visaId,
            @Valid @RequestBody VisaRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        return ResponseEntity.ok(visaService.updateVisa(visaId, userId, request));
    }

    @DeleteMapping("/{visaId}")
    @Operation(summary = "Remove a visa record")
    public ResponseEntity<Void> deleteVisa(
            @PathVariable UUID visaId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        visaService.deleteVisa(visaId, userId);
        return ResponseEntity.noContent().build();
    }
}
