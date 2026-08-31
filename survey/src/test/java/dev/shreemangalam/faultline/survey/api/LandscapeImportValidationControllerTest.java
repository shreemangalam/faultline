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
class LandscapeImportValidationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void acceptsAValidLandscapeImportPreview() throws Exception {
        mockMvc.perform(post("/api/v1/landscape/import/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "systems": [
                                    {"id":"SAP_ECC","label":"SAP ECC","criticality":10},
                                    {"id":"CPI_ORDER_SYNC","label":"CPI Order Sync","criticality":9}
                                  ],
                                  "integrations": [
                                    {"id":"ECC_TO_ORDER","sourceSystemId":"SAP_ECC","targetSystemId":"CPI_ORDER_SYNC","protocol":"RFC"}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.systemCount").value(2))
                .andExpect(jsonPath("$.integrationCount").value(1))
                .andExpect(jsonPath("$.issues").isEmpty());
    }

    @Test
    void returnsFieldErrorsForAnInvalidLandscapeImportPreview() throws Exception {
        mockMvc.perform(post("/api/v1/landscape/import/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "systems": [{"id":"SAP_ECC","label":"","criticality":11}],
                                  "integrations": [
                                    {"id":"bad edge","sourceSystemId":"SAP_ECC","targetSystemId":"MISSING","protocol":""}
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.issues.length()").value(5))
                .andExpect(jsonPath("$.issues[0].field").value("systems[0].label"));
    }
}
