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
class LandscapeImportPersistenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void persistsAndRetrievesAValidatedLandscapeImport() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/v1/landscape/imports")
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
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/v1/landscape/imports/")))
                .andExpect(jsonPath("$.systems.length()").value(2))
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String importId = created.get("importId").asText();
        assertThat(importId).isNotBlank();

        mockMvc.perform(get("/api/v1/landscape/imports/{importId}", importId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importId").value(importId))
                .andExpect(jsonPath("$.systems[0].systemId").value("CPI_ORDER_SYNC"))
                .andExpect(jsonPath("$.systems[1].systemId").value("SAP_ECC"))
                .andExpect(jsonPath("$.integrations[0].integrationId").value("ECC_TO_ORDER"));
    }
}
