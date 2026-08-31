package dev.shreemangalam.faultline.survey.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LandscapeHealthMapControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exposesTheSeededLandscapeInADashboardFriendlyShape() throws Exception {
        mockMvc.perform(get("/api/v1/landscape/health-map"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.systems.length()").value(8))
                .andExpect(jsonPath("$.systems[0].systemId").value("SAP_ECC"))
                .andExpect(jsonPath("$.systems[0].health").value("HEALTHY"))
                .andExpect(jsonPath("$.integrations.length()").value(7))
                .andExpect(jsonPath("$.integrations[0].integrationId").value("ECC_TO_CPI_ORDER"));
    }
}
