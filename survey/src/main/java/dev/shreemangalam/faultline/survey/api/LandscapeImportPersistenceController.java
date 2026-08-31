package dev.shreemangalam.faultline.survey.api;

import dev.shreemangalam.faultline.survey.domain.LandscapeImportValidation;
import dev.shreemangalam.faultline.survey.service.ActiveLandscapeService;
import dev.shreemangalam.faultline.survey.service.LandscapeImportPersistenceService;
import dev.shreemangalam.faultline.survey.service.LandscapeImportValidationService;
import dev.shreemangalam.faultline.survey.service.StoredLandscape;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/v1/landscape/imports", produces = MediaType.APPLICATION_JSON_VALUE)
public class LandscapeImportPersistenceController {

    private final LandscapeImportValidationService validationService;
    private final LandscapeImportPersistenceService persistenceService;
    private final ActiveLandscapeService activeLandscapeService;

    public LandscapeImportPersistenceController(
            LandscapeImportValidationService validationService,
            LandscapeImportPersistenceService persistenceService,
            ActiveLandscapeService activeLandscapeService) {
        this.validationService = validationService;
        this.persistenceService = persistenceService;
        this.activeLandscapeService = activeLandscapeService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> store(@RequestBody LandscapeImportValidationRequest request) {
        LandscapeImportValidation validation = validationService.validate(request.toDefinition());
        if (!validation.valid()) {
            return ResponseEntity.badRequest().body(LandscapeImportValidationResponse.from(validation));
        }

        StoredLandscape storedLandscape = persistenceService.store(request.toDefinition());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{importId}")
                .buildAndExpand(storedLandscape.importId())
                .toUri();
        return ResponseEntity.created(location).body(StoredLandscapeResponse.from(storedLandscape));
    }

    @GetMapping("/{importId}")
    public ResponseEntity<StoredLandscapeResponse> findById(@PathVariable UUID importId) {
        return persistenceService.findById(importId)
                .map(StoredLandscapeResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{importId}/activate")
    public ResponseEntity<StoredLandscapeResponse> activate(@PathVariable UUID importId) {
        return activeLandscapeService.activate(importId)
                .map(StoredLandscapeResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}