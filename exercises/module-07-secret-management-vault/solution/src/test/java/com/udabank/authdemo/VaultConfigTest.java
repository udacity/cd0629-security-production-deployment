package com.udabank.authdemo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class VaultConfigTest {

    @RegisterExtension
    static final FakeVaultServer vaultServer = new FakeVaultServer();

    @Autowired
    private MockMvc mockMvc;

    @Test
    void dbCredentialsComeFromVaultNotFromApplicationYml() throws Exception {
        mockMvc.perform(get("/internal/db-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("vault_managed_user"))
                .andExpect(jsonPath("$.passwordConfigured").value(true));
    }
}
