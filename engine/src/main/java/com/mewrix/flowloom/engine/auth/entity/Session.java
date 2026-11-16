package com.mewrix.flowloom.engine.auth.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Getter;
import lombok.Setter;

/** 对应 sessions 表的用户会话实体。时间字段为 Asia/Shanghai 墙钟时间。 */
@Getter
@Setter
@TableName("sessions")
public class Session {

    /** 会话唯一标识。 */
    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    /** 所属用户 ID。 */
    private UUID userId;

    /** 会话凭证的 SHA-256 哈希，不存储明文。 */
    private byte[] tokenHash;

    /** 会话创建时间。 */
    private LocalDateTime createdAt;

    /** 会话过期时间；滑动续期时会更新。 */
    private LocalDateTime expiresAt;

    /** 吊销时间；null 表示尚未吊销。 */
    private LocalDateTime revokedAt;

}
