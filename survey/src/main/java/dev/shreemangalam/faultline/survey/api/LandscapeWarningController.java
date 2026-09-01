package dev.shreemangalam.faultline.survey.api;

import dev.shreemangalam.faultline.survey.service.LandscapeWarningService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(path = "/api/v1/landscape/warnings", produces = MediaType.APPLICATION_JSON_VALUE)
public class LandscapeWarningController {
    private final LandscapeWarningService warningService;
    public LandscapeWarningController(LandscapeWarningService warningService) { this.warningService = warningService; }
    @GetMapping public WarningResponse warnings() { return new WarningResponse(warningService.warnings()); }
    public record WarningResponse(List<String> warnings) { }
}