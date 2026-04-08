package com.claudecode.bridge;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TrustedDeviceVerifier {

    private final Map<String, DeviceRecord> devices;
    private final String currentDeviceId;

    public TrustedDeviceVerifier(String currentDeviceId) {
        this.devices = new ConcurrentHashMap<>();
        this.currentDeviceId = currentDeviceId;
    }

    public void registerDevice(String deviceId, DeviceInfo info) {
        devices.put(deviceId, new DeviceRecord(
            deviceId,
            info,
            Instant.now(),
            TrustLevel.UNTRUSTED
        ));
    }

    public boolean isTrusted(String deviceId) {
        DeviceRecord record = devices.get(deviceId);
        if (record == null) {
            return false;
        }
        return record.trustLevel() == TrustLevel.TRUSTED;
    }

    public void trustDevice(String deviceId) {
        DeviceRecord existing = devices.get(deviceId);
        if (existing != null) {
            devices.put(deviceId, new DeviceRecord(
                existing.deviceId(),
                existing.info(),
                existing.registeredAt(),
                TrustLevel.TRUSTED
            ));
        }
    }

    public void distrustDevice(String deviceId) {
        DeviceRecord existing = devices.get(deviceId);
        if (existing != null) {
            devices.put(deviceId, new DeviceRecord(
                existing.deviceId(),
                existing.info(),
                existing.registeredAt(),
                TrustLevel.UNTRUSTED
            ));
        }
    }

    public void updateDeviceInfo(String deviceId, DeviceInfo info) {
        DeviceRecord existing = devices.get(deviceId);
        if (existing != null) {
            devices.put(deviceId, new DeviceRecord(
                deviceId,
                info,
                existing.registeredAt(),
                existing.trustLevel()
            ));
        }
    }

    public DeviceRecord getDevice(String deviceId) {
        return devices.get(deviceId);
    }

    public boolean isCurrentDevice(String deviceId) {
        return currentDeviceId != null && currentDeviceId.equals(deviceId);
    }

    public int getTrustedDeviceCount() {
        return (int) devices.values().stream()
            .filter(r -> r.trustLevel() == TrustLevel.TRUSTED)
            .count();
    }

    public enum TrustLevel {
        TRUSTED, UNTRUSTED, PENDING
    }

    public record DeviceRecord(
        String deviceId,
        DeviceInfo info,
        Instant registeredAt,
        TrustLevel trustLevel
    ) {}

    public record DeviceInfo(
        String name,
        String type,
        String os,
        String lastIp
    ) {}
}