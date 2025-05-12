package service;

public interface PomodoroService {
    void startPomodoro(long chatId);
    void stopPomodoro(long chatId);
    String getStatistics(long chatId);
    String getAchievements(long chatId);
    void exportStatistics(long chatId); // Для отправки CSV-файла
}
