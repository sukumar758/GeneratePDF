package org.acentrik.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
    @Autowired
    private JavaMailSender mailSender;

    /**
     * Sends an email with PDF attachment
     *
     * @param to Recipient email address
     * @param subject Email subject
     * @param body Email body content
     * @param pdfAttachment PDF file as byte array
     * @param attachmentName Name for the attached file
     * @throws MessagingException If email sending fails
     */
    public void sendPdfEmail(String to, String subject, String body,
                             byte[] pdfAttachment, String attachmentName) throws MessagingException {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(body);

        // Attach the PDF
        helper.addAttachment(attachmentName, new ByteArrayResource(pdfAttachment), "application/pdf");

        mailSender.send(message);
    }
}