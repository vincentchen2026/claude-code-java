package com.claudecode.services.credit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ReferralService {

    private static final Logger log = LoggerFactory.getLogger(ReferralService.class);

    private final Map<String, Referral> referrals = new ConcurrentHashMap<>();
    private final Map<String, String> referrerByUser = new ConcurrentHashMap<>();
    private final double defaultCreditAmount;

    public ReferralService() {
        this(5.0);
    }

    public ReferralService(double defaultCreditAmount) {
        this.defaultCreditAmount = defaultCreditAmount;
    }

    public String createReferralLink(String referrerId) {
        String code = generateReferralCode(referrerId);
        Referral referral = new Referral(
            code,
            referrerId,
            null,
            Instant.now(),
            null,
            ReferralStatus.PENDING,
            defaultCreditAmount,
            0
        );
        referrals.put(code, referral);
        log.info("Created referral code {} for referrer {}", code, referrerId);
        return code;
    }

    public boolean applyReferral(String referralCode, String newUserId) {
        Referral referral = referrals.get(referralCode);
        if (referral == null) {
            log.warn("Referral code {} not found", referralCode);
            return false;
        }

        if (referral.status() != ReferralStatus.PENDING) {
            log.warn("Referral code {} already used", referralCode);
            return false;
        }

        Referral updated = new Referral(
            referral.code(),
            referral.referrerId(),
            newUserId,
            referral.createdAt(),
            Instant.now(),
            ReferralStatus.APPLIED,
            referral.creditAmount(),
            0
        );
        referrals.put(referralCode, updated);
        referrerByUser.put(newUserId, referral.referrerId());

        log.info("Applied referral {} for new user {} (referrer: {})", 
            referralCode, newUserId, referral.referrerId());
        return true;
    }

    public void creditReferrer(String referralCode) {
        Referral referral = referrals.get(referralCode);
        if (referral == null || referral.status() != ReferralStatus.APPLIED) {
            return;
        }

        Referral updated = new Referral(
            referral.code(),
            referral.referrerId(),
            referral.newUserId(),
            referral.createdAt(),
            referral.appliedAt(),
            ReferralStatus.CREDITED,
            referral.creditAmount(),
            1
        );
        referrals.put(referralCode, updated);
        log.info("Credited referrer {} for referral {}", referral.referrerId(), referralCode);
    }

    public Referral getReferral(String code) {
        return referrals.get(code);
    }

    public int getReferralCount(String referrerId) {
        return (int) referrals.values().stream()
            .filter(r -> r.referrerId().equals(referrerId))
            .filter(r -> r.status() == ReferralStatus.CREDITED)
            .count();
    }

    public double getTotalCreditsEarned(String referrerId) {
        return referrals.values().stream()
            .filter(r -> r.referrerId().equals(referrerId))
            .filter(r -> r.status() == ReferralStatus.CREDITED)
            .mapToDouble(r -> r.creditAmount() * r.creditCount())
            .sum();
    }

    private String generateReferralCode(String referrerId) {
        return "ref_" + referrerId.hashCode() + "_" + System.currentTimeMillis();
    }

    public record Referral(
        String code,
        String referrerId,
        String newUserId,
        Instant createdAt,
        Instant appliedAt,
        ReferralStatus status,
        double creditAmount,
        int creditCount
    ) {}

    public enum ReferralStatus {
        PENDING,
        APPLIED,
        CREDITED,
        EXPIRED
    }
}