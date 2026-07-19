package me.choir_backend.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ChoirFlowE2ETest {

    @Autowired
    MockMvc mockMvc;

    final ObjectMapper objectMapper = new ObjectMapper();

    private RequestPostProcessor admin() {
        return user("admin").roles("ADMIN", "DOORMAN", "MEMBER");
    }

    private RequestPostProcessor doorman() {
        return user("doorman").roles("DOORMAN", "MEMBER");
    }

    private RequestPostProcessor member() {
        return user("member").roles("MEMBER");
    }

    private String createMember(String name, int regular, int commit) throws Exception {
        String body = mockMvc.perform(post("/admin/member")
                        .with(admin()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                "{\"name\":\"%s\",\"regularTickets\":%d,\"commitTickets\":%d}",
                                name, regular, commit)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String secretKey = objectMapper.readTree(body).get("secretKey").asText();
        assertNotNull(secretKey);
        return secretKey;
    }

    private JsonNode adminMemberByName(String name) throws Exception {
        String body = mockMvc.perform(get("/admin/members").with(admin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        for (JsonNode node : objectMapper.readTree(body)) {
            if (name.equals(node.get("name").asText())) return node;
        }
        return null;
    }

    @Test
    void fullSessionLifecycle() throws Exception {
        // Admin creates three members
        String annaKey = createMember("Anna", 1, 1);
        String benKey = createMember("Ben", 0, 2);
        String caraKey = createMember("Cara", 1, 0);

        // Duplicate names are rejected with 409
        mockMvc.perform(post("/admin/member")
                        .with(admin()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"anna\",\"regularTickets\":0,\"commitTickets\":0}"))
                .andExpect(status().isConflict());

        // Anna sees her info and is not checked in yet
        mockMvc.perform(get("/member/" + annaKey).with(member()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Anna"))
                .andExpect(jsonPath("$.regularTickets").value(1))
                .andExpect(jsonPath("$.commitTickets").value(1))
                .andExpect(jsonPath("$.checkedIn").value(false));

        // Anna checks herself in; this opens a new session and spends her commit ticket first
        mockMvc.perform(post("/member/checkin/" + annaKey).with(member()).with(csrf()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/member/" + annaKey).with(member()))
                .andExpect(jsonPath("$.checkedIn").value(true))
                .andExpect(jsonPath("$.regularTickets").value(1))
                .andExpect(jsonPath("$.commitTickets").value(0));

        // Checking in twice is rejected
        mockMvc.perform(post("/member/checkin/" + annaKey).with(member()).with(csrf()))
                .andExpect(status().isBadRequest());

        // Unknown secret key yields 404
        mockMvc.perform(get("/member/does-not-exist").with(member()))
                .andExpect(status().isNotFound());

        // The doorman checks Cara in by id; her regular ticket is spent
        long caraId = adminMemberByName("Cara").get("id").asLong();
        mockMvc.perform(post("/doorman/checkin/" + caraId).with(doorman()).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Cara"))
                .andExpect(jsonPath("$.checkedIn").value(false))
                .andExpect(jsonPath("$.regularTickets").value(0));

        // Doorman sees both checked-in members
        String checkedIn = mockMvc.perform(get("/doorman/checkedin").with(doorman()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertEquals(2, objectMapper.readTree(checkedIn).size());

        // There is exactly one open session with two attendees
        String sessions = mockMvc.perform(get("/admin/sessions").with(admin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode sessionList = objectMapper.readTree(sessions);
        assertEquals(1, sessionList.size());
        JsonNode session = sessionList.get(0);
        assertTrue(session.get("isOpen").asBoolean());
        long sessionId = session.get("id").asLong();

        // Admin finalizes the session as COMMIT: Ben was absent with commit tickets and is charged
        mockMvc.perform(post("/admin/finalizeSession")
                        .with(admin()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"sessionId\":%d,\"sessionType\":\"COMMIT\"}", sessionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.presentMembers").value(2))
                .andExpect(jsonPath("$.absentCommitMembers").value(1));

        // Finalizing the same session twice is rejected
        mockMvc.perform(post("/admin/finalizeSession")
                        .with(admin()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"sessionId\":%d,\"sessionType\":\"COMMIT\"}", sessionId)))
                .andExpect(status().isBadRequest());

        // Final ticket balances: Anna 1/0, Ben 0/1 (charged), Cara 0/0
        JsonNode ben = adminMemberByName("Ben");
        assertEquals(0, ben.get("regularTickets").asInt());
        assertEquals(1, ben.get("commitTickets").asInt());
        assertEquals(1, adminMemberByName("Anna").get("regularTickets").asInt());
        assertEquals(0, adminMemberByName("Anna").get("commitTickets").asInt());
        assertEquals(0, adminMemberByName("Cara").get("regularTickets").asInt());

        // Admin tops up tickets
        mockMvc.perform(post("/admin/tickets")
                        .with(admin()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                "{\"memberId\":%d,\"regularTickets\":5,\"commitTickets\":2}", ben.get("id").asLong())))
                .andExpect(status().isOk());
        assertEquals(5, adminMemberByName("Ben").get("regularTickets").asInt());
        assertEquals(3, adminMemberByName("Ben").get("commitTickets").asInt());

        // Admin archives and unarchives Cara
        mockMvc.perform(post("/admin/members/archive")
                        .with(admin()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"memberId\":%d,\"archived\":true}", caraId)))
                .andExpect(status().isOk());
        assertTrue(adminMemberByName("Cara").get("archived").asBoolean());
        mockMvc.perform(post("/admin/members/archive")
                        .with(admin()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"memberId\":%d,\"archived\":false}", caraId)))
                .andExpect(status().isOk());
        assertFalse(adminMemberByName("Cara").get("archived").asBoolean());

        // Cara can still check in with an empty balance (down to -3 total)
        mockMvc.perform(post("/member/checkin/" + caraKey).with(member()).with(csrf()))
                .andExpect(status().isOk());
        assertEquals(-1, adminMemberByName("Cara").get("regularTickets").asInt());

        // Anna's ticket log shows her check-in (commit spent first) and one Init row per ticket type
        mockMvc.perform(get("/member/" + annaKey + "/ticketlog").with(member()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].type").value("CHECK_IN"))
                .andExpect(jsonPath("$[0].commitDelta").value(-1))
                .andExpect(jsonPath("$[0].regularDelta").value(0))
                .andExpect(jsonPath("$[1].type").value("INITIAL_BALANCE"))
                .andExpect(jsonPath("$[1].commitDelta").value(1))
                .andExpect(jsonPath("$[1].regularDelta").value(0))
                .andExpect(jsonPath("$[2].type").value("INITIAL_BALANCE"))
                .andExpect(jsonPath("$[2].regularDelta").value(1))
                .andExpect(jsonPath("$[2].commitDelta").value(0));

        // Ben's full log includes the no-show charge from the COMMIT finalize
        mockMvc.perform(get("/member/" + benKey + "/ticketlog").with(member()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].type").value("ADMIN_ADJUSTMENT"))
                .andExpect(jsonPath("$[0].regularDelta").value(5))
                .andExpect(jsonPath("$[0].commitDelta").value(2))
                .andExpect(jsonPath("$[1].type").value("NO_SHOW_CHARGE"))
                .andExpect(jsonPath("$[1].commitDelta").value(-1))
                .andExpect(jsonPath("$[2].type").value("INITIAL_BALANCE"));

        // The admin view of Ben's log matches the member view, including session charges
        mockMvc.perform(get("/admin/members/" + ben.get("id").asLong() + "/ticketlog").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].type").value("ADMIN_ADJUSTMENT"))
                .andExpect(jsonPath("$[1].type").value("NO_SHOW_CHARGE"))
                .andExpect(jsonPath("$[2].type").value("INITIAL_BALANCE"));

        // The ticket log of an unknown member yields 404
        mockMvc.perform(get("/member/does-not-exist/ticketlog").with(member()))
                .andExpect(status().isNotFound());
    }
}
