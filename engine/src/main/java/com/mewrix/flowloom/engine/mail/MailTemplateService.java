package com.mewrix.flowloom.engine.mail;

import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

/**
 * 邮件 HTML 模版渲染。文案由模版 {@code #{...}} 与 Context Locale 解析。
 */
@Service
public class MailTemplateService {
    private final SpringTemplateEngine mailTemplateEngine;

    public MailTemplateService(
            @Qualifier("mailTemplateEngine") SpringTemplateEngine mailTemplateEngine) {
        this.mailTemplateEngine = mailTemplateEngine;
    }

    public String renderHtml(String templateName, Locale locale, Map<String, Object> variables) {
        Context context = new Context(locale == null ? Locale.ENGLISH : locale);
        if (variables != null) {
            context.setVariables(variables);
        }
        return mailTemplateEngine.process(templateName, context);
    }
}
