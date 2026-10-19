package dev.shreemangalam.faultline.survey.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "faultline.security.enabled=true",
        "faultline.security.api-key=test-operator-api-key"
})
@AutoConfigureMockMvc
class ApiSecurityTest {
    private static final String API_KEY = "test-operator-api-key";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicReadsHaveSecurityHeadersAndMutationsRequireTheOperatorKey() throws Exception {
        mockMvc.perform(get("/api/v1/landscape/health-map"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"));

        String incident = """
                {"systemId":"SAP_ECC","degradationType":"COMPLETE_OUTAGE"}
                """;
        mockMvc.perform(post("/api/v1/analysis/impact").contentType(MediaType.APPLICATION_JSON).content(incident))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "ApiKey realm=\"faultline\""))
                .andExpect(jsonPath("$.status").value(401));
        mockMvc.perform(post("/api/v1/analysis/impact").header("X-Faultline-Api-Key", "incorrect-api-key-value")
                        .contentType(MediaType.APPLICATION_JSON).content(incident))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/analysis/impact").header("X-Faultline-Api-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(incident))
                .andExpect(status().isCreated());
    }

    @Test
    void generatedContractMarksOnlyMutationsAsProtected() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.operatorApiKey.name").value("X-Faultline-Api-Key"))
                .andExpect(jsonPath("$['paths']['/api/v1/analysis/impact']['post']['security'][0]['operatorApiKey']").isArray())
                .andExpect(jsonPath("$['paths']['/api/v1/landscape/health-map']['get']['security']").doesNotExist());
    }
}
