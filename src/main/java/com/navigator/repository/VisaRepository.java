package com.navigator.repository;

import com.navigator.entity.Visa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface VisaRepository extends JpaRepository<Visa, UUID> {

    List<Visa> findByUserId(UUID userId);

    List<Visa> findByUserIdAndStatus(UUID userId, Visa.VisaStatus status);

    /**
     * Finds visas expiring within a given number of days.
     * Used by the scheduled reminder service to send SNS alerts.
     *
     * Design note: Running this as a DB query (not in-memory filter)
     * because at scale this table could have millions of rows.
     * See ADR-002 for scheduler design decisions.
     */
    @Query("""
        SELECT v FROM Visa v
        WHERE v.expiryDate BETWEEN :today AND :targetDate
        AND v.status = 'ACTIVE'
        """)
    List<Visa> findVisasExpiringBetween(
        @Param("today") LocalDate today,
        @Param("targetDate") LocalDate targetDate
    );

    @Query("""
        SELECT v FROM Visa v
        WHERE v.expiryDate BETWEEN :today AND :targetDate
        AND v.status = 'ACTIVE'
        AND v.alert90DaySent = false
        """)
    List<Visa> findVisasNeedingAlert90Day(
        @Param("today") LocalDate today,
        @Param("targetDate") LocalDate targetDate
    );

    @Query("""
        SELECT v FROM Visa v
        WHERE v.expiryDate BETWEEN :today AND :targetDate
        AND v.status = 'ACTIVE'
        AND v.alert60DaySent = false
        """)
    List<Visa> findVisasNeedingAlert60Day(
        @Param("today") LocalDate today,
        @Param("targetDate") LocalDate targetDate
    );

    @Query("""
        SELECT v FROM Visa v
        WHERE v.expiryDate BETWEEN :today AND :targetDate
        AND v.status = 'ACTIVE'
        AND v.alert30DaySent = false
        """)
    List<Visa> findVisasNeedingAlert30Day(
        @Param("today") LocalDate today,
        @Param("targetDate") LocalDate targetDate
    );
}
