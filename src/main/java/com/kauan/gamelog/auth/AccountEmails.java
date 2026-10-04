package com.kauan.gamelog.auth;

import com.kauan.gamelog.auth.AccountTokens.Purpose;
import com.kauan.gamelog.shared.ConflictException;
import com.kauan.gamelog.shared.TooManyRequestsException;
import com.kauan.gamelog.shared.UnprocessableException;
import com.kauan.gamelog.shared.mail.Mail;
import com.kauan.gamelog.shared.mail.MailSettings;
import com.kauan.gamelog.shared.mail.Mailer;
import com.kauan.gamelog.user.AccountContact;
import com.kauan.gamelog.user.UserService;
import java.time.Duration;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Confirmar o e-mail e redefinir a senha por um link enviado por e-mail (RF09, RN21). */
@Service
public class AccountEmails {
    private static final Duration VERIFY_TTL = Duration.ofHours(24);
    private static final Duration RESET_TTL = Duration.ofHours(1);
    /** Um e-mail por minuto de cada tipo, para ninguém usar a conta de outra pessoa como spam. */
    private static final Duration RESEND_WINDOW = Duration.ofMinutes(1);

    private final AccountTokens tokens;
    private final UserService users;
    private final Mailer mailer;
    private final MailSettings settings;

    AccountEmails(AccountTokens tokens, UserService users, Mailer mailer, MailSettings settings) {
        this.tokens = tokens;
        this.users = users;
        this.mailer = mailer;
        this.settings = settings;
    }

    /** Depois do cadastro e quando a pessoa pede de novo. */
    @Transactional
    public void sendVerification(long userId) {
        AccountContact contact = users.contact(userId);
        if (contact.verified()) {
            throw new ConflictException("Este e-mail já foi confirmado.");
        }
        if (tokens.sentWithin(userId, Purpose.VERIFY_EMAIL, RESEND_WINDOW)) {
            throw new TooManyRequestsException("Espere um minuto para pedir outro e-mail.", RESEND_WINDOW);
        }
        String token = tokens.create(userId, Purpose.VERIFY_EMAIL, VERIFY_TTL);
        mailer.send(new Mail(contact.email(), "Confirme seu e-mail no Game Log", """
                Olá, %s!

                Para confirmar este e-mail, abra o link abaixo. Ele vale por 24 horas.
                %s/verify-email?token=%s

                Se você não criou uma conta no Game Log, é só ignorar esta mensagem.
                """.formatted(
                        contact.name(), settings.appUrl(), token)));
    }

    @Transactional
    public void verify(String token) {
        users.markEmailVerified(tokens.consume(token, Purpose.VERIFY_EMAIL).orElseThrow(AccountEmails::invalid));
    }

    /** Responde igual exista a conta ou não, para não revelar quem tem cadastro. */
    @Transactional
    public void forgotPassword(String email) {
        users.contactByEmail(email)
                .filter(contact -> !tokens.sentWithin(contact.id(), Purpose.RESET_PASSWORD, RESEND_WINDOW))
                .ifPresent(contact -> {
                    String token = tokens.create(contact.id(), Purpose.RESET_PASSWORD, RESET_TTL);
                    mailer.send(new Mail(contact.email(), "Redefinir a senha do Game Log", """
                            Olá, %s!

                            Recebemos um pedido para redefinir a sua senha. Para escolher uma nova, abra o link abaixo.
                            Ele vale por 1 hora.
                            %s/reset-password?token=%s

                            Se não foi você, ignore esta mensagem: a senha atual continua valendo.
                            """.formatted(
                                    contact.name(), settings.appUrl(), token)));
                });
    }

    /** Troca a senha e encerra as sessões abertas; o link usado também confirma o e-mail. */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        users.resetPassword(
                tokens.consume(token, Purpose.RESET_PASSWORD).orElseThrow(AccountEmails::invalid), newPassword);
    }

    private static UnprocessableException invalid() {
        return UnprocessableException.field("INVALID_TOKEN", "token", "link inválido ou expirado");
    }
}
