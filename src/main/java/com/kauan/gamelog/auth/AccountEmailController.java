package com.kauan.gamelog.auth;

import com.kauan.gamelog.auth.dto.ForgotPasswordRequest;
import com.kauan.gamelog.auth.dto.ResetPasswordRequest;
import com.kauan.gamelog.auth.dto.VerifyEmailRequest;
import com.kauan.gamelog.shared.security.CurrentUserId;
import com.kauan.gamelog.shared.security.OpenApiConfig;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Os links por e-mail: confirmar o e-mail e redefinir a senha. Todas as rotas respondem 204. */
@RestController
public class AccountEmailController {
    private final AccountEmails emails;

    public AccountEmailController(AccountEmails emails) {
        this.emails = emails;
    }

    @PostMapping("/auth/email/verify")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verify(@Valid @RequestBody VerifyEmailRequest request) {
        emails.verify(request.token());
    }

    /** Manda o link de novo: 409 se o e-mail já foi confirmado, 429 se o último saiu há menos de um minuto. */
    @PostMapping("/me/email/verification")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = OpenApiConfig.BEARER)
    public void resend(@CurrentUserId Long userId) {
        emails.sendVerification(userId);
    }

    /** Sempre 204: a resposta não revela se o e-mail tem conta. */
    @PostMapping("/auth/password/forgot")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void forgot(@Valid @RequestBody ForgotPasswordRequest request) {
        emails.forgotPassword(request.email());
    }

    /** Encerra todas as sessões: é preciso entrar de novo com a senha nova. */
    @PostMapping("/auth/password/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset(@Valid @RequestBody ResetPasswordRequest request) {
        emails.resetPassword(request.token(), request.newPassword());
    }
}
