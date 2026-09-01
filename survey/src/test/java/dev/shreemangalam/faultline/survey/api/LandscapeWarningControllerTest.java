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
class LandscapeWarningControllerTest {
    @Autowired private MockMvc mockMvc;
    @Test
    void exposesTheBundledLegacyCycleAsAnOperatorWarning() throws Exception {
        mockMvc.perform(get("/api/v1/landscape/warnings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warnings[0]").value(org.hamcrest.Matchers.containsString("CPI_INVENTORY_SYNC")));
    }
}