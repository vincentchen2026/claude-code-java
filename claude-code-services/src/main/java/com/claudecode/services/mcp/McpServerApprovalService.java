package com.claudecode.services.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class McpServerApprovalService {

    private static final Logger log = LoggerFactory.getLogger(McpServerApprovalService.class);

    private final Map<String, ServerApproval> approvals = new ConcurrentHashMap<>();
    private final Map<String, ServerInfo> servers = new ConcurrentHashMap<>();

    public String requestApproval(String serverId, String serverName, String serverVersion, String requestedPermissions) {
        String approvalId = "approval_" + System.currentTimeMillis();

        ServerApproval approval = new ServerApproval(
            approvalId,
            serverId,
            serverName,
            serverVersion,
            requestedPermissions,
            Instant.now(),
            ApprovalStatus.PENDING,
            null,
            null
        );

        approvals.put(approvalId, approval);
        servers.put(serverId, new ServerInfo(serverId, serverName, serverVersion, ApprovalStatus.PENDING.name()));

        log.info("Approval requested for server {} ({})", serverName, serverId);
        return approvalId;
    }

    public boolean approve(String approvalId, String approvedBy) {
        ServerApproval approval = approvals.get(approvalId);
        if (approval == null || approval.status() != ApprovalStatus.PENDING) {
            return false;
        }

        ServerApproval updated = new ServerApproval(
            approval.approvalId(),
            approval.serverId(),
            approval.serverName(),
            approval.serverVersion(),
            approval.requestedPermissions(),
            approval.requestedAt(),
            ApprovalStatus.APPROVED,
            approvedBy,
            Instant.now()
        );

        approvals.put(approvalId, updated);
        servers.put(approval.serverId(), new ServerInfo(
            approval.serverId(),
            approval.serverName(),
            approval.serverVersion(),
            ApprovalStatus.APPROVED.name()
        ));

        log.info("Server {} approved by {}", approval.serverName(), approvedBy);
        return true;
    }

    public boolean reject(String approvalId, String rejectedBy, String reason) {
        ServerApproval approval = approvals.get(approvalId);
        if (approval == null || approval.status() != ApprovalStatus.PENDING) {
            return false;
        }

        ServerApproval updated = new ServerApproval(
            approval.approvalId(),
            approval.serverId(),
            approval.serverName(),
            approval.serverVersion(),
            approval.requestedPermissions(),
            approval.requestedAt(),
            ApprovalStatus.REJECTED,
            rejectedBy,
            Instant.now()
        );

        approvals.put(approvalId, updated);
        servers.put(approval.serverId(), new ServerInfo(
            approval.serverId(),
            approval.serverName(),
            approval.serverVersion(),
            ApprovalStatus.REJECTED.name()
        ));

        log.info("Server {} rejected by {}: {}", approval.serverName(), rejectedBy, reason);
        return true;
    }

    public ServerApproval getApproval(String approvalId) {
        return approvals.get(approvalId);
    }

    public ServerInfo getServerInfo(String serverId) {
        return servers.get(serverId);
    }

    public boolean isApproved(String serverId) {
        ServerInfo info = servers.get(serverId);
        return info != null && ApprovalStatus.APPROVED.name().equals(info.status());
    }

    public record ServerApproval(
        String approvalId,
        String serverId,
        String serverName,
        String serverVersion,
        String requestedPermissions,
        Instant requestedAt,
        ApprovalStatus status,
        String reviewedBy,
        Instant reviewedAt
    ) {}

    public record ServerInfo(
        String serverId,
        String serverName,
        String serverVersion,
        String status
    ) {}

    public enum ApprovalStatus {
        PENDING,
        APPROVED,
        REJECTED,
        EXPIRED
    }
}