-- 增加session吊销时间
ALTER TABLE sessions
ADD COLUMN revoked_at TIMESTAMP DEFAULT NULL;

-- ============================================================
-- 字段注释
-- ============================================================
COMMENT ON COLUMN sessions.revoked_at IS '会话吊销时间，null表示未吊销';

-- 增加 token_hash 索引
CREATE INDEX ix_sessions_token_hash ON sessions(token_hash);

-- ============================================================
-- 索引注释
-- ============================================================
COMMENT ON INDEX ix_sessions_token_hash IS '会话凭证哈希查询索引';
