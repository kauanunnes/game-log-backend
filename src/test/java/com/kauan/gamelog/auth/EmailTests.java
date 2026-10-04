package com.kauan.gamelog.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kauan.gamelog.Account;
import com.kauan.gamelog.IntegrationTest;
import com.kauan.gamelog.RecordingMailer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@IntegrationTest
class EmailTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private RecordingMailer mailer;

    @Test
    void theSignupLinkConfirmsTheEmailOnce() throws Exception {
        Account ana = Account.register(mockMvc, "em_ana");
        as(ana, get("/api/v1/me")).andExpect(jsonPath("$.emailVerified").value(false));
        String token = mailer.lastToken("em_ana@example.com").orElseThrow();
        assertThat(mailer.sentTo("em_ana@example.com").getLast().text())
                .contains("Olá, em_ana!")
                .contains("/verify-email?token=" + token);

        verify(token).andExpect(status().isNoContent());
        as(ana, get("/api/v1/me")).andExpect(jsonPath("$.emailVerified").value(true));
        verify(token)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
        as(ana, post("/api/v1/me/email/verification")).andExpect(status().isConflict());
    }

    @Test
    void resendingWaitsAMinuteAndReplacesTheOldLink() throws Exception {
        Account bia = Account.register(mockMvc, "em_bia");
        String first = mailer.lastToken("em_bia@example.com").orElseThrow();
        as(bia, post("/api/v1/me/email/verification")).andExpect(status().isTooManyRequests());

        age("em_bia");
        as(bia, post("/api/v1/me/email/verification")).andExpect(status().isNoContent());
        String second = mailer.lastToken("em_bia@example.com").orElseThrow();

        verify(first).andExpect(status().isUnprocessableContent());
        verify(second).andExpect(status().isNoContent());
    }

    @Test
    void forgotPasswordNeverTellsWhoHasAnAccount() throws Exception {
        forgot("ninguem_aqui@example.com").andExpect(status().isNoContent());

        assertThat(mailer.sentTo("ninguem_aqui@example.com")).isEmpty();
    }

    @Test
    void theResetLinkChangesThePasswordEndsTheSessionsAndConfirmsTheEmail() throws Exception {
        Account caio = Account.register(mockMvc, "em_caio");
        forgot("EM_CAIO@example.com").andExpect(status().isNoContent());
        String token = mailer.lastToken("em_caio@example.com").orElseThrow();

        reset(token, "senha-nova-123").andExpect(status().isNoContent());
        reset(token, "outra-senha-123").andExpect(status().isUnprocessableContent());

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(caio.refreshCookie()))
                .andExpect(status().isUnauthorized());
        Account again = Account.login(mockMvc, "em_caio", "senha-nova-123");
        as(again, get("/api/v1/me")).andExpect(jsonPath("$.emailVerified").value(true));
    }

    @Test
    void anExpiredLinkDoesNotWork() throws Exception {
        Account.register(mockMvc, "em_davi");
        forgot("em_davi@example.com").andExpect(status().isNoContent());
        String token = mailer.lastToken("em_davi@example.com").orElseThrow();
        jdbc.sql("""
                        UPDATE account_tokens SET expires_at = now() - interval '1 minute'
                        WHERE user_id = (SELECT id FROM users WHERE username = 'em_davi')
                        """).update();

        reset(token, "senha-nova-123")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    /** Volta no tempo os links já enviados, para não esperar o minuto entre um e-mail e outro. */
    private void age(String username) {
        jdbc.sql("""
                        UPDATE account_tokens SET created_at = created_at - interval '2 minutes'
                        WHERE user_id = (SELECT id FROM users WHERE username = :username)
                        """).param("username", username).update();
    }

    private ResultActions verify(String token) throws Exception {
        return send("/api/v1/auth/email/verify", "{\"token\": \"%s\"}".formatted(token));
    }

    private ResultActions forgot(String email) throws Exception {
        return send("/api/v1/auth/password/forgot", "{\"email\": \"%s\"}".formatted(email));
    }

    private ResultActions reset(String token, String password) throws Exception {
        return send(
                "/api/v1/auth/password/reset",
                "{\"token\": \"%s\", \"newPassword\": \"%s\"}".formatted(token, password));
    }

    private ResultActions send(String url, String body) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions as(Account account, MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, account.bearer()));
    }
}
