package dev.shreemangalam.faultline.survey.api;

import dev.shreemangalam.faultline.shared.domain.SystemId;
import dev.shreemangalam.faultline.survey.domain.UnknownSystemException;
import dev.shreemangalam.faultline.survey.service.ImpactAnalysisService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/analysis", produces = MediaType.APPLICATION_JSON_VALUE)
public class ImpactAnalysisController {

    private final ImpactAnalysisService impactAnalysisService;

    public ImpactAnalysisController(ImpactAnalysisService impactAnalysisService) {
        this.impactAnalysisService = impactAnalysisService;
    }

    @PostMapping(path = "/impact", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ImpactAnalysisResponse analyze(@Valid @RequestBody ImpactAnalysisRequest request) {
        return ImpactAnalysisResponse.from(
                impactAnalysisService.analyze(new SystemId(request.systemId())), request.degradationType());
    }

    @ExceptionHandler(UnknownSystemException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    void unknownSystem() {
    }
}
