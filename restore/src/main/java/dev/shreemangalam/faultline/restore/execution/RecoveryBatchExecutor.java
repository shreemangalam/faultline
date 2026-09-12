package dev.shreemangalam.faultline.restore.execution;

import dev.shreemangalam.faultline.restore.domain.RecoveryEventState;
import dev.shreemangalam.faultline.restore.service.ClaimRecoveryEventsCommand;
import dev.shreemangalam.faultline.restore.service.ClaimedRecoveryEvent;
import dev.shreemangalam.faultline.restore.service.CompleteRecoveryEventCommand;
import dev.shreemangalam.faultline.restore.service.FailRecoveryEventCommand;
import dev.shreemangalam.faultline.restore.service.RecoveryEventLeaseService;
import dev.shreemangalam.faultline.restore.service.RetryRecoveryEventCommand;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RecoveryBatchExecutor {
    private final RecoveryEventLeaseService leaseService;
    private final DownstreamDeliveryClient downstreamClient;

    public RecoveryBatchExecutor(RecoveryEventLeaseService leaseService, DownstreamDeliveryClient downstreamClient) {
        this.leaseService = leaseService;
        this.downstreamClient = downstreamClient;
    }

    public List<ExecutedRecoveryEvent> execute(ExecuteRecoveryBatchCommand command) {
        return leaseService.claim(new ClaimRecoveryEventsCommand(command.workerId(), command.maxEvents(), command.leaseSeconds()))
                .stream()
                .map(event -> execute(event, command.workerId()))
                .toList();
    }

    private ExecutedRecoveryEvent execute(ClaimedRecoveryEvent event, String workerId) {
        DownstreamExecutionResult downstream = downstreamClient.deliver(event);
        return switch (downstream.classification()) {
            case SUCCESS -> {
                var completed = leaseService.complete(new CompleteRecoveryEventCommand(
                        event.eventId(), workerId, downstream.httpStatus(), downstream.durationMillis()));
                yield new ExecutedRecoveryEvent(
                        event.eventId(), RecoveryEventState.COMPLETED,
                        downstream.classification(), downstream.httpStatus(), downstream.durationMillis(), downstream.reason(),
                        completed.releasedEventIds());
            }
            case TRANSIENT_FAILURE -> {
                var retry = leaseService.retry(new RetryRecoveryEventCommand(
                        event.eventId(), workerId, downstream.reason(), downstream.httpStatus(), downstream.durationMillis()));
                yield new ExecutedRecoveryEvent(
                        event.eventId(), retry.state(), downstream.classification(), downstream.httpStatus(),
                        downstream.durationMillis(), downstream.reason(), List.of());
            }
            case PERMANENT_FAILURE -> {
                var failed = leaseService.failFinal(new FailRecoveryEventCommand(
                        event.eventId(), workerId, downstream.reason(), downstream.httpStatus(), downstream.durationMillis()));
                yield new ExecutedRecoveryEvent(
                        event.eventId(), failed.state(), downstream.classification(), downstream.httpStatus(),
                        downstream.durationMillis(), downstream.reason(), List.of());
            }
        };
    }
}
