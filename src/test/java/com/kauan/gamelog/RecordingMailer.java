package com.kauan.gamelog;

import com.kauan.gamelog.shared.mail.Mail;
import com.kauan.gamelog.shared.mail.Mailer;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Primary;

/** Guarda os e-mails em memória, no lugar do SMTP, para os testes lerem os links. */
@Primary
@TestConfiguration(proxyBeanMethods = false)
public class RecordingMailer implements Mailer {
    private static final Pattern TOKEN = Pattern.compile("token=([A-Za-z0-9_-]+)");

    private final List<Mail> sent = new CopyOnWriteArrayList<>();

    @Override
    public void send(Mail mail) {
        sent.add(mail);
    }

    public List<Mail> sentTo(String to) {
        return sent.stream().filter(mail -> mail.to().equals(to)).toList();
    }

    /** O token do link do último e-mail para esse endereço. */
    public Optional<String> lastToken(String to) {
        List<Mail> mails = sentTo(to);
        if (mails.isEmpty()) {
            return Optional.empty();
        }
        Matcher matcher = TOKEN.matcher(mails.getLast().text());
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }
}
