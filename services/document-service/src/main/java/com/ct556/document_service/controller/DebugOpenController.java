package com.ct556.document_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DebugOpenController {

    @GetMapping("/debug-open")
    public String debugOpen() {
        return "DOCUMENT SERVICE IS REALLY RUNNING";
    }
}