package dev.shreemangalam.faultline.survey.api;

import dev.shreemangalam.faultline.survey.service.LandscapeImportValidationService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/landscape", produces = MediaType.APPLICATION_JSON_VALUE)
public class LandscapeImportValidationController {

    private final LandscapeImportValidationService landscapeImportValidationService;

    public LandscapeImportValidationController(LandscapeImportValidationService landscapeImportValidationService) {
        this.landscapeImportValidationService = landscapeImportValidationService;
    }

    @PostMapping(path = "/import/validate", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LandscapeImportValidationResponse> validate(@RequestBody LandscapeImportValidationRequest request) {
        LandscapeImportValidationResponse response = LandscapeImportValidationResponse.from(
                landscapeImportValidationService.validate(request.toDefinition()));
        return response.valid() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }
}
