package dev.shreemangalam.faultline.survey.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SurveyStatusController.class)
@AutoConfigureMockMvc(addFilters = false)
class SurveyStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exposesTheFoundationStatusContract() throws Exception {
        mockMvc.perform(get("/api/v1/survey/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.module").value("survey"))
                .andExpect(jsonPath("$.status").value("FOUNDATION_READY"));
    }
}
