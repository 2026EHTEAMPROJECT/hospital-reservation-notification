package com.hospital.notification.service;

import com.hospital.notification.dto.NotificationRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class LiveNotificationService {

    private static final int MAX_RECENT = 50;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final List<NotificationRecord> recentNotifications = new ArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        return emitter;
    }

    public synchronized List<NotificationRecord> getRecentNotifications() {
        return List.copyOf(recentNotifications);
    }

    public void publish(String type, String message) {
        NotificationRecord record = new NotificationRecord(type, message, OffsetDateTime.now().format(FORMATTER));
        store(record);
        broadcast(record);
    }

    private synchronized void store(NotificationRecord record) {
        recentNotifications.add(record);
        if (recentNotifications.size() > MAX_RECENT) {
            recentNotifications.remove(0);
        }
    }

    private void broadcast(NotificationRecord record) {
        List<SseEmitter> stale = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("notification").data(record));
            } catch (IOException e) {
                stale.add(emitter);
            }
        }
        emitters.removeAll(stale);
    }
}
