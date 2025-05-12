package repository;

import java.time.LocalDateTime;

public interface UserDataRepository {
    // Создание записи о новом сеансе (WORK или REST)
    void recordSession(long chatId, String sessionType, int durationMinutes, LocalDateTime startAt);

    // Обновление записи о сеансе (завершение периода)
    void completeSession(long chatId, String sessionType, LocalDateTime stopAt);

    // Фиксация ручной остановки сеанса
    void markSessionStopped(long chatId, LocalDateTime stopAt);

    // Получение статистики в виде строки
    String getStatistics(long chatId);

    // Получение списка достижений
    String getAchievements(long chatId);

    // Экспорт статистики в виде CSV (возвращаем массив байт)
    byte[] exportStatistics(long chatId);
}
