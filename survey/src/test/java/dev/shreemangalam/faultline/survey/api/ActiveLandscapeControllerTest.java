package dev.shreemangalam.faultline.survey.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.shreemangalam.faultline.survey.persistence.ActiveLandscapeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ActiveLandscapeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ActiveLandscapeRepository activeLandscapeRepository;

    @AfterEach
    void clearActiveLandscapeSelection() {
        activeLandscapeRepository.deleteAll();
    }

    @Test
    void activatesAnImportAndUsesItForTheHealthMapAndAnalysis() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/v1/landscape/imports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "systems": [
                                    {"id":"SOURCE","label":"Imported Source","criticality":10},
                                    {"id":"TARGET","label":"Imported Target","criticality":7}
                                  ],
                                  "integrations": [
                                    {"id":"SOURCE_TO_TARGET","sourceSystemId":"SOURCE","targetSystemId":"TARGET","protocol":"REST"}
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String importId = created.get("importId").asText();

        mockMvc.perform(put("/api/v1/landscape/imports/{importId}/activate", importId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importId").value(importId));

        mockMvc.perform(get("/api/v1/landscape/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.systems[0].systemId").value("SOURCE"));

        mockMvc.perform(get("/api/v1/landscape/health-map"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.systems.length()").value(2))
                .andExpect(jsonPath("$.systems[0].label").value("Imported Source"));

        mockMvc.perform(post("/api/v1/analysis/impact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"systemId":"SOURCE","degradationType":"COMPLETE_OUTAGE"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.affectedSystems[0].systemId").value("TARGET"));
    }

    @Test
    void rejectsActivationForAnUnknownImport() throws Exception {
        mockMvc.perform(put("/api/v1/landscape/imports/{importId}/activate", "11111111-1111-1111-1111-111111111111"))
                .andExpect(status().isNotFound());
    }
}