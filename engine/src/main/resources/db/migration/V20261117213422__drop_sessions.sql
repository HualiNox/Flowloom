-- 会话已迁移至 Redis，移除遗留的 PostgreSQL sessions 表。
DROP TABLE IF EXISTS sessions;
