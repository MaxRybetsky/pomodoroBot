package service;

import config.Config;
import repository.UserDataRepository;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

public class PomodoroServiceImpl implements PomodoroService {

    private final UserDataRepository userDataRepository;
    private final TelegramClient telegramClient;
    private final int workDuration; // длительность рабочего периода в минутах
    private final int restDuration; // длительность периода отдыха в минутах

    // Пул для планирования задач
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    // Список запланированных задач для каждого чата
    private final Map<Long, List<ScheduledFuture<?>>> activeTasks = new ConcurrentHashMap<>();

    public PomodoroServiceImpl(UserDataRepository userDataRepository, TelegramClient telegramClient, Config config) {
        this.userDataRepository = userDataRepository;
        this.telegramClient = telegramClient;
        this.workDuration = config.workDuration();
        this.restDuration = config.restDuration();
    }

    @Override
    public void startPomodoro(long chatId) {
        if (activeTasks.containsKey(chatId)) {
            sendMessage(chatId, "Таймер Pomodoro уже запущен.");
            return;
        }

        // Инициализируем список задач для данного чата
        List<ScheduledFuture<?>> tasks = new ArrayList<>();
        activeTasks.put(chatId, tasks);

        // Запуск рабочего периода
        sendMessage(chatId, "Начинается рабочий период (" + workDuration + " мин).");
        userDataRepository.recordSession(chatId, "WORK", workDuration, LocalDateTime.now());

        // Планируем завершение рабочего периода через workDuration минут
        ScheduledFuture<?> workTask = scheduler.schedule(() -> {
            userDataRepository.completeSession(chatId, "WORK", LocalDateTime.now());
            sendMessage(chatId, "Пора отдыхать!");

            // Запуск периода отдыха
            userDataRepository.recordSession(chatId, "REST", restDuration, LocalDateTime.now());
            sendMessage(chatId, "Период отдыха (" + restDuration + " мин) начался.");

            // Планируем завершение отдыха через restDuration минут
            ScheduledFuture<?> restTask = scheduler.schedule(() -> {
                userDataRepository.completeSession(chatId, "REST", LocalDateTime.now());
                sendMessageWithMotivationalImage(chatId, "Время поработать!");
                activeTasks.remove(chatId);
            }, restDuration, TimeUnit.MINUTES);

            // Добавляем задачу для завершения отдыха
            tasks.add(restTask);
        }, workDuration, TimeUnit.MINUTES);

        // Сохраняем задачу завершения рабочего периода
        tasks.add(workTask);
    }

    @Override
    public void stopPomodoro(long chatId) {
        List<ScheduledFuture<?>> tasks = activeTasks.get(chatId);
        if (tasks != null && !tasks.isEmpty()) {
            // Отменяем все запланированные задачи для данного чата
            for (ScheduledFuture<?> future : tasks) {
                future.cancel(false);
            }
            activeTasks.remove(chatId);
            sendMessage(chatId, "Таймер Pomodoro остановлен.");
            userDataRepository.markSessionStopped(chatId, LocalDateTime.now());
        } else {
            sendMessage(chatId, "Нет активного таймера для остановки.");
        }
    }

    @Override
    public String getStatistics(long chatId) {
        return userDataRepository.getStatistics(chatId);
    }

    @Override
    public String getAchievements(long chatId) {
        return userDataRepository.getAchievements(chatId);
    }

    @Override
    public void exportStatistics(long chatId) {
        byte[] csvData = userDataRepository.exportStatistics(chatId);
        System.out.println("Экспорт статистики для chatId=" + chatId + " (размер файла " + csvData.length + " байт).");
    }

    // ───────── Вспомогательные методы для отправки сообщений ─────────

    private void sendMessage(long chatId, String text) {
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

    private void sendMessageWithMotivationalImage(long chatId, String text) {
        // Здесь можно использовать SendPhoto для отправки изображения.
        sendMessage(chatId, text + "\n(Мотивационное изображение отправлено)");
    }
}
