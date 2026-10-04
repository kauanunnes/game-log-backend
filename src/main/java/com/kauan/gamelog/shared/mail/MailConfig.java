package com.kauan.gamelog.shared.mail;

import io.micrometer.context.ContextExecutorService;
import io.micrometer.context.ContextSnapshotFactory;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/** SMTP quando {@code spring.mail.host} está configurado; sem ele, o e-mail inteiro vai para o log. */
@Configuration
class MailConfig {
    private static final Logger log = LoggerFactory.getLogger(MailConfig.class);

    @Bean
    Mailer mailer(ObjectProvider<JavaMailSender> smtp, MailSettings settings) {
        JavaMailSender sender = smtp.getIfAvailable();
        if (sender == null) {
            log.warn("Sem SMTP (spring.mail.host): os e-mails vão para o log, com os links.");
            return mail -> log.info("E-mail para {}: {}\n{}", mail.to(), mail.subject(), mail.text());
        }
        // Numa thread à parte: quem pediu não espera o SMTP, e o tempo de resposta não revela se a conta existe.
        // O contexto da requisição vai junto, para uma falha sair no log com o mesmo traceId.
        var executor = ContextExecutorService.wrap(
                Executors.newVirtualThreadPerTaskExecutor(),
                ContextSnapshotFactory.builder().build());
        return mail -> executor.execute(() -> {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(settings.from());
            message.setTo(mail.to());
            message.setSubject(mail.subject());
            message.setText(mail.text());
            try {
                sender.send(message);
            } catch (MailException e) {
                log.warn("Não foi possível enviar o e-mail \"{}\".", mail.subject(), e);
            }
        });
    }
}
