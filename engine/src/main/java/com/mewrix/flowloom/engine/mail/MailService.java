package com.mewrix.flowloom.engine.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMailMessage;
import org.springframework.stereotype.Service;

import com.mewrix.flowloom.engine.common.response.BusinessCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {
    private final MailProperties mailProperties;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    private BusinessCode sendSimpleEmail(String to, String subject, String content) {
        if (!mailProperties.enable()) {
            return MailBusinessCode.MAIL_DISABLED;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);

        try {
            mailSender.send(message);
        } catch (MailException ex) {
            log.warn("Failed to send mail to {}: {}", to, ex.getMessage());
            return MailBusinessCode.MAIL_SEND_FAILED;
        }

        return BusinessCode.ok();
    }

    private BusinessCode sendMimeEmail(String to, String subject, String htmlContent) {
        if (!mailProperties.enable()) {
            return MailBusinessCode.MAIL_DISABLED;
        }

        MimeMailMessage message = new MimeMailMessage(mailSender.createMimeMessage());
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(htmlContent);

        try {
            mailSender.send(message.getMimeMessage());
        } catch (MailException ex) {
            log.warn("Failed to send mail to {}: {}", to, ex.getMessage());
            return MailBusinessCode.MAIL_SEND_FAILED;
        }

        return BusinessCode.ok();
    }

    public BusinessCode sendEmail(String to, String subject, String content, boolean isHtml) {
        if (isHtml) {
            return sendMimeEmail(to, subject, content);
        } else {
            return sendSimpleEmail(to, subject, content);
        }
    }

}
