package dev.shreemangalam.faultline.survey.api;

import dev.shreemangalam.faultline.shared.domain.SystemId;
import dev.shreemangalam.faultline.survey.domain.UnknownSystemException;
import dev.shreemangalam.faultline.survey.service.RecoverySequenceService;
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
public class RecoverySequenceController {

    private final RecoverySequenceService recoverySequenceService;

    public RecoverySequenceController(RecoverySequenceService recoverySequenceService) {
        this.recoverySequenceService = recoverySequenceService;
    }

    @PostMapping(path = "/recovery-sequence", consumes = MediaType.APPLICATION_JSON_VALUE)
    public RecoverySequenceResponse plan(@Valid @RequestBody ImpactAnalysisRequest request) {
        return RecoverySequenceResponse.from(recoverySequenceService.plan(new SystemId(request.systemId())));
    }

    @ExceptionHandler(UnknownSystemException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    void unknownSystem() {
    }
}
