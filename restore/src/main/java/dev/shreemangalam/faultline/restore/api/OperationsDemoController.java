package dev.shreemangalam.faultline.restore.api;

import dev.shreemangalam.faultline.restore.service.OperationsDemoService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/scenarios/operations-demo")
public class OperationsDemoController {
    private final OperationsDemoService service;
    public OperationsDemoController(OperationsDemoService service) { this.service = service; }
    @GetMapping
    public OperationsDemoService.Status status() { return service.status(); }
    @PostMapping
    public OperationsDemoService.Status load() throws InterruptedException { return service.load(); }
    @PostMapping("/recover")
    public OperationsDemoService.Status recover() { return service.recover(); }
}
