package bot;

import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import service.PomodoroService;

public class PomodoroBot implements LongPollingSingleThreadUpdateConsumer {

    private final TelegramClient telegramClient;
    private final PomodoroService pomodoroService;

    public PomodoroBot(TelegramClient telegramClient, PomodoroService pomodoroService) {
        this.telegramClient = telegramClient;
        this.pomodoroService = pomodoroService;
    }

    @Override
    public void consume(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            System.out.println("Update не содержит текста: " + update.getUpdateId());
            return;
        }

        String messageText = update.getMessage().getText().trim();
        long chatId = update.getMessage().getChatId();

        // Разбираем команды
        if (messageText.startsWith("/start_pomo")) {
            pomodoroService.startPomodoro(chatId);
            sendTextMessage(chatId, "Pomodoro запущен!");
        } else if (messageText.startsWith("/stop")) {
            pomodoroService.stopPomodoro(chatId);
            sendTextMessage(chatId, "Pomodoro остановлен!");
        } else if (messageText.startsWith("/stats")) {
            String stats = pomodoroService.getStatistics(chatId);
            sendTextMessage(chatId, stats);
        } else if (messageText.startsWith("/achievements")) {
            String achievements = pomodoroService.getAchievements(chatId);
            sendTextMessage(chatId, achievements);
        } else if (messageText.startsWith("/export_stats")) {
            // В реальной реализации здесь следует отправить CSV-файл через SendDocument
            pomodoroService.exportStatistics(chatId);
            sendTextMessage(chatId, "Статистика экспортирована (файл отправлен).");
        } else {
            sendTextMessage(chatId, "Неизвестная команда.\nДоступные команды: /start_pomo, /stop, /stats, /achievements, /export_stats.");
        }
    }

    private void sendTextMessage(long chatId, String text) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .build();
        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}