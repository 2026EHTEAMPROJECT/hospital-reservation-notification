package com.hospital.notification.controller;

import com.hospital.notification.dto.NotificationRecord;
import com.hospital.notification.service.LiveNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final LiveNotificationService liveNotificationService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam String patientId) {
        return liveNotificationService.subscribe(patientId);
    }

    @GetMapping
    public ResponseEntity<List<NotificationRecord>> getRecent(@RequestParam String patientId) {
        return ResponseEntity.ok(liveNotificationService.getRecentNotifications(patientId));
    }
}
