package com.example.iotserver.scheduler;

import com.example.iotserver.service.RuleEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class RuleScheduler {

    private final RuleEngineService ruleEngineService;

    /**
     * Chạy Rule Engine mỗi 30 giây
     * 
     * fixedDelay = 30000 nghĩa là sau khi hoàn thành, đợi 30 giây rồi chạy lại
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 10000)
    public void executeRules() {
        log.debug("🔄 Bắt đầu kiểm tra quy tắc tự động...");

        try {
            ruleEngineService.executeAllRules();
        } catch (Exception e) {
            log.error("Lỗi khi chạy Rule Engine: {}", e.getMessage(), e);
        }
    }

    /**
     * Dọn dẹp log cũ mỗi ngày lúc 2:00 sáng
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOldLogs() {
        log.info("🧹 Bắt đầu dọn dẹp log cũ...");

        // TODO: Triển khai logic xóa log cũ hơn 30 ngày

        log.info("✅ Hoàn thành dọn dẹp log");
    }
}