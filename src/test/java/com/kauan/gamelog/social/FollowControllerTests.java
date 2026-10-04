package com.kauan.gamelog.social;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
class FollowControllerTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void followingAgainKeepsTheDateAndTheListsComeNewestFirst() throws Exception {
        Account ana = Account.register(mockMvc, "fw_ana");
        Account bia = Account.register(mockMvc, "fw_bia");
        Account caio = Account.register(mockMvc, "fw_caio");

        as(ana, put("/api/v1/users/fw_bia/follow")).andExpect(status().isNoContent());
        as(caio, put("/api/v1/users/fw_bia/follow")).andExpect(status().isNoContent());
        as(ana, put("/api/v1/users/FW_BIA/follow")).andExpect(status().isNoContent());
        as(bia, put("/api/v1/users/fw_ana/follow")).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/users/fw_bia/followers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].username", contains("fw_caio", "fw_ana")))
                .andExpect(jsonPath("$.content[0].followedAt").isNotEmpty())
                .andExpect(jsonPath("$.page.totalElements").value(2));
        mockMvc.perform(get("/api/v1/users/fw_bia/following"))
                .andExpect(jsonPath("$.content[*].username", contains("fw_ana")));
        mockMvc.perform(get("/api/v1/users/fw_bia"))
                .andExpect(jsonPath("$.counts.followers").value(2))
                .andExpect(jsonPath("$.counts.following").value(1))
                .andExpect(jsonPath("$.counts.played").value(0));
    }

    @Test
    void checksAndUnfollows() throws Exception {
        Account davi = Account.register(mockMvc, "fw_davi");
        Account.register(mockMvc, "fw_eva");

        as(davi, get("/api/v1/users/fw_eva/follow")).andExpect(status().isNotFound());
        as(davi, put("/api/v1/users/fw_eva/follow")).andExpect(status().isNoContent());
        as(davi, get("/api/v1/users/fw_eva/follow")).andExpect(status().isNoContent());

        as(davi, delete("/api/v1/users/fw_eva/follow")).andExpect(status().isNoContent());
        as(davi, delete("/api/v1/users/fw_eva/follow")).andExpect(status().isNoContent());
        as(davi, get("/api/v1/users/fw_eva/follow"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Você não segue \"fw_eva\"."));
        mockMvc.perform(get("/api/v1/users/fw_eva"))
                .andExpect(jsonPath("$.counts.followers").value(0));
    }

    @Test
    void refusesFollowingItselfOrSomeoneWhoDoesNotExist() throws Exception {
        Account fabi = Account.register(mockMvc, "fw_fabi");

        as(fabi, put("/api/v1/users/fw_fabi/follow"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("CANNOT_FOLLOW_SELF"));
        as(fabi, put("/api/v1/users/fw_ninguem/follow")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/users/fw_ninguem/followers")).andExpect(status().isNotFound());
    }

    @Test
    void followingNeedsLoginButTheListsArePublic() throws Exception {
        Account.register(mockMvc, "fw_gil");

        mockMvc.perform(put("/api/v1/users/fw_gil/follow")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/users/fw_gil/follow")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/users/fw_gil/followers")).andExpect(status().isOk());
    }

    @Test
    void privateProfilesHideTheirListsExceptFromTheOwner() throws Exception {
        Account hana = Account.register(mockMvc, "fw_hana");
        Account ivo = Account.register(mockMvc, "fw_ivo");
        as(
                        hana,
                        patch("/api/v1/me/settings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                        {"profileVisibility": "PRIVATE"}
                        """))
                .andExpect(status().isOk());

        as(ivo, put("/api/v1/users/fw_hana/follow")).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/users/fw_hana/followers")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/users/fw_hana"))
                .andExpect(jsonPath("$.counts").doesNotExist());
        as(hana, get("/api/v1/me/followers")).andExpect(jsonPath("$.content[*].username", contains("fw_ivo")));
        as(ivo, get("/api/v1/me/following")).andExpect(jsonPath("$.content[*].username", contains("fw_hana")));
    }

    @Test
    void deletingTheAccountRemovesItsFollows() throws Exception {
        Account jade = Account.register(mockMvc, "fw_jade");
        Account.register(mockMvc, "fw_kai");
        as(jade, put("/api/v1/users/fw_kai/follow")).andExpect(status().isNoContent());

        as(jade, delete("/api/v1/me").contentType(MediaType.APPLICATION_JSON).content("""
                        {"password": "%s"}
                        """.formatted(Account.PASSWORD)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/users/fw_kai/followers"))
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    private ResultActions as(Account account, MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, account.bearer()));
    }
}
