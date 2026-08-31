package dev.shreemangalam.faultline.survey.api;

import dev.shreemangalam.faultline.survey.service.LandscapeHealthMapService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/landscape", produces = MediaType.APPLICATION_JSON_VALUE)
public class LandscapeHealthMapController {

    private final LandscapeHealthMapService landscapeHealthMapService;

    public LandscapeHealthMapController(LandscapeHealthMapService landscapeHealthMapService) {
        this.landscapeHealthMapService = landscapeHealthMapService;
    }

    @GetMapping("/health-map")
    public LandscapeHealthMapResponse healthMap() {
        return LandscapeHealthMapResponse.from(landscapeHealthMapService.healthMap());
    }
}
