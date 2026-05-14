package com.jmarket.notification.service;

import com.jmarket.notification.dto.NotificationEventResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class NotificationSseService {

    private static final long SSE_TIMEOUT_MILLIS = 0L;

    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emittersByUser = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        emittersByUser.computeIfAbsent(userId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);

        Runnable cleanup = () -> removeEmitter(userId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(ignored -> cleanup.run());

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("ok"));
        } catch (IOException | IllegalStateException ex) {
            cleanup.run();
            safeComplete(emitter);
        }
        return emitter;
    }

    public void sendToUser(Long userId, NotificationEventResponse event) {
        List<SseEmitter> emitters = emittersByUser.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .id(String.valueOf(event.notificationId()))
                        .name("notification")
                        .data(event, MediaType.APPLICATION_JSON));
            } catch (IOException | IllegalStateException ex) {
                removeEmitter(userId, emitter);
                safeComplete(emitter);
            }
        }
    }

    @Scheduled(fixedRateString = "${notification.sse-heartbeat-ms:25000}")
    public void sendHeartbeat() {
        for (Map.Entry<Long, CopyOnWriteArrayList<SseEmitter>> entry : emittersByUser.entrySet()) {
            Long userId = entry.getKey();
            List<SseEmitter> emitters = entry.getValue();
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("ping")
                            .data(Instant.now().toString()));
                } catch (IOException | IllegalStateException ex) {
                    removeEmitter(userId, emitter);
                    safeComplete(emitter);
                }
            }
        }
    }

    private void removeEmitter(Long userId, SseEmitter emitter) {
        List<SseEmitter> emitters = emittersByUser.get(userId);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emittersByUser.remove(userId);
        }
    }

    private void safeComplete(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (IllegalStateException ignored) {
            // Async context already completed/errored by container.
        }
    }
}
