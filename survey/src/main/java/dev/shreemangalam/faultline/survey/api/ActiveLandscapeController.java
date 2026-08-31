package dev.shreemangalam.faultline.survey.api;

import dev.shreemangalam.faultline.survey.service.ActiveLandscapeService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/landscape/active", produces = MediaType.APPLICATION_JSON_VALUE)
public class ActiveLandscapeController {

    private final ActiveLandscapeService activeLandscapeService;

    public ActiveLandscapeController(ActiveLandscapeService activeLandscapeService) {
        this.activeLandscapeService = activeLandscapeService;
    }

    @GetMapping
    public ResponseEntity<StoredLandscapeResponse> activeLandscape() {
        return activeLandscapeService.activeLandscape()
                .map(StoredLandscapeResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}