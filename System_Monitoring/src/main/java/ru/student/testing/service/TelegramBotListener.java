package ru.student.testing.service;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.student.testing.util.TelegramNotifier;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Слушатель входящих сообщений Telegram-бота.
 *
 * Работает через long polling: отдельный daemon-поток циклически
 * дёргает getUpdates и обрабатывает новые сообщения.
 *
 * Поддерживаемые команды:
 *   /start          — приветствие и меню
 *   /help, /помощь  — справка
 *   /отчет N        — отчёт о состоянии системы за последние N минут
 *   /report N       — то же самое (латиницей)
 *
 * Отвечает ВСЕГДА в тот чат, откуда пришла команда,
 * независимо от telegram.bot.chat-id в properties.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramBotListener {

    private final TelegramNotifier telegramNotifier;
    private final IMetricService metricService;

    @Value("${telegram.bot.enabled:false}")
    private boolean enabled;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread pollingThread;
    private long lastUpdateId = 0;

    @PostConstruct
    public void start() {
        if (!enabled) {
            log.info("Telegram-бот отключён (telegram.bot.enabled=false)");
            return;
        }

        running.set(true);
        pollingThread = new Thread(this::pollLoop, "telegram-bot-polling");
        pollingThread.setDaemon(true);
        pollingThread.start();

        log.info("🤖 Telegram-бот запущен (long polling)");
    }

    @PreDestroy
    public void stop() {
        running.set(false);
        if (pollingThread != null) {
            pollingThread.interrupt();
        }
        log.info("🤖 Telegram-бот остановлен");
    }

    /**
     * Раз в ~1.5 секунды тянем новые апдейты.
     * На старте «проглатываем» все старые апдейты, чтобы бот не отвечал
     * на команды, отправленные до перезапуска приложения.
     */
    private void pollLoop() {
        // Сброс очереди апдейтов, накопившихся до старта
        JsonNode initial = telegramNotifier.getUpdates(0, 0);
        if (initial != null && initial.path("result").isArray()) {
            for (JsonNode update : initial.path("result")) {
                lastUpdateId = Math.max(lastUpdateId, update.path("update_id").asLong());
            }
            lastUpdateId++;
            log.info("Стартовый offset для getUpdates: {}", lastUpdateId);
        }

        while (running.get()) {
            try {
                JsonNode response = telegramNotifier.getUpdates(lastUpdateId, 2);

                if (response != null && response.path("ok").asBoolean()) {
                    JsonNode result = response.path("result");
                    for (JsonNode update : result) {
                        lastUpdateId = Math.max(lastUpdateId, update.path("update_id").asLong()) + 1;
                        try {
                            handleUpdate(update);
                        } catch (Exception e) {
                            log.error("Ошибка обработки апдейта: {}", e.getMessage(), e);
                        }
                    }
                }

                Thread.sleep(1500);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Ошибка polling-цикла: {}", e.getMessage());
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }


    private void handleUpdate(JsonNode update) {
        JsonNode message = update.path("message");
        if (message.isMissingNode()) {
            return; // не текстовое сообщение (фото, стикер и т.п.)
        }

        long chatId = message.path("chat").path("id").asLong();
        String text = message.path("text").asText("").trim();

        if (text.isEmpty() || chatId == 0) {
            return;
        }

        log.debug("Получена команда от {}: {}", chatId, text);

        String command = text.split("\\s+")[0].toLowerCase();
        if (command.contains("@")) {
            command = command.substring(0, command.indexOf("@"));
        }

        String cid = String.valueOf(chatId);

        try {
            switch (command) {
                case "/start" -> handleStart(cid);
                case "/help", "/помощь" -> handleHelp(cid);
                case "/отчет", "/report", "/отчёт" -> handleReport(cid, text);
                default -> {
                    if (text.startsWith("/")) {
                        telegramNotifier.sendMessage(cid,
                                "🤔 Неизвестная команда. Напиши /help", "HTML");
                    }
                }
            }
        } catch (Exception e) {
            log.error("Ошибка обработки команды '{}' от {}: {}", text, chatId, e.getMessage(), e);
            telegramNotifier.sendMessage(cid,
                    "❌ Ошибка выполнения: " + escapeHtml(e.getMessage()), "HTML");
        }
    }


    private void handleStart(String cid) {
        String msg = """
                👋 <b>Привет! Я бот мониторинга системы.</b>

                Я умею показывать состояние сервера в реальном времени.

                <b>Доступные команды:</b>
                /отчет 5   — отчёт за последние 5 минут
                /отчет 10  — отчёт за последние 10 минут
                /отчет 30  — отчёт за последние 30 минут
                /отчет N   — отчёт за N минут (1..1440)

                /help — эта справка
                """;
        telegramNotifier.sendMessage(cid, msg, "HTML");
    }

    private void handleHelp(String cid) {
        String msg = """
                📋 <b>Справка</b>

                <b>/отчет N</b> — отчёт о состоянии системы за последние N минут.

                Примеры:
                  /отчет 5    — за 5 минут
                  /отчет 15   — за 15 минут
                  /отчет 60   — за час
                  /отчет 1440 — за сутки

                N — целое число от 1 до 1440.

                Отчёт содержит:
                  • min / avg / max по CPU, RAM, диску, сети
                  • количество активных алертов
                """;
        telegramNotifier.sendMessage(cid, msg, "HTML");
    }

    private void handleReport(String cid, String text) {
        String[] parts = text.trim().split("\\s+");

        if (parts.length < 2) {
            telegramNotifier.sendMessage(cid,
                    "Укажи период, например: <code>/отчет 10</code>", "HTML");
            return;
        }

        int minutes;
        try {
            minutes = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            telegramNotifier.sendMessage(cid,
                    "❌ '" + escapeHtml(parts[1]) + "' не число. Пример: <code>/отчет 10</code>",
                    "HTML");
            return;
        }

        if (minutes <= 0 || minutes > 1440) {
            telegramNotifier.sendMessage(cid,
                    "❌ Период должен быть от 1 до 1440 минут", "HTML");
            return;
        }

        telegramNotifier.sendMessage(cid, "⏳ Собираю отчёт за " + minutes + " мин...", null);

        try {
            String report = metricService.buildReportForLastMinutes(minutes);
            telegramNotifier.sendMessage(cid, report, "HTML");
            log.info("Отчёт за {} мин отправлен в чат {}", minutes, cid);
        } catch (Exception e) {
            log.error("Ошибка сборки отчёта за {} мин: {}", minutes, e.getMessage(), e);
            telegramNotifier.sendMessage(cid,
                    "❌ Не удалось собрать отчёт: " + escapeHtml(e.getMessage()), "HTML");
        }
    }

    /**
     * Простая экранизация HTML для безопасной вставки текста
     * в сообщения с parse_mode=HTML.
     */
    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}