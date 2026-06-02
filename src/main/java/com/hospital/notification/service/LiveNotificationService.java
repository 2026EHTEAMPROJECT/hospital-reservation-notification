package com.hospital.notification.service;

import com.hospital.notification.dto.NotificationRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class LiveNotificationService {

    private static final int MAX_RECENT = 50;
    private static final long SSE_TIMEOUT = 300_000L; // 5분
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<SseEmitter>> emitterMap =
            new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, List<NotificationRecord>> recentMap =
            new ConcurrentHashMap<>();

    public SseEmitter subscribe(String userId) {

        validateUserId(userId);

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        emitterMap
                .computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>())
                .add(emitter);

        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError(e -> removeEmitter(userId, emitter));

        // ★ 추가
        try {
            emitter.send(
                    SseEmitter.event()
                            .name("connect")
                            .data("connected")
            );
        } catch (IOException e) {
            removeEmitter(userId, emitter);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    public synchronized List<NotificationRecord> getRecentNotifications(String userId) {

        validateUserId(userId);

        return List.copyOf(
                recentMap.getOrDefault(
                        userId,
                        Collections.emptyList()
                )
        );
    }

    public void publish(
            String userId,
            String type,
            String message
    ) {

        validateUserId(userId);

        NotificationRecord record =
                new NotificationRecord(
                        type,
                        message,
                        OffsetDateTime.now().format(FORMATTER)
                );

        store(userId, record);

        broadcastToUser(userId, record);
    }

    private synchronized void store(
            String userId,
            NotificationRecord record
    ) {

        List<NotificationRecord> list =
                recentMap.computeIfAbsent(
                        userId,
                        k -> new ArrayList<>()
                );

        list.add(record);

        if (list.size() > MAX_RECENT) {
            list.remove(0);
        }
    }

    private void broadcastToUser(
            String userId,
            NotificationRecord record
    ) {

        CopyOnWriteArrayList<SseEmitter> emitters =
                emitterMap.get(userId);

        if (emitters == null) {
            return;
        }

        List<SseEmitter> stale = new ArrayList<>();

        for (SseEmitter emitter : emitters) {

            try {

                emitter.send(
                        SseEmitter.event()
                                .name("notification")
                                .data(record)
                );

            } catch (IOException e) {

                stale.add(emitter);
            }
        }

        emitters.removeAll(stale);

        if (emitters.isEmpty()) {
            emitterMap.remove(userId, emitters);
        }
    }

    private void removeEmitter(
            String userId,
            SseEmitter emitter
    ) {

        CopyOnWriteArrayList<SseEmitter> emitters =
                emitterMap.get(userId);

        if (emitters != null) {

            emitters.remove(emitter);

            if (emitters.isEmpty()) {
                emitterMap.remove(userId, emitters);
            }
        }
    }

    private void validateUserId(String userId) {

        if (userId == null || userId.isBlank()) {

            throw new IllegalArgumentException(
                    "userId는 null이거나 빈 값일 수 없습니다."
            );
        }
    }
}