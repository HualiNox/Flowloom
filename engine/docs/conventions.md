# Engine 开发约定

## HTTP 状态码

HTTP 状态码由控制器或统一处理机制显式指定，用于表达 HTTP 请求的处理结果，不用于承载业务状态。

不要在 Engine 的类或方法上直接使用 `@ResponseStatus`，以免注解隐式改变响应状态。

`AnnotationRulesTest` 使用 ArchUnit 检查 `com.mewrix.flowloom.engine` 包下的类和方法是否直接标注了 `@ResponseStatus`。这是一项测试阶段的代码约束，不会禁用 Spring 的运行时功能；当前规则也不检查通过组合注解间接使用的情况。

## 请求日志

`RequestIdFilter` 为进入过滤器的 HTTP 请求生成请求 ID，并将其放入当前线程的 MDC，键名为 `requestId`。过滤器链执行结束后会清除该键，避免线程复用时残留上下文。

当前全局异常处理器会在记录未预期异常时读取并记录请求 ID。MDC 是线程上下文；在异步任务或其他线程中不能假定该值会自动传递。
