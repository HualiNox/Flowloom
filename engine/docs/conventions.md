# Engine 开发约定

## HTTP 状态码

HTTP 状态码由控制器或统一处理机制显式指定，用于表达 HTTP 请求的处理结果，不用于承载业务状态。

不要在 Engine 的类或方法上直接使用 `@ResponseStatus`，以免注解隐式改变响应状态。

`AnnotationRulesTest` 使用 ArchUnit 检查 `com.mewrix.flowloom.engine` 包下的类和方法是否直接标注了 `@ResponseStatus`。这是一项测试阶段的代码约束，不会禁用 Spring 的运行时功能；当前规则也不检查通过组合注解间接使用的情况。

## 请求日志

`RequestIdFilter` 为进入过滤器的 HTTP 请求生成请求 ID，并将其放入当前线程的 MDC，键名为 `requestId`。过滤器链执行结束后会清除该键，避免线程复用时残留上下文。

当前全局异常处理器会在记录未预期异常时读取并记录请求 ID。MDC 是线程上下文；在异步任务或其他线程中不能假定该值会自动传递。

## 会话存储（Redis）

用户会话存放在 Redis，不再使用 PostgreSQL `sessions` 表。

- 会话 key：`flowloom:session:{sha256(token)}`，值为 `userId`，TTL 由 `flowloom.session.timeout` 控制
- 用户索引：`flowloom:user:{userId}:sessions`（SET），用于按用户吊销全部会话
- 滑动续期：`redis/session-touch.lua`（`TTL` + 条件 `EXPIRE`）

Redis ACL 至少需要：`+get +set +del +expire +ttl +sadd +srem +smembers +eval +evalsha`，key 模式覆盖 `~flowloom:*`。

## 邮箱未验证访问范围

未验证邮箱的用户可以登录并获得会话 token。`EmailVerificationFilter` 在会话认证之后限制其业务范围：

- 允许：`/api/v1/user/email/verify/send`、`/api/v1/user/email/verify`、`/api/v1/auth/logout`（以及登录/注册，避免误拦）
- 其余已认证业务接口返回业务码 `EMAIL_NOT_VERIFIED`（HTTP 403），由前端引导至验证流程

## 邮箱验证

- `POST /api/v1/user/email/verify/send`：向当前用户发 6 位验证码；冷却键按 `userId`（非 IP）
- `POST /api/v1/user/email/verify`：校验验证码并写入 `email_verified_at`
- Redis：`flowloom:email-verify:code:{userId}`（值 `code:attempts`）、`flowloom:email-verify:cooldown:{userId}`
- 配置：`flowloom.mail.verification.code-ttl` / `resend-interval` / `max-attempts`
- 失败重试会读剩余 TTL，需 ACL 允许 `PTTL`（以及已有的 `TTL`）
