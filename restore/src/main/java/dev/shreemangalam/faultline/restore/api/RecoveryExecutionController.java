package dev.shreemangalam.faultline.restore.api;

import dev.shreemangalam.faultline.restore.execution.ExecuteRecoveryBatchCommand;
import dev.shreemangalam.faultline.restore.execution.RecoveryBatchExecutor;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(path = "/api/v1/worker", produces = MediaType.APPLICATION_JSON_VALUE)
public class RecoveryExecutionController {
    private final RecoveryBatchExecutor executor;

    public RecoveryExecutionController(RecoveryBatchExecutor executor) {
        this.executor = executor;
    }

    @PostMapping(path = "/run-once", consumes = MediaType.APPLICATION_JSON_VALUE)
    public List<ExecutedRecoveryEventResponse> execute(@Valid @RequestBody ExecuteRecoveryBatchRequest request) {
        return executor.execute(new ExecuteRecoveryBatchCommand(request.workerId(), request.maxEvents(), request.leaseSeconds()))
                .stream()
                .map(event -> new ExecutedRecoveryEventResponse(
                        event.eventId(), event.state(), event.classification(), event.httpStatus(), event.durationMillis(),
                        event.reason(), event.releasedEventIds()))
                .toList();
    }
}
