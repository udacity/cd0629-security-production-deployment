package com.udabank.authdemo.invoice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests 1-3 are given as worked examples. Tests 4-5 are the ones that
 * actually verify Yusuf's near-miss can't happen again — TODO: fill them in.
 *
 * TestJwtIssuer.issueToken(subject, entitlements...) mints a real signed
 * token; pass it as "Authorization: Bearer <token>".
 */
@SpringBootTest
@AutoConfigureMockMvc
class InvoiceAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void noTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/invoices"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void readScopeTokenCanListInvoices() throws Exception {
        String token = TestJwtIssuer.issueToken("partner-quickbooks-sync", "read");

        mockMvc.perform(get("/api/invoices").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void readWriteScopeTokenCanCreateInvoice() throws Exception {
        String token = TestJwtIssuer.issueToken("partner-billing-connector", "read", "write");

        mockMvc.perform(post("/api/invoices")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerName\":\"Marlowe Studio\",\"amountCents\":75000}"))
                .andExpect(status().isCreated());
    }

    @Test
    void readOnlyScopeTokenCannotCreateInvoice() throws Exception {
        // TODO: this is Yusuf's exact near-miss. A token minted with ONLY
        // "read" (TestJwtIssuer.issueToken(subject, "read")) should get 403
        // on POST /api/invoices.
    }

    @Test
    void writeOnlyScopeTokenCannotListInvoices() throws Exception {
        // TODO: a token minted with ONLY "write" (no "read") should get 403
        // on GET /api/invoices.
    }
}
