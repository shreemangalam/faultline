package dev.shreemangalam.faultline.restore.api;

import dev.shreemangalam.faultline.restore.streaming.RecoveryEventStream;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/stream")
public class RecoveryStreamController {
    private final RecoveryEventStream stream;

    public RecoveryStreamController(RecoveryEventStream stream) {
        this.stream = stream;
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");
        return stream.subscribe();
    }
}
