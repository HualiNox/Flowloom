package com.mewrix.flowloom.engine.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.mewrix.flowloom.engine.common.response.BusinessCode;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 业务邮件发送：有 HTML 时发 multipart（HTML + 纯文本），否则发纯文本。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {
    private final MailProperties mailProperties;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    /**
     * @param plainText 纯文本；可与 htmlText 同时提供
     * @param htmlText  HTML；非空时作为主内容
     */
    public BusinessCode sendEmail(String to, String subject, String plainText, String htmlText) {
        if (!mailProperties.enable()) {
            return MailBusinessCode.MAIL_DISABLED;
        }

        boolean hasHtml = StringUtils.hasText(htmlText);
        boolean hasPlain = StringUtils.hasText(plainText);
        if (!hasHtml && !hasPlain) {
            throw new IllegalArgumentException("Mail content must not be empty");
        }

        try {
            if (hasHtml) {
                sendMime(to, subject, hasPlain ? plainText : stripToPlain(htmlText), htmlText);
            } else {
                sendSimple(to, subject, plainText);
            }
            return BusinessCode.ok();
        } catch (MailException | MessagingException ex) {
            log.warn("Failed to send mail to {}: {}", to, ex.getMessage());
            return MailBusinessCode.MAIL_SEND_FAILED;
        }
    }

    private void sendSimple(String to, String subject, String plainText) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(plainText);
        mailSender.send(message);
    }

    private void sendMime(String to, String subject, String plainText, String htmlText)
            throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(plainText, htmlText);
        mailSender.send(mimeMessage);
    }

    private static String stripToPlain(String html) {
        return html.replaceAll("(?s)<[^>]*>", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
