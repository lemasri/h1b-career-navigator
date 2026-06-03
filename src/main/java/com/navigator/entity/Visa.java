package com.navigator.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tracks H1B/H4/EAD visa information and key deadlines.
 *
 * Design note: Used PostgreSQL (not DynamoDB) because visa data is
 * relational — a user has multiple visas, each linked to jobs and
 * financial records. Complex joins are frequent. See ADR-001.
 */
@Entity
@Table(name = "visas")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Visa {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VisaType visaType;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate expiryDate;

    private String caseNumber;

    private String sponsorEmployer;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private VisaStatus status = VisaStatus.ACTIVE;

    // Reminder tracking — ensures we don't spam alerts
    // Explicit column names: the default naming strategy maps e.g. alert90DaySent
    // -> alert90day_sent (no underscore around the digit), which would not match
    // the alert_90_day_sent columns defined in V1__initial_schema.sql.
    @Column(name = "alert_90_day_sent")
    private Boolean alert90DaySent;
    @Column(name = "alert_60_day_sent")
    private Boolean alert60DaySent;
    @Column(name = "alert_30_day_sent")
    private Boolean alert30DaySent;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum VisaType {
        H1B, H4, H4_EAD, F1, OPT, CPT, GREEN_CARD, CITIZEN
    }

    public enum VisaStatus {
        ACTIVE, EXPIRED, PENDING, CANCELLED
    }

    /**
     * Days until expiry — used by scheduler for alert decisions.
     */
    public long daysUntilExpiry() {
        return java.time.temporal.ChronoUnit.DAYS.between(
            LocalDate.now(), this.expiryDate
        );
    }
}
