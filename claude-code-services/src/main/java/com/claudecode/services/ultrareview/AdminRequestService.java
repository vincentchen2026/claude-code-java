package com.claudecode.services.ultrareview;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AdminRequestService {

    private static final Logger log = LoggerFactory.getLogger(AdminRequestService.class);

    private final ConcurrentLinkedQueue<AdminRequest> requests = new ConcurrentLinkedQueue<>();
    private final Map<String, AdminRequest> requestById = new ConcurrentHashMap<>();
    private final Map<String, AdminUser> adminUsers = new ConcurrentHashMap<>();

    public AdminRequestService() {
        adminUsers.put("admin", new AdminUser("admin", "admin", true));
    }

    public String submitRequest(AdminRequestType type, Map<String, Object> payload, String submittedBy) {
        String requestId = "req_" + System.currentTimeMillis() + "_" + type.name().toLowerCase();
        AdminRequest request = new AdminRequest(
            requestId,
            type,
            payload,
            submittedBy,
            Instant.now(),
            RequestStatus.PENDING,
            null,
            null
        );
        requests.add(request);
        requestById.put(requestId, request);
        log.info("Admin request submitted: {} by {}", requestId, submittedBy);
        return requestId;
    }

    public AdminRequest getRequest(String requestId) {
        return requestById.get(requestId);
    }

    public List<AdminRequest> getPendingRequests() {
        return requests.stream()
            .filter(r -> r.status() == RequestStatus.PENDING)
            .toList();
    }

    public boolean approveRequest(String requestId, String approvedBy) {
        AdminRequest request = requestById.get(requestId);
        if (request == null || request.status() != RequestStatus.PENDING) {
            return false;
        }

        AdminRequest updated = new AdminRequest(
            request.requestId(),
            request.type(),
            request.payload(),
            request.submittedBy(),
            request.submittedAt(),
            RequestStatus.APPROVED,
            approvedBy,
            Instant.now()
        );
        requestById.put(requestId, updated);
        log.info("Admin request approved: {} by {}", requestId, approvedBy);
        return true;
    }

    public boolean rejectRequest(String requestId, String rejectedBy, String reason) {
        AdminRequest request = requestById.get(requestId);
        if (request == null || request.status() != RequestStatus.PENDING) {
            return false;
        }

        AdminRequest updated = new AdminRequest(
            request.requestId(),
            request.type(),
            request.payload(),
            request.submittedBy(),
            request.submittedAt(),
            RequestStatus.REJECTED,
            rejectedBy,
            Instant.now()
        );
        requestById.put(requestId, updated);
        log.info("Admin request rejected: {} by {} ({})", requestId, rejectedBy, reason);
        return true;
    }

    public boolean isAdmin(String userId) {
        AdminUser user = adminUsers.get(userId);
        return user != null && user.isAdmin();
    }

    public void addAdminUser(String userId, String role) {
        adminUsers.put(userId, new AdminUser(userId, role, "admin".equals(role)));
    }

    public record AdminRequest(
        String requestId,
        AdminRequestType type,
        Map<String, Object> payload,
        String submittedBy,
        Instant submittedAt,
        RequestStatus status,
        String reviewedBy,
        Instant reviewedAt
    ) {}

    public record AdminUser(
        String userId,
        String role,
        boolean isAdmin
    ) {}

    public enum AdminRequestType {
        USER_MANAGEMENT,
        QUOTA_ADJUSTMENT,
        SYSTEM_CONFIG,
        AUDIT_LOG_REQUEST,
        DATA_EXPORT,
        FEATURE_FLAG_CHANGE
    }

    public enum RequestStatus {
        PENDING,
        APPROVED,
        REJECTED,
        EXPIRED
    }
}