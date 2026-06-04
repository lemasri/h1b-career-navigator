package com.navigator.service;

import com.navigator.entity.Visa;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

/**
 * AWS SNS notification service for visa expiry alerts.
 *
 * Circuit breaker pattern added after production incident:
 * When SNS had a 2-hour regional outage, notification failures
 * were propagating up and causing the visa scheduler to fail entirely,
 * marking alerts as "sent" when they weren't.
 *
 * Fix: Wrapped SNS calls in circuit breaker. Fallback logs the failure
 * and does NOT mark alert as sent — so it retries next day.
 * See POSTMORTEM.md — Mistake #2.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final SnsClient snsClient;

    @Value("${aws.sns.topic-arn}")
    private String topicArn;

    /**
     * Sends the alert and reports whether delivery succeeded.
     *
     * Returns {@code true} only when SNS confirms publish. On any failure
     * the circuit breaker fallback returns {@code false}, so the caller can
     * skip marking the alert as sent and let the scheduler retry tomorrow.
     */
    @CircuitBreaker(name = "sns-service", fallbackMethod = "sendVisaExpiryAlertFallback")
    public boolean sendVisaExpiryAlert(Visa visa, int daysRemaining) {
        String message = buildAlertMessage(visa, daysRemaining);

        PublishRequest request = PublishRequest.builder()
                .topicArn(topicArn)
                .subject("⚠️ Visa Expiry Alert — " + daysRemaining + " days remaining")
                .message(message)
                .build();

        snsClient.publish(request);
        log.info("SNS alert sent for visa: {} expiring in {} days", visa.getId(), daysRemaining);
        return true;
    }

    /**
     * Fallback — reports failure so the caller does NOT mark the alert as sent.
     * This ensures the scheduler retries tomorrow.
     * Critical design decision — see ADR-002.
     */
    public boolean sendVisaExpiryAlertFallback(Visa visa, int daysRemaining, Exception ex) {
        log.error("SNS circuit breaker OPEN — failed to send alert for visa: {}. " +
                  "Alert NOT marked as sent — will retry tomorrow. Error: {}",
                  visa.getId(), ex.getMessage());
        // Returning false tells the scheduler to leave the flag unset and retry
        return false;
    }

    private String buildAlertMessage(Visa visa, int daysRemaining) {
        return String.format("""
                H1B Career Navigator — Visa Expiry Alert
                
                Visa Type: %s
                Expiry Date: %s
                Days Remaining: %d
                Case Number: %s
                Sponsor: %s
                
                Action Required:
                %s
                
                Login to your dashboard: https://h1b-navigator.com/dashboard
                """,
                visa.getVisaType(),
                visa.getExpiryDate(),
                daysRemaining,
                visa.getCaseNumber() != null ? visa.getCaseNumber() : "N/A",
                visa.getSponsorEmployer() != null ? visa.getSponsorEmployer() : "N/A",
                getActionRequired(visa.getVisaType(), daysRemaining)
        );
    }

    private String getActionRequired(Visa.VisaType visaType, int daysRemaining) {
        return switch (visaType) {
            case H4_EAD -> daysRemaining <= 60
                    ? "File EAD renewal IMMEDIATELY. Processing takes 4-8 months. No automatic extension."
                    : "Start preparing EAD renewal documents. File at least 6 months before expiry.";
            case H1B -> "Contact your employer's immigration attorney to initiate H1B extension.";
            case H4 -> "H4 extension tied to H1B — ensure your spouse's H1B is also being extended.";
            default -> "Contact your immigration attorney immediately.";
        };
    }
}
