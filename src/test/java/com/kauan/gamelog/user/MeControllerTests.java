package com.kauan.gamelog.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kauan.gamelog.Account;
import com.kauan.gamelog.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@IntegrationTest
class MeControllerTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void requiresAValidAccessToken() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Não autenticado"));
        mockMvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer nao.e.um.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsTheAccountWithDefaultSettings() throws Exception {
        Account account = Account.register(mockMvc, "me_ana");

        send(get("/api/v1/me"), account, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("me_ana"))
                .andExpect(jsonPath("$.email").value("me_ana@example.com"))
                .andExpect(jsonPath("$.gender").doesNotExist())
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.profileVisibility").value("PUBLIC"))
                .andExpect(jsonPath("$.showSpending").value(false))
                .andExpect(jsonPath("$.defaultCurrency").value("BRL"));
    }

    @Test
    void patchChangesOnlyWhatWasSentAndNullClears() throws Exception {
        Account account = Account.register(mockMvc, "me_bia");

        send(patch("/api/v1/me"), account, """
                        {"displayName": "Bia", "bio": "Jogo de tudo um pouco.", "gender": "FEMALE"}
                        """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gender").value("FEMALE"));

        send(patch("/api/v1/me"), account, """
                        {"gender": null, "bio": "  "}
                        """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Bia"))
                .andExpect(jsonPath("$.bio").doesNotExist())
                .andExpect(jsonPath("$.gender").doesNotExist());
    }

    @Test
    void changesTheUsernameUnlessItIsTaken() throws Exception {
        Account.register(mockMvc, "me_caio");
        Account account = Account.register(mockMvc, "me_davi");

        send(patch("/api/v1/me"), account, """
                        {"username": "ME_CAIO"}
                        """).andExpect(status().isConflict());
        send(patch("/api/v1/me"), account, """
                        {"username": "Me_Davi_Novo"}
                        """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("me_davi_novo"));
        send(patch("/api/v1/me"), account, """
                        {"username": null}
                        """).andExpect(status().isUnprocessableContent());
    }

    @Test
    void updatesThePrivacySettingsAndTheCurrency() throws Exception {
        Account account = Account.register(mockMvc, "me_eva");

        send(patch("/api/v1/me/settings"), account, """
                        {"profileVisibility": "PRIVATE", "showSpending": true, "defaultCurrency": "USD"}
                        """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileVisibility").value("PRIVATE"))
                .andExpect(jsonPath("$.showSpending").value(true))
                .andExpect(jsonPath("$.defaultCurrency").value("USD"));
        send(patch("/api/v1/me/settings"), account, """
                        {"defaultCurrency": "XYZ"}
                        """)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("UNKNOWN_CURRENCY"));
    }

    @Test
    void changingThePasswordEndsEverySession() throws Exception {
        Account account = Account.register(mockMvc, "me_fabi");

        send(put("/api/v1/me/password"), account, """
                        {"currentPassword": "errada", "newPassword": "nova-senha-123"}
                        """)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors[0].field").value("currentPassword"));
        send(put("/api/v1/me/password"), account, """
                        {"currentPassword": "%s", "newPassword": "nova-senha-123"}
                        """.formatted(Account.PASSWORD))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(account.refreshCookie()))
                .andExpect(status().isUnauthorized());
        Account.login(mockMvc, "me_fabi", "nova-senha-123");
    }

    @Test
    void deletesTheAccountWithThePassword() throws Exception {
        Account account = Account.register(mockMvc, "me_gil");

        send(delete("/api/v1/me"), account, """
                        {"password": "errada"}
                        """).andExpect(status().isUnprocessableContent());
        send(delete("/api/v1/me"), account, """
                        {"password": "%s"}
                        """.formatted(Account.PASSWORD)).andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login": "me_gil", "password": "%s"}
                                """.formatted(Account.PASSWORD)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(account.refreshCookie()))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions send(MockHttpServletRequestBuilder request, Account account, String body) throws Exception {
        request.header(HttpHeaders.AUTHORIZATION, account.bearer());
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mockMvc.perform(request);
    }
}
