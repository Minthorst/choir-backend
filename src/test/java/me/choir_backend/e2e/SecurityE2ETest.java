package me.choir_backend.e2e;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityE2ETest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void adminEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/admin/members"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void memberRoleCannotAccessAdminEndpoints() throws Exception {
        mockMvc.perform(get("/admin/members").with(user("member").roles("MEMBER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void memberRoleCannotAccessDoormanEndpoints() throws Exception {
        mockMvc.perform(get("/doorman/members").with(user("member").roles("MEMBER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminRoleCanAccessAdminEndpoints() throws Exception {
        mockMvc.perform(get("/admin/members").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void doormanRoleCanAccessDoormanEndpoints() throws Exception {
        mockMvc.perform(get("/doorman/members").with(user("doorman").roles("DOORMAN")))
                .andExpect(status().isOk());
    }

    @Test
    void meReportsUnauthenticatedWithoutLogin() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false));
    }

    @Test
    void loginWithConfiguredAdminPasswordSucceeds() throws Exception {
        mockMvc.perform(post("/api/login")
                        .with(csrf())
                        .param("username", "admin")
                        .param("password", "admin"))
                .andExpect(status().isOk());
    }

    @Test
    void loginWithWrongPasswordIsRejected() throws Exception {
        mockMvc.perform(post("/api/login")
                        .with(csrf())
                        .param("username", "admin")
                        .param("password", "wrong"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postWithoutCsrfTokenIsRejected() throws Exception {
        mockMvc.perform(post("/admin/tickets")
                        .with(user("admin").roles("ADMIN"))
                        .contentType("application/json")
                        .content("{\"memberId\":1,\"regularTickets\":1,\"commitTickets\":0}"))
                .andExpect(status().isForbidden());
    }
}
