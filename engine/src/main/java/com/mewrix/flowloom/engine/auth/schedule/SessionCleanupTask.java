package com.mewrix.flowloom.engine.auth.schedule;

import java.util.concurrent.TimeUnit;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.mewrix.flowloom.engine.auth.service.SessionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 定时清理过期会话。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionCleanupTask {
    private final SessionService sessionService;

    /**
     * 清理过期会话。
     *
     * <p>
     * 该任务会在后台定时运行，删除所有已过期的会话记录。
     * 如果 deletedCount 大于 0，则会在日志中打印清理的会话数量。
     * </p>
     *
     * @Scheduled 每隔 30 分钟执行一次。
     */
    @Scheduled(fixedDelay = 30, timeUnit = TimeUnit.MINUTES)
    public void cleanupUnvailedSessions() {
        int deletedCount = sessionService.deleteExpiredSessionsAndRevokedSessions();

        if (deletedCount > 0) {
            log.info("Clened up {} expired sessions", deletedCount);
        }
    }
}
