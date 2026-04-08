package com.claudecode.services.credit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OverageCreditGrantService {

    private static final Logger log = LoggerFactory.getLogger(OverageCreditGrantService.class);

    private final Map<String, CreditGrant> grants = new ConcurrentHashMap<>();
    private final String apiKey;

    public OverageCreditGrantService(String apiKey) {
        this.apiKey = apiKey;
    }

    public CreditGrant getGrant(String userId) {
        return grants.get(userId);
    }

    public boolean hasGrant(String userId) {
        return grants.containsKey(userId);
    }

    public CreditGrant createGrant(String userId, double amount, String reason) {
        CreditGrant grant = new CreditGrant(
            "grant_" + System.currentTimeMillis(),
            userId,
            amount,
            Instant.now(),
            Instant.now().plusSeconds(30 * 24 * 60 * 60),
            GrantStatus.ACTIVE,
            reason,
            amount
        );
        grants.put(userId, grant);
        log.info("Created credit grant {} for user {}: ${}", grant.grantId(), userId, amount);
        return grant;
    }

    public boolean useGrant(String userId, double amount) {
        CreditGrant grant = grants.get(userId);
        if (grant == null || grant.status() != GrantStatus.ACTIVE) {
            return false;
        }

        if (Instant.now().isAfter(grant.expiresAt())) {
            updateGrantStatus(userId, GrantStatus.EXPIRED);
            return false;
        }

        double remaining = grant.remainingAmount();
        if (remaining < amount) {
            return false;
        }

        CreditGrant updated = new CreditGrant(
            grant.grantId(),
            userId,
            grant.originalAmount(),
            grant.createdAt(),
            grant.expiresAt(),
            grant.status(),
            grant.reason(),
            remaining - amount
        );
        grants.put(userId, updated);
        log.debug("User {} used ${} from grant, remaining: ${}", userId, amount, remaining - amount);
        return true;
    }

    public void updateGrantStatus(String userId, GrantStatus status) {
        CreditGrant grant = grants.get(userId);
        if (grant != null) {
            CreditGrant updated = new CreditGrant(
                grant.grantId(),
                userId,
                grant.originalAmount(),
                grant.createdAt(),
                grant.expiresAt(),
                status,
                grant.reason(),
                grant.remainingAmount()
            );
            grants.put(userId, updated);
        }
    }

    public double getRemainingAmount(String userId) {
        CreditGrant grant = grants.get(userId);
        return grant != null ? grant.remainingAmount() : 0.0;
    }

    public record CreditGrant(
        String grantId,
        String userId,
        double originalAmount,
        Instant createdAt,
        Instant expiresAt,
        GrantStatus status,
        String reason,
        double remainingAmount
    ) {}

    public enum GrantStatus {
        ACTIVE,
        EXHAUSTED,
        EXPIRED,
        REVOKED
    }
}