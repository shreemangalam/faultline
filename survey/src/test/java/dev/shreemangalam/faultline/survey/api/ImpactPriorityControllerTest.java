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
class ImpactPriorityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsRankedAndExplainablePrioritiesForTheSeededOutage() throws Exception {
        mockMvc.perform(post("/api/v1/analysis/impact-priorities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"systemId":"SAP_ECC","degradationType":"COMPLETE_OUTAGE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.failedSystemId").value("SAP_ECC"))
                .andExpect(jsonPath("$.priorities[0].systemId").value("CPI_ORDER_SYNC"))
                .andExpect(jsonPath("$.priorities[0].severity").value("CRITICAL"))
                .andExpect(jsonPath("$.priorities[0].scoreBreakdown.criticalityPoints").value(90))
                .andExpect(jsonPath("$.priorities[0].scoreBreakdown.fanOutPoints").value(6));
    }
}
