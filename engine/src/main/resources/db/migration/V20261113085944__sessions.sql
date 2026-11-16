-- ============================================================
-- 创建会话表
-- ============================================================
CREATE TABLE sessions (
    id              UUID            NOT NULL DEFAULT GEN_RANDOM_UUID(),

    user_id         UUID            NOT NULL,
    token_hash      BYTEA           NOT NULL,

    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMP       NOT NULL,

    CONSTRAINT pk_sessions_id PRIMARY KEY (id),

    CONSTRAINT fk_sessions_user_id FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_sessions_token_hash UNIQUE (token_hash),

    CONSTRAINT ck_sessions_token_hash_length CHECK (
        OCTET_LENGTH(token_hash) = 32
    ),

    CONSTRAINT ck_sessions_expires_at CHECK (
        expires_at > created_at
    )
);

-- ============================================================
-- 索引
-- ============================================================
CREATE INDEX ix_sessions_user_id
    ON sessions (user_id);

CREATE INDEX ix_sessions_expires_at
    ON sessions (expires_at);


-- ============================================================
-- 表注释
-- ============================================================
COMMENT ON TABLE sessions IS '用户会话信息表';

-- ============================================================
-- 字段注释
-- ============================================================
COMMENT ON COLUMN sessions.id IS '会话唯一标识，使用 UUID';
COMMENT ON COLUMN sessions.user_id IS '所属用户 ID';
COMMENT ON COLUMN sessions.token_hash IS '会话凭证 SHA-256 哈希值，不存储原始凭证';
COMMENT ON COLUMN sessions.created_at IS '会话创建时间';
COMMENT ON COLUMN sessions.expires_at IS '会话过期时间';

-- ============================================================
-- 约束注释
-- ============================================================
COMMENT ON CONSTRAINT pk_sessions_id ON sessions IS '会话 ID 主键约束';
COMMENT ON CONSTRAINT fk_sessions_user_id ON sessions IS '关联用户表，用户删除时级联删除会话';
COMMENT ON CONSTRAINT uq_sessions_token_hash ON sessions IS '会话凭证哈希唯一约束';
COMMENT ON CONSTRAINT ck_sessions_token_hash_length ON sessions IS 'SHA-256 哈希长度约束，固定为 32 字节';
COMMENT ON CONSTRAINT ck_sessions_expires_at ON sessions IS '会话过期时间必须晚于创建时间';

-- ============================================================
-- 索引注释
-- ============================================================
COMMENT ON INDEX ix_sessions_user_id IS '用户会话查询索引';
COMMENT ON INDEX ix_sessions_expires_at IS '会话过期清理索引';
