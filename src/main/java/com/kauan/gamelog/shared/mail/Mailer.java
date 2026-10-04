package com.kauan.gamelog.shared.mail;

/** Envia os e-mails da conta. Uma falha no envio nunca derruba quem pediu: o erro vai para o log. */
public interface Mailer {
    void send(Mail mail);
}
