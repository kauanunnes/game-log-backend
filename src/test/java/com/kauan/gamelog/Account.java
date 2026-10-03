package com.kauan.gamelog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Conta criada pela API, como o front faria, com o access token e o cookie de refresh. */
public record Account(String username, String accessToken, Cookie refreshCookie) {
    public static final String PASSWORD = "senha-de-teste";

    public static Account register(MockMvc mockMvc, String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "%s", "email": "%s@example.com", "password": "%s"}
                                """.formatted(username, username, PASSWORD)))
                .andExpect(status().isCreated())
                .andReturn();
        return from(username, result);
    }

    public static Account login(MockMvc mockMvc, String login, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login": "%s", "password": "%s"}
                                """.formatted(login, password)))
                .andExpect(status().isOk())
                .andReturn();
        return from(login, result);
    }

    /** Não existe rota que crie admin: o papel é dado direto no banco. */
    public static Account admin(MockMvc mockMvc, JdbcClient jdbc, String username) throws Exception {
        register(mockMvc, username);
        jdbc.sql("UPDATE users SET role = 'ADMIN' WHERE username = :username")
                .param("username", username)
                .update();
        return login(mockMvc, username, PASSWORD);
    }

    public static Account from(String username, MvcResult result) throws Exception {
        String token = JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
        return new Account(username, token, result.getResponse().getCookie("refresh_token"));
    }

    public String bearer() {
        return "Bearer " + accessToken;
    }
}
