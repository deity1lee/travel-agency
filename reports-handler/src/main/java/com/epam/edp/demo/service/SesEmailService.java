package com.epam.edp.demo.service;

import com.epam.edp.demo.exception.EmailProcessingException;
import com.epam.edp.demo.export.CsvColumnOrder;
import com.epam.edp.demo.model.AggregateReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.RawMessage;
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest;

import javax.activation.DataHandler;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.*;
import javax.mail.util.ByteArrayDataSource;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Properties;

@Slf4j
@Service
public class SesEmailService {

    private final SesClient sesClient;
    private final SpringTemplateEngine templateEngine;
    private final String admin;

    public SesEmailService(SesClient sesClient,
                           SpringTemplateEngine templateEngine, @Value("${app.admin.email}")String admin) {
        this.sesClient = sesClient;
        this.templateEngine = templateEngine;
        this.admin = admin;
    }

    public void sendReportEmail(List<? extends AggregateReport> reports, byte[] csv, String template, String recipient,
                                CsvColumnOrder columnOrder) {
        try {
            log.info("Sending report email to {}", admin);

            Context context = new Context();
            context.setVariable("reports", reports);
            context.setVariable("columns", columnOrder.columns());
            String html = templateEngine.process("email/"+template, context);

            Session session = Session.getDefaultInstance(new Properties());
            MimeMessage message = new MimeMessage(session);

            message.setSubject("Weekly Report");
            message.setFrom(new InternetAddress(admin));
            message.setRecipients(Message.RecipientType.TO, admin);

            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(html, "text/html; charset=UTF-8");

            ByteArrayDataSource dataSource = new ByteArrayDataSource(csv, "text/csv");

            MimeBodyPart attachmentPart = new MimeBodyPart();
            attachmentPart.setDataHandler(new DataHandler(dataSource));
            attachmentPart.setFileName(template+".csv");

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(htmlPart);
            multipart.addBodyPart(attachmentPart);

            message.setContent(multipart);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            message.writeTo(outputStream);

            RawMessage rawMessage = RawMessage.builder()
                    .data(SdkBytes.fromByteArray(outputStream.toByteArray()))
                    .build();

            sesClient.sendRawEmail(
                    SendRawEmailRequest.builder()
                            .rawMessage(rawMessage)
                            .build()
            );

            log.info("Email sent successfully");

        } catch (Exception e) {
            log.error("Failed to send email", e);
            throw new EmailProcessingException("Email sending failed", e);
        }
    }
}
