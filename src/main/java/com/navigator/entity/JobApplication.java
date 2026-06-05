package com.navigator.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "job_applications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String companyName;

    @Column(nullable = false)
    private String roleTitle;

    private String jobUrl;

    private String recruiterName;

    private String recruiterEmail;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.APPLIED;

    private LocalDate appliedDate;

    private LocalDate nextInterviewDate;

    // H1B sponsorship tracking — critical for your situation
    private Boolean sponsorsH1b;

    private Integer salaryRangeMin;

    private Integer salaryRangeMax;

    private String referredBy;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum ApplicationStatus {
        APPLIED,
        PHONE_SCREEN,
        TECHNICAL_ROUND_1,
        TECHNICAL_ROUND_2,
        SYSTEM_DESIGN,
        LEADERSHIP_ROUND,
        OFFER_RECEIVED,
        OFFER_ACCEPTED,
        OFFER_DECLINED,
        REJECTED,
        WITHDRAWN
    }
}
