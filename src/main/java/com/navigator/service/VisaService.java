package com.navigator.service;

import com.navigator.dto.request.VisaRequest;
import com.navigator.dto.response.VisaResponse;
import com.navigator.entity.Visa;
import com.navigator.exception.ResourceNotFoundException;
import com.navigator.repository.UserRepository;
import com.navigator.repository.VisaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VisaService {

    private final VisaRepository visaRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /**
     * Cached — visa data doesn't change often.
     * TTL configured in application.yml (default 1 hour).
     * Cache evicted on any update. See ADR-004 for caching strategy.
     */
    @Cacheable(value = "userVisas", key = "#userId")
    @Transactional(readOnly = true)
    public List<VisaResponse> getVisasByUser(UUID userId) {
        log.info("Fetching visas for user: {}", userId);
        return visaRepository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public VisaResponse getVisaById(UUID visaId, UUID userId) {
        Visa visa = visaRepository.findById(visaId)
                .filter(v -> v.getUser().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Visa not found: " + visaId));
        return toResponse(visa);
    }

    @CacheEvict(value = "userVisas", key = "#userId")
    public VisaResponse createVisa(UUID userId, VisaRequest request) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Visa visa = Visa.builder()
                .user(user)
                .visaType(request.getVisaType())
                .startDate(request.getStartDate())
                .expiryDate(request.getExpiryDate())
                .caseNumber(request.getCaseNumber())
                .sponsorEmployer(request.getSponsorEmployer())
                .notes(request.getNotes())
                .alert90DaySent(false)
                .alert60DaySent(false)
                .alert30DaySent(false)
                .build();

        Visa saved = visaRepository.save(visa);
        log.info("Visa created: {} for user: {}", saved.getId(), userId);
        return toResponse(saved);
    }

    @CacheEvict(value = "userVisas", key = "#userId")
    public VisaResponse updateVisa(UUID visaId, UUID userId, VisaRequest request) {
        Visa visa = visaRepository.findById(visaId)
                .filter(v -> v.getUser().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Visa not found: " + visaId));

        visa.setVisaType(request.getVisaType());
        visa.setStartDate(request.getStartDate());
        visa.setExpiryDate(request.getExpiryDate());
        visa.setCaseNumber(request.getCaseNumber());
        visa.setSponsorEmployer(request.getSponsorEmployer());
        visa.setNotes(request.getNotes());

        return toResponse(visaRepository.save(visa));
    }

    @CacheEvict(value = "userVisas", key = "#userId")
    public void deleteVisa(UUID visaId, UUID userId) {
        Visa visa = visaRepository.findById(visaId)
                .filter(v -> v.getUser().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Visa not found: " + visaId));
        visaRepository.delete(visa);
        log.info("Visa deleted: {} for user: {}", visaId, userId);
    }

    /**
     * Scheduled reminder service — runs daily at 9 AM.
     *
     * Lesson learned: Initially ran this hourly — caused duplicate
     * SNS notifications. Switched to daily + alert-sent flags in DB.
     * The boolean flags prevent duplicate alerts even if scheduler
     * runs twice accidentally. See POSTMORTEM.md for full details.
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void sendExpiryReminders() {
        LocalDate today = LocalDate.now();
        log.info("Running visa expiry reminder check: {}", today);

        // 90-day alerts
        List<Visa> visas90 = visaRepository.findVisasNeedingAlert90Day(
                today, today.plusDays(90)
        );
        visas90.forEach(visa -> {
            notificationService.sendVisaExpiryAlert(visa, 90);
            visa.setAlert90DaySent(true);
            visaRepository.save(visa);
        });

        // 60-day alerts
        List<Visa> visas60 = visaRepository.findVisasNeedingAlert60Day(
                today, today.plusDays(60)
        );
        visas60.forEach(visa -> {
            notificationService.sendVisaExpiryAlert(visa, 60);
            visa.setAlert60DaySent(true);
            visaRepository.save(visa);
        });

        // 30-day alerts
        List<Visa> visas30 = visaRepository.findVisasNeedingAlert30Day(
                today, today.plusDays(30)
        );
        visas30.forEach(visa -> {
            notificationService.sendVisaExpiryAlert(visa, 30);
            visa.setAlert30DaySent(true);
            visaRepository.save(visa);
        });

        log.info("Reminder check complete. Sent: 90-day={}, 60-day={}, 30-day={}",
                visas90.size(), visas60.size(), visas30.size());
    }

    private VisaResponse toResponse(Visa visa) {
        return VisaResponse.builder()
                .id(visa.getId())
                .visaType(visa.getVisaType())
                .startDate(visa.getStartDate())
                .expiryDate(visa.getExpiryDate())
                .daysUntilExpiry(visa.daysUntilExpiry())
                .caseNumber(visa.getCaseNumber())
                .sponsorEmployer(visa.getSponsorEmployer())
                .status(visa.getStatus())
                .notes(visa.getNotes())
                .createdAt(visa.getCreatedAt())
                .build();
    }
}
