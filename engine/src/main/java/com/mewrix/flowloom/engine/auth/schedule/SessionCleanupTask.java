package com.mewrix.flowloom.engine.auth.schedule;

import java.util.concurrent.TimeUnit;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.mewrix.flowloom.engine.auth.service.SessionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 定时清理无效会话（已过期或已吊销）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionCleanupTask {
    private final SessionService sessionService;

    /**
     * 物理删除无效会话行（已过期或已吊销）。
     *
     * <p>
     * 认证路径已通过 {@code expires_at} / {@code revoked_at} 拒绝无效凭证；
     * 本任务仅负责清理库表残留，减少膨胀。上次执行结束后间隔 30 分钟再跑。
     * </p>
     */
    @Scheduled(fixedDelay = 30, timeUnit = TimeUnit.MINUTES)
    public void cleanupInvalidSessions() {
        int deletedCount = sessionService.deleteInvalidSessions();

        if (deletedCount > 0) {
            log.info("Cleaned up {} invalid sessions", deletedCount);
        }
    }
}
