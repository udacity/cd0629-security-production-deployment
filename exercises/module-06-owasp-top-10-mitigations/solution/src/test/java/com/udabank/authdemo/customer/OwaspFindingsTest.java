package com.udabank.authdemo.customer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
        mockMvc.perform(get("/api/customers/search")
                        .param("name", "x' OR '1'='1' --")
                        .with(httpBasic("marcus", "QueueZero!2025")))
                .andExpect(status().isOk())
                .andExpect(content().string("[]"));
    }

    @Test
    void xssPayloadInNoteIsEscapedInRenderedHtml() throws Exception {
        mockMvc.perform(get("/support/customers/2").with(httpBasic("sana", "TicketDesk!2025")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("<script>"))))
                .andExpect(content().string(containsString("&lt;script&gt;")));
    }
}
