package ru.student.testing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import ru.student.testing.service.IMetricService;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket обработчик для отправки метрик в реальном времени.
 * Поддерживает постоянное соединение с клиентами для трансляции данных.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketHandler extends TextWebSocketHandler {

    private final IMetricService metricService;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        log.info("🔌 Новое WebSocket подключение: {}", sessionId);

        sendLatestMetrics(session);

        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (session.isOpen()) {
                    sendLatestMetrics(session);
                }
            } catch (Exception e) {
                log.error("Ошибка отправки метрик через WebSocket: {}", e.getMessage());
            }
        }, 5, 5, TimeUnit.SECONDS);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionId = session.getId();
        sessions.remove(sessionId);
        log.info("🔌 WebSocket отключен: {}, статус: {}", sessionId, status);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.debug("Получено сообщение от клиента: {}", payload);

        if ("ping".equals(payload)) {
            session.sendMessage(new TextMessage("pong"));
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("Ошибка WebSocket для сессии {}: {}", session.getId(), exception.getMessage());
        sessions.remove(session.getId());
        try {
            session.close(CloseStatus.SERVER_ERROR);
        } catch (IOException e) {
            log.error("Ошибка закрытия сессии: {}", e.getMessage());
        }
    }

    /**
     * Отправляет последние метрики конкретному клиенту.
     */
    private void sendLatestMetrics(WebSocketSession session) {
        try {
            if (session.isOpen()) {
                Map<String, Double> metrics = metricService.getLatestAllMetrics();
                String json = objectMapper.writeValueAsString(metrics);
                session.sendMessage(new TextMessage(json));
                log.debug("Отправлены метрики клиенту: {}", session.getId());
            }
        } catch (IOException e) {
            log.error("Ошибка отправки сообщения клиенту {}: {}", session.getId(), e.getMessage());
            try {
                session.close(CloseStatus.SERVER_ERROR);
            } catch (IOException ex) {
                log.error("Ошибка закрытия сессии: {}", ex.getMessage());
            }
        }
    }

    /**
     * Отправляет метрики всем подключенным клиентам (broadcast).
     */
    public void broadcastMetrics() {
        if (sessions.isEmpty()) {
            return;
        }

        try {
            Map<String, Double> metrics = metricService.getLatestAllMetrics();
            String json = objectMapper.writeValueAsString(metrics);

            int sentCount = 0;
            for (WebSocketSession session : sessions.values()) {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(json));
                        sentCount++;
                    }
                } catch (IOException e) {
                    log.error("Ошибка отправки клиенту {}: {}", session.getId(), e.getMessage());
                }
            }
            log.debug("Broadcast метрик отправлен {} клиентам", sentCount);
        } catch (Exception e) {
            log.error("Ошибка broadcast метрик: {}", e.getMessage());
        }
    }

    /**
     * Получить количество активных подключений.
     */
    public int getActiveConnections() {
        return sessions.size();
    }
}