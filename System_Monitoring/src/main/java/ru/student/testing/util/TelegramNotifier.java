package ru.student.testing.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Утилита для работы с Telegram Bot API.
 *
 * Умеет:
 *   - отправлять сообщения в чат (sendMessage)
 *   - получать входящие апдейты (getUpdates) — используется для long polling
 *
 * Ничего не сохраняет в БД. Вся конфигурация — через application.properties:
 *   telegram.bot.token    — токен бота (без префикса "bot")
 *   telegram.bot.chat-id  — чат по умолчанию (для служебных уведомлений)
 *   telegram.bot.enabled  — вкл/выкл интеграцию (true/false)
 */
@Slf4j
@Component
public class TelegramNotifier {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${telegram.bot.token:}")
    private String botToken;

    @Value("${telegram.bot.chat-id:}")
    private String chatId;

    @Value("${telegram.bot.enabled:false}")
    private boolean enabled;


    /**
     * Отправляет сообщение в конкретный chat_id (переопределяет дефолт из properties).
     */
    public boolean sendMessage(String chatId, String text, String parseMode) {
        if (!enabled) {
            log.info("Telegram-уведомления отключены (telegram.bot.enabled=false)");
            return false;
        }
        if (botToken == null || botToken.isBlank()) {
            log.warn("Telegram не настроен: telegram.bot.token пустой");
            return false;
        }
        if (chatId == null || chatId.isBlank()) {
            log.warn("chatId пустой — не могу отправить сообщение");
            return false;
        }

        try {
            String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";

            StringBuilder body = new StringBuilder();
            body.append("{")
                    .append("\"chat_id\":\"").append(escapeJson(chatId)).append("\",")
                    .append("\"text\":\"").append(escapeJson(text)).append("\"");

            if (parseMode != null && !parseMode.isBlank()) {
                body.append(",\"parse_mode\":\"").append(parseMode).append("\"");
            }
            body.append(",\"disable_web_page_preview\":true");
            body.append("}");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request = new HttpEntity<>(body.toString(), headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            boolean ok = response.getStatusCode().is2xxSuccessful();
            if (ok) {
                log.info("✈️ Сообщение отправлено в chat {} ({} символов)", chatId, text.length());
            } else {
                log.warn("Telegram вернул: {} — {}", response.getStatusCode(), response.getBody());
            }
            return ok;

        } catch (Exception e) {
            log.error("Ошибка отправки в Telegram (chat={}): {}", chatId, e.getMessage());
            return false;
        }
    }

    /**
     * Отправка в дефолтный чат из properties (используется для служебных уведомлений).
     */
    public boolean sendMessage(String text, String parseMode) {
        return sendMessage(this.chatId, text, parseMode);
    }

    /**
     * Отправка в дефолтный чат с HTML-разметкой.
     */
    public boolean sendMessage(String text) {
        return sendMessage(this.chatId, text, "HTML");
    }

    /**
     * Забирает новые апдейты у Telegram.
     */
    public JsonNode getUpdates(long offset, int timeout) {
        if (!enabled) {
            return null;
        }
        if (botToken == null || botToken.isBlank()) {
            log.warn("Telegram не настроен: telegram.bot.token пустой");
            return null;
        }

        try {
            String url = "https://api.telegram.org/bot" + botToken + "/getUpdates"
                    + "?offset=" + offset
                    + "&timeout=" + timeout
                    + "&allowed_updates=[\"message\"]";

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return objectMapper.readTree(response.getBody());

        } catch (Exception e) {
            log.debug("Ошибка getUpdates: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Простейшая экранизация спецсимволов для встраивания в JSON-строку.
     */
    private String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"'  -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }
}