package com.mewrix.flowloom.engine.auth.mapper;

import java.time.LocalDateTime;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.mewrix.flowloom.engine.auth.entity.Session;

/** sessions 表访问接口。 */
@Mapper
public interface SessionMapper {

        /**
         * 按凭证哈希查找仍有效（未过期且未吊销）的会话。
         *
         * @param tokenHash 凭证 SHA-256 哈希
         * @param now       当前业务时间，用于与 {@code expires_at} 比较
         * @return 有效会话；不存在、已过期或已吊销时返回 {@code null}
         */
        Session findValidSessionByHash(
                        @Param("tokenHash") byte[] tokenHash,
                        @Param("now") LocalDateTime now);

        /**
         * 插入新会话。主键由数据库生成。
         *
         * @param session 待插入会话（需含 userId、tokenHash、createdAt、expiresAt）
         * @return 受影响行数
         */
        int insert(Session session);

        /**
         * 在满足滑动续期间隔时延长过期时间。
         *
         * <p>
         * {@code refreshThreshold} 一般为 {@code now + timeout - minRefreshInterval}；
         * 仅当 {@code now < expires_at <= refreshThreshold} 时更新为 {@code newExpiresAt}。
         *
         * @param tokenHash        凭证哈希
         * @param now              当前业务时间
         * @param refreshThreshold 允许续期的过期时间上限
         * @param newExpiresAt     续期后的过期时间（一般为 {@code now + timeout}）
         * @return 受影响行数；未到续期间隔时为 0
         */
        int extendExpiryIfDue(
                        @Param("tokenHash") byte[] tokenHash,
                        @Param("now") LocalDateTime now,
                        @Param("refreshThreshold") LocalDateTime refreshThreshold,
                        @Param("newExpiresAt") LocalDateTime newExpiresAt);

        /**
         * 删除已过期的会话（{@code expires_at <= now}）。
         *
         * @param now 当前业务时间
         * @return 受影响行数
         */
        int deleteExpiredSessions(@Param("now") LocalDateTime now);

        /**
         * 删除已吊销的会话（{@code revoked_at IS NOT NULL}）。
         *
         * @return 受影响行数
         */
        int deleteRevokedSessions();

        /**
         * 吊销会话：将 {@code revoked_at} 设为 {@code now}；已吊销则不更新。
         *
         * @param tokenHash 凭证 SHA-256 哈希
         * @param now       吊销时间
         * @return 受影响行数；未知或已吊销时为 0
         */
        int revokeSession(@Param("tokenHash") byte[] tokenHash, @Param("now") LocalDateTime now);
}
