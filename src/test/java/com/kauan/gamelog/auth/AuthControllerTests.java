package com.kauan.gamelog.auth;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kauan.gamelog.Account;
import com.kauan.gamelog.IntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@IntegrationTest
class AuthControllerTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void registersAndStartsASession() throws Exception {
        register("auth_ana", "auth_ana@example.com", Account.PASSWORD)
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/v1/me"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(cookie().httpOnly("refresh_token", true))
                .andExpect(cookie().secure("refresh_token", true))
                .andExpect(cookie().sameSite("refresh_token", "Lax"))
                .andExpect(cookie().path("refresh_token", "/api/v1/auth"));
    }

    @Test
    void rejectsTakenUsernamesAndEmailsIgnoringCase() throws Exception {
        Account.register(mockMvc, "auth_bia");

        register("AUTH_BIA", "outra@example.com", Account.PASSWORD)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Esse username já está em uso."));
        register("auth_bia2", "Auth_Bia@Example.com", Account.PASSWORD)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Esse e-mail já tem uma conta."));
    }

    @ParameterizedTest
    @CsvSource({
        "ab, ab@example.com, senha-de-teste, username, 'use de 3 a 20 letras, números ou _'",
        "admin, adm@example.com, senha-de-teste, username, esse username é reservado",
        "com espaco, esp@example.com, senha-de-teste, username, 'use de 3 a 20 letras, números ou _'",
        "auth_caio, nao-e-email, senha-de-teste, email, e-mail inválido",
        "auth_caio, caio@example.com, curta, password, use de 8 a 64 caracteres"
    })
    void validatesTheRegistration(String username, String email, String password, String field, String message)
            throws Exception {
        register(username, email, password)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.errors[0].field").value(field))
                .andExpect(jsonPath("$.errors[0].message").value(message));
    }

    @Test
    void logsInWithUsernameOrEmail() throws Exception {
        Account.register(mockMvc, "auth_davi");

        Account.login(mockMvc, "Auth_Davi", Account.PASSWORD);
        Account.login(mockMvc, "AUTH_DAVI@example.com", Account.PASSWORD);
    }

    @Test
    void doesNotTellWhetherTheUserExists() throws Exception {
        Account.register(mockMvc, "auth_eva");

        for (String login : new String[] {"auth_eva", "ninguem_aqui"}) {
            login(login, "senha-errada")
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.detail").value("Username, e-mail ou senha incorretos."));
        }
    }

    @Test
    void blocksTheLoginAfterFiveWrongPasswords() throws Exception {
        Account.register(mockMvc, "auth_fabi");
        for (int i = 0; i < LoginThrottle.MAX_FAILURES; i++) {
            login("auth_fabi", "senha-errada").andExpect(status().isUnauthorized());
        }

        login("auth_fabi", Account.PASSWORD)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER));
    }

    @Test
    void rotatesTheRefreshTokenAndEndsTheSessionWhenAnOldOneComesBack() throws Exception {
        Account account = Account.register(mockMvc, "auth_gil");

        Account rotated = Account.from(
                "auth_gil",
                refresh(account.refreshCookie())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.accessToken").isNotEmpty())
                        .andReturn());

        // O cookie antigo reaparece: quem o copiou e o dono legítimo perdem a sessão.
        refresh(account.refreshCookie()).andExpect(status().isUnauthorized());
        refresh(rotated.refreshCookie())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Sua sessão expirou. Entre de novo."));
    }

    @Test
    void logoutEndsTheSession() throws Exception {
        Account account = Account.register(mockMvc, "auth_hugo");

        mockMvc.perform(post("/api/v1/auth/logout").cookie(account.refreshCookie()))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("refresh_token", 0));
        refresh(account.refreshCookie()).andExpect(status().isUnauthorized());
    }

    @Test
    void refreshWithoutCookieIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Não autenticado"));
    }

    @Test
    void reportsEveryInvalidField() throws Exception {
        register("", "", "")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors[*].field", hasItems("username", "email", "password")));
    }

    private ResultActions register(String username, String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username": "%s", "email": "%s", "password": "%s"}
                        """.formatted(username, email, password)));
    }

    private ResultActions login(String login, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"login": "%s", "password": "%s"}
                        """.formatted(login, password)));
    }

    private ResultActions refresh(Cookie cookie) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/refresh").cookie(cookie));
    }
}
