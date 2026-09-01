package dev.shreemangalam.faultline.survey.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ImpactAnalysisControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void persistsAndRetrievesTheSeededSapEccImpactReport() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/v1/analysis/impact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"systemId\":\"SAP_ECC\",\"degradationType\":\"COMPLETE_OUTAGE\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/v1/analysis/impact/")))
                .andExpect(jsonPath("$.incident.systemId").value("SAP_ECC"))
                .andExpect(jsonPath("$.affectedSystems[0].classification").value("ORPHANED"))
                .andExpect(jsonPath("$.affectedIntegrations[0].dataDomain").value("ORDER"))
                .andExpect(jsonPath("$.priorities[0].scoreBreakdown.criticalityPoints").value(100))
                .andExpect(jsonPath("$.recovery.cycleWarning").value(true))
                .andExpect(jsonPath("$.warnings[0]").value(org.hamcrest.Matchers.containsString("Dependency cycle")))
                .andReturn();
        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String reportId = created.get("reportId").asText();
        assertThat(reportId).isNotBlank();

        mockMvc.perform(get("/api/v1/analysis/impact/{reportId}", reportId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportId").value(reportId))
                .andExpect(jsonPath("$.landscapeReference").value("BUNDLED_SAMPLE"))
                .andExpect(jsonPath("$.incident.systemId").value("SAP_ECC"));
    }

    @Test
    void rejectsAnIncidentForAnUnknownSystem() throws Exception {
        mockMvc.perform(post("/api/v1/analysis/impact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"systemId\":\"MISSING_SYSTEM\",\"degradationType\":\"COMPLETE_OUTAGE\"}"))
                .andExpect(status().isNotFound());
    }
}