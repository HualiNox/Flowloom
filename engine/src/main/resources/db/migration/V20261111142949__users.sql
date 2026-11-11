-- 创建用户表
CREATE TABLE users (
    id                  UUID            NOT NULL DEFAULT GEN_RANDOM_UUID(),

    display_name        VARCHAR(64)     NOT NULL,

    username            VARCHAR(32)     NOT NULL,
    email               VARCHAR(254)    NOT NULL,
    password_hash       TEXT            NOT NULL,

    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    email_verified_at   TIMESTAMP       DEFAULT NULL,
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_id PRIMARY KEY (id),

    CONSTRAINT ck_username CHECK (
        LENGTH(username) BETWEEN 4 AND 32
            AND username = BTRIM(username)
            AND username ~ '^[a-zA-Z][a-zA-Z0-9_\-]{3,31}$'
    ),

    CONSTRAINT ck_email CHECK (
        LENGTH(email) <= 254
            AND email = BTRIM(email)
            AND LOWER(email) ~ '^[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,}$'
    )
);

-- 唯一索引
CREATE UNIQUE INDEX uq_users_username
    ON users (LOWER(username));
CREATE UNIQUE INDEX uq_users_email
    ON users (LOWER(email));


-- ============================================================
-- 表注释
-- ============================================================
COMMENT ON TABLE users IS '用户账户信息表';

-- ============================================================
-- 字段注释
-- ============================================================
COMMENT ON COLUMN users.id IS '用户唯一标识，使用 UUID';
COMMENT ON COLUMN users.display_name IS '用户显示名称';
COMMENT ON COLUMN users.username IS '用户登录名，大小写不敏感';
COMMENT ON COLUMN users.email IS '用户邮箱地址，大小写不敏感';
COMMENT ON COLUMN users.password_hash IS '用户密码哈希值，使用安全密码哈希算法生成';
COMMENT ON COLUMN users.created_at IS '账户创建时间';
COMMENT ON COLUMN users.email_verified_at IS '邮箱验证完成时间，NULL 表示尚未验证';
COMMENT ON COLUMN users.updated_at IS '账户信息最后更新时间';

-- ============================================================
-- 约束注释
-- ============================================================
COMMENT ON CONSTRAINT pk_id ON users IS '用户 ID 主键约束';
COMMENT ON CONSTRAINT ck_username ON users
    IS '用户名格式约束：长度 4-32，仅允许字母、数字、下划线和连字符，且必须以字母开头';
COMMENT ON CONSTRAINT ck_email ON users IS '邮箱格式约束：限制邮箱基本格式及首尾空格';

-- ============================================================
-- 索引注释
-- ============================================================
COMMENT ON INDEX uq_users_username IS '用户名唯一索引，忽略大小写';
COMMENT ON INDEX uq_users_email IS '邮箱唯一索引，忽略大小写';
