package dev.shreemangalam.faultline.survey.api;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/survey", produces = MediaType.APPLICATION_JSON_VALUE)
public class SurveyStatusController {

    @GetMapping("/status")
    public SurveyStatusResponse status() {
        return new SurveyStatusResponse("survey", "FOUNDATION_READY");
    }
}
