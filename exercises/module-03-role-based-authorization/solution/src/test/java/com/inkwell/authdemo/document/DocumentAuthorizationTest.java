package com.inkwell.authdemo.document;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DocumentAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void ownerCanViewTheirOwnDocument() throws Exception {
        mockMvc.perform(get("/api/documents/1").with(httpBasic("bob", "MyContracts!2025")))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanViewAnyDocument() throws Exception {
        mockMvc.perform(get("/api/documents/2").with(httpBasic("alice", "SupportDesk!2025")))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanDeleteAnyDocument() throws Exception {
        mockMvc.perform(delete("/api/documents/2").with(httpBasic("alice", "SupportDesk!2025")))
                .andExpect(status().isNoContent());
    }

    @Test
    void nonOwnerNonAdminCannotViewSomeoneElsesDocument() throws Exception {
        mockMvc.perform(get("/api/documents/2").with(httpBasic("bob", "MyContracts!2025")))
                .andExpect(status().isForbidden());
    }

    @Test
    void nonAdminCannotDeleteEvenTheirOwnDocument() throws Exception {
        mockMvc.perform(delete("/api/documents/1").with(httpBasic("bob", "MyContracts!2025")))
                .andExpect(status().isForbidden());
    }
}
