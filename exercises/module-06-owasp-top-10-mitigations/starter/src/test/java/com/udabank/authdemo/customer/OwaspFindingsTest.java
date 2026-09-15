package com.udabank.authdemo.customer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Seed data (see data.sql):
 *   marcus / QueueZero!2025 — SUPPORT, assigned to customer 1
 *   sana   / TicketDesk!2025 — SUPPORT, assigned to customer 2 (her note
 *                              contains the seeded XSS payload)
 *
 * Tests 1-2 are given as worked examples (they verify the access-control
 * fix, which is already done). Tests 3-4 verify your two TODOs — fill
 * them in.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OwaspFindingsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void agentCanViewAssignedCustomer() throws Exception {
        mockMvc.perform(get("/api/customers/1").with(httpBasic("marcus", "QueueZero!2025")))
                .andExpect(status().isOk());
    }

    @Test
    void agentCannotViewUnassignedCustomer() throws Exception {
        mockMvc.perform(get("/api/customers/2").with(httpBasic("marcus", "QueueZero!2025")))
                .andExpect(status().isForbidden());
    }

    @Test
    void searchWithSqlInjectionPayloadReturnsNoResults() throws Exception {
        // TODO: GET /api/customers/search?name=... with a classic injection
        // payload like "x' OR '1'='1' --" should come back as an EMPTY
        // JSON array ("[]"), not every customer in the database. Use
        // .andExpect(content().string("[]")) — you'll need to add
        // MockMvcResultMatchers.content() to the static imports above.
    }

    @Test
    void xssPayloadInNoteIsEscapedInRenderedHtml() throws Exception {
        // TODO: GET /support/customers/2 (sana's customer, whose seeded
        // note contains a <script> tag) should NOT contain a literal
        // "<script>" in the response body — it should contain the
        // HTML-escaped form instead ("&lt;script&gt;"). Use
        // .andExpect(content().string(org.hamcrest.Matchers.not(
        //     org.hamcrest.Matchers.containsString("<script>"))))
    }
}
