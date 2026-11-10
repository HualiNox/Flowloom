# Flowloom Engine

Flowloom Engine 是 Flowloom 项目的服务端模块，基于 Spring Boot 和 Spring MVC。目前模块处于基础建设阶段。

## 开发环境

- JDK 21：项目编译目标为 Java 21。
- 使用项目自带的 Maven Wrapper；首次使用可能需要下载 Maven 和项目依赖。

在 `engine` 目录下运行：

```sh
# 启动应用
./mvnw spring-boot:run

# 编译并运行测试
./mvnw test
```

Windows 下使用 `mvnw.cmd` 执行相同的 Maven 目标。

## 文档

- [开发约定](docs/conventions.md)：模块内的注解使用和请求日志约定。
