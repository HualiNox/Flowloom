package com.mewrix.flowloom.engine.common.util;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 项目统一时间工具。
 *
 * <p>
 * 业务层写入与比较一律使用 {@link #ZONE_ID} 下的墙钟时间，避免依赖 JVM 默认时区或数据库
 * {@code NOW()}。
 */
public final class TimeUtil {

    /**
     * 项目统一时区：Asia/Shanghai。
     */
    public static final ZoneId ZONE_ID = ZoneId.of("Asia/Shanghai");

    /**
     * 私有构造函数，防止实例化。
     */
    private TimeUtil() {
    }

    /**
     * 获取当前业务时间（Asia/Shanghai）。
     *
     * @return 当前业务时间（Asia/Shanghai）
     */
    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE_ID);
    }
}
