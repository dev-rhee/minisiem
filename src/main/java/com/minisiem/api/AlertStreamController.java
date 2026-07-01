package com.minisiem.api;

import com.minisiem.correlation.AlertEventPublisher;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
@Tag(name = "Alert Stream", description = "실시간 경보 스트림 (SSE)")
public class AlertStreamController {

    private final AlertEventPublisher alertEventPublisher;

    @GetMapping(value = "/stream", produces = "text/event-stream")
    @Operation(summary = "실시간 경보 스트림 구독", description = "SSE로 새 경보를 실시간으로 받습니다.")
    public SseEmitter stream() {
        return alertEventPublisher.subscribe();
    }
}