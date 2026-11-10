package com.mewrix.flowloom.engine.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.web.bind.annotation.ResponseStatus;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

/**
 * 项目注解使用规范测试
 *
 * <p>
 * 使用 ArchUnit 检查项目中的注解使用情况，
 * 确保代码符合既定的架构与编码规范。
 * </p>
 */
@AnalyzeClasses(packages = "com.mewrix.flowloom.engine")
public class AnnotationRulesTest {

    /**
     * 禁止在类上使用 {@link ResponseStatus}。
     *
     * <p>
     * HTTP 响应状态应由 Controller 或统一响应处理机制显式控制，
     * 避免注解隐式改变响应行为。
     * </p>
     */
    @ArchTest
    static final ArchRule no_response_status_on_classes =
            noClasses().should()
                    .beAnnotatedWith(ResponseStatus.class);

    /**
     * 禁止在方法上使用 {@link ResponseStatus}。
     *
     * <p>
     * HTTP 响应状态应通过 {@code ResponseEntity} 或项目约定的响应机制控制，
     * 避免 {@code @ResponseStatus} 的隐式处理行为与项目响应规范冲突。
     * </p>
     */
    @ArchTest
    static final ArchRule no_response_status_on_methods =
            noMethods().should()
                    .beAnnotatedWith(ResponseStatus.class);
}