package dev.shreemangalam.faultline.restore.api;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/restore", produces = MediaType.APPLICATION_JSON_VALUE)
public class RestoreStatusController {

    @GetMapping("/status")
    public RestoreStatusResponse status() {
        return new RestoreStatusResponse("restore", "FOUNDATION_READY");
    }
}
