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
class ImpactAnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void analyzesTheSeededSapEccOutageThroughTheHttpContract() throws Exception {
        mockMvc.perform(post("/api/v1/analysis/impact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"systemId":"SAP_ECC","degradationType":"COMPLETE_OUTAGE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incident.systemId").value("SAP_ECC"))
                .andExpect(jsonPath("$.affectedSystems[0].systemId").value("CPI_ORDER_SYNC"))
                .andExpect(jsonPath("$.affectedSystems[0].impactRing").value(1))
                .andExpect(jsonPath("$.affectedSystems[4].systemId").value("SALESFORCE"))
                .andExpect(jsonPath("$.affectedSystems[4].impactRing").value(2))
                .andExpect(jsonPath("$.affectedIntegrations[0].integrationId").value("ECC_TO_CPI_ORDER"))
                .andExpect(jsonPath("$.affectedIntegrations[0].impactRing").value(1));
    }

    @Test
    void rejectsAnIncidentForAnUnknownSystem() throws Exception {
        mockMvc.perform(post("/api/v1/analysis/impact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"systemId":"MISSING_SYSTEM","degradationType":"COMPLETE_OUTAGE"}
                                """))
                .andExpect(status().isNotFound());
    }
}
