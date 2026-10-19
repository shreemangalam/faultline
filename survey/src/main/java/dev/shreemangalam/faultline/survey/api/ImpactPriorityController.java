package dev.shreemangalam.faultline.survey.api;

import dev.shreemangalam.faultline.shared.domain.SystemId;
import dev.shreemangalam.faultline.survey.service.ImpactPriorityService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/analysis", produces = MediaType.APPLICATION_JSON_VALUE)
public class ImpactPriorityController {

    private final ImpactPriorityService impactPriorityService;

    public ImpactPriorityController(ImpactPriorityService impactPriorityService) {
        this.impactPriorityService = impactPriorityService;
    }

    @PostMapping(path = "/impact-priorities", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ImpactPriorityResponse prioritize(@Valid @RequestBody ImpactAnalysisRequest request) {
        return ImpactPriorityResponse.from(impactPriorityService.prioritize(new SystemId(request.systemId())));
    }
}
