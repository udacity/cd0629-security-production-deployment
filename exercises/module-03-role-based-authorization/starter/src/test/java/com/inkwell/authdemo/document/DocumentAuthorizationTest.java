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

/**
 * Seed data (see data.sql):
 *   alice / SupportDesk!2025  (ADMIN)
 *   bob   / MyContracts!2025  (USER) — owns document 1
 *   carol / AgencyOwner!2025  (USER) — owns document 2
 *
 * Tests 1-3 are given as worked examples. Tests 4-5 are the ones that
 * actually verify Jules's bug is fixed — TODO: fill them in yourself.
 *
 * @Transactional rolls back each test's DB changes afterward, so the
 * "adminCanDeleteAnyDocument" test doesn't leave document 2 missing for
 * the tests that run after it.
 */
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
        // TODO: this is Jules's exact bug report. bob (not carol, not admin)
        // requesting GET /api/documents/2 (carol's document) should get 403.
    }

    @Test
    void nonAdminCannotDeleteEvenTheirOwnDocument() throws Exception {
        // TODO: bob deleting DELETE /api/documents/1 (his OWN document)
        // should still get 403 — ownership isn't enough for delete.
    }
}
