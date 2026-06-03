package com.navigator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * H1B Career Navigator API
 *
 * Built from personal experience navigating H1B/H4 transitions,
 * job searching after a career break, and making financial decisions
 * as an immigrant professional in the US.
 *
 * Modules:
 *   1. Visa Timeline Tracker  - Track H1B/H4/EAD key dates with automated alerts
 *   2. Job Application Tracker - Pipeline management for job applications
 *   3. AI Career Advisor       - AWS Bedrock powered career recommendations
 *   4. Financial Calculator    - 401k, NRE/NRO, US-India transfer calculations
 */
@SpringBootApplication
@EnableCaching
@EnableScheduling
public class H1bCareerNavigatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(H1bCareerNavigatorApplication.class, args);
    }
}
