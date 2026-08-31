package dev.shreemangalam.faultline.survey.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RecoverySequenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsARecoveryOrderForTheSeededOutage() throws Exception {
        mockMvc.perform(post("/api/v1/analysis/recovery-sequence")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"systemId":"SAP_ECC","degradationType":"COMPLETE_OUTAGE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.failedSystemId").value("SAP_ECC"))
                .andExpect(jsonPath("$.recoverySequence[0]").value("SAP_ECC"))
                .andExpect(jsonPath("$.recoverySequence[1]").value("CPI_INVOICE_SYNC"))
                .andExpect(jsonPath("$.cycleWarning").value(true))
                .andExpect(jsonPath("$.unresolvedSystemIds[0]").value("CARRIER_API"));
    }
}
