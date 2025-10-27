// src/main/java/com/example/iotserver/scheduler/DeviceScheduler.java
package com.example.iotserver.scheduler;

import com.example.iotserver.service.DeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class DeviceScheduler {

    private final DeviceService deviceService;

    // Chạy mỗi phút để kiểm tra thiết bị offline
    @Scheduled(fixedRate = 60000)
    public void checkDeviceStatus() {
        log.debug("Running scheduled task to check for stale devices...");
        deviceService.checkStaleDevices();
    }
}