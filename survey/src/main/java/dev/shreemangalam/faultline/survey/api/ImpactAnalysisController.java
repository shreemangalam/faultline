package dev.shreemangalam.faultline.survey.api;

import dev.shreemangalam.faultline.shared.domain.SystemId;
import dev.shreemangalam.faultline.survey.domain.UnknownSystemException;
import dev.shreemangalam.faultline.survey.service.ImpactReportService;
import dev.shreemangalam.faultline.survey.service.StoredImpactReport;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/v1/analysis", produces = MediaType.APPLICATION_JSON_VALUE)
public class ImpactAnalysisController {
    private final ImpactReportService impactReportService;
    public ImpactAnalysisController(ImpactReportService impactReportService) { this.impactReportService = impactReportService; }

    @PostMapping(path = "/impact", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StoredImpactReportResponse> analyze(@Valid @RequestBody ImpactAnalysisRequest request) {
        StoredImpactReport report = impactReportService.create(new SystemId(request.systemId()), request.degradationType());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{reportId}").buildAndExpand(report.reportId()).toUri();
        return ResponseEntity.created(location).body(StoredImpactReportResponse.from(report));
    }

    @GetMapping("/impact/{reportId}")
    public ResponseEntity<StoredImpactReportResponse> findById(@PathVariable UUID reportId) {
        return impactReportService.findById(reportId).map(StoredImpactReportResponse::from).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @ExceptionHandler(UnknownSystemException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    void unknownSystem() { }
}