package repository;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CsvUserDataRepository implements UserDataRepository {

    // Каталоги для хранения данных
    private static final String SESSIONS_DIR = "data/sessions/";
    private static final String STATS_DIR = "data/stats/";
    private static final String ACHIEVEMENTS_DIR = "data/achievements/";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // Индексы колонок CSV
    private static final int IDX_TYPE = 0;
    private static final int IDX_DURATION = 1;
    private static final int IDX_START_AT = 2;
    private static final int IDX_STOP_AT = 3;
    private static final int IDX_COMPLETED = 4;

    // Заголовок CSV-файла
    private static final String[] CSV_HEADER = {"type", "duration", "start_at", "stop_at", "completed"};

    public CsvUserDataRepository() {
        new File(SESSIONS_DIR).mkdirs();
        new File(STATS_DIR).mkdirs();
        new File(ACHIEVEMENTS_DIR).mkdirs();
    }

    /**
     * При запуске сессии создаётся запись с начальными данными.
     * Пример:
     * "WORK","25","2025-02-09 15:21","","false"
     */
    @Override
    public void recordSession(long chatId, String sessionType, int durationMinutes, LocalDateTime startAt) {
        String fileName = SESSIONS_DIR + chatId + ".csv";
        String[] record = new String[5];
        record[IDX_TYPE] = sessionType;
        record[IDX_DURATION] = String.valueOf(durationMinutes);
        record[IDX_START_AT] = startAt.format(FORMATTER);
        record[IDX_STOP_AT] = "";
        record[IDX_COMPLETED] = "false";
        appendRecord(fileName, record);
    }

    /**
     * При автоматическом завершении сессии (по таймеру) обновляем запись:
     * - Записываем время окончания в stop_at
     * - Устанавливаем completed = "true"
     *
     * Пример:
     * "WORK","25","2025-02-09 15:21","2025-02-09 15:46","true"
     */
    @Override
    public void completeSession(long chatId, String sessionType, LocalDateTime stopAt) {
        String fileName = SESSIONS_DIR + chatId + ".csv";
        List<String[]> records = readAllRecords(fileName);
        boolean updated = false;

        for (int i = records.size() - 1; i >= 0; i--) {
            String[] record = records.get(i);
            if (!isHeaderRecord(record)
                    && record[IDX_TYPE].equalsIgnoreCase(sessionType)
                    && record[IDX_STOP_AT].isEmpty()) {
                record[IDX_STOP_AT] = stopAt.format(FORMATTER);
                record[IDX_COMPLETED] = "true";
                updated = true;
                break;
            }
        }

        if (updated) {
            writeAllRecords(fileName, records);
        } else {
            System.err.println("Не найдена активная сессия " + sessionType + " для chatId=" + chatId);
        }
    }

    /**
     * При принудительной остановке сессии (команда /stop) обновляем запись:
     * - Записываем время остановки в stop_at
     * - Оставляем completed = "false"
     *
     * Пример:
     * "WORK","25","2025-02-09 15:21","2025-02-09 15:21","false"
     */
    @Override
    public void markSessionStopped(long chatId, LocalDateTime stopAt) {
        String fileName = SESSIONS_DIR + chatId + ".csv";
        List<String[]> records = readAllRecords(fileName);
        boolean updated = false;

        for (int i = records.size() - 1; i >= 0; i--) {
            String[] record = records.get(i);
            if (!isHeaderRecord(record) && record[IDX_STOP_AT].isEmpty()) {
                record[IDX_STOP_AT] = stopAt.format(FORMATTER);
                // completed остаётся "false"
                updated = true;
                break;
            }
        }

        if (updated) {
            writeAllRecords(fileName, records);
        } else {
            System.err.println("Не найдена активная сессия для остановки chatId=" + chatId);
        }
    }

    /**
     * Формирование статистики по завершённым сессиям.
     */
    @Override
    public String getStatistics(long chatId) {
        String fileName = SESSIONS_DIR + chatId + ".csv";
        int workMinutes = 0, restMinutes = 0, workCycles = 0, restCycles = 0;
        List<String[]> records = readAllRecords(fileName);

        for (String[] record : records) {
            if (isHeaderRecord(record)) {
                continue;
            }

            String type = record[IDX_TYPE];
            String durationStr = record[IDX_DURATION];
            String stopAt = record[IDX_STOP_AT];

            // Считаем только завершённые сессии (где задано время окончания)
            if (!stopAt.isEmpty() && durationStr != null && !durationStr.isEmpty()) {
                int duration = Integer.parseInt(durationStr);
                if ("WORK".equalsIgnoreCase(type)) {
                    workMinutes += duration;
                    workCycles++;
                } else if ("REST".equalsIgnoreCase(type)) {
                    restMinutes += duration;
                    restCycles++;
                }
            }
        }

        return "Статистика:\n" +
                "• Рабочее время: " + workMinutes + " мин\n" +
                "• Отдых: " + restMinutes + " мин\n" +
                "• Рабочих циклов: " + workCycles + "\n" +
                "• Циклов отдыха: " + restCycles;
    }

    @Override
    public String getAchievements(long chatId) {
        String fileName = ACHIEVEMENTS_DIR + "achievements_" + chatId + ".txt";
        try {
            List<String> lines = Files.readAllLines(Paths.get(fileName));
            return String.join("\n", lines);
        } catch (IOException e) {
            return "Достижений пока нет.";
        }
    }

    @Override
    public byte[] exportStatistics(long chatId) {
        String fileName = STATS_DIR + "stats_" + chatId + ".csv";
        try {
            Path path = Paths.get(fileName);
            if (Files.exists(path)) {
                return Files.readAllBytes(path);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new byte[0];
    }

    // ───────── Вспомогательные методы ─────────

    /**
     * Добавляет запись в конец CSV-файла.
     * Если файла нет — сначала записывается заголовок.
     */
    private void appendRecord(String fileName, String[] record) {
        boolean fileExists = new File(fileName).exists();
        try (CSVWriter writer = new CSVWriter(new FileWriter(fileName, true))) {
            if (!fileExists) {
                writer.writeNext(CSV_HEADER);
            }
            writer.writeNext(record);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Чтение всех записей из файла.
     */
    private List<String[]> readAllRecords(String fileName) {
        List<String[]> records = new ArrayList<>();
        File file = new File(fileName);
        if (!file.exists()) {
            return records;
        }
        try (CSVReader reader = new CSVReader(new FileReader(file))) {
            records = reader.readAll();
        } catch (IOException | CsvException e) {
            e.printStackTrace();
        }
        return records;
    }

    /**
     * Перезаписывает CSV-файл переданным списком записей.
     */
    private void writeAllRecords(String fileName, List<String[]> records) {
        try (CSVWriter writer = new CSVWriter(new FileWriter(fileName, false))) {
            writer.writeAll(records);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Определяет, является ли запись заголовком.
     */
    private boolean isHeaderRecord(String[] record) {
        return record != null
                && record.length > 0
                && record[IDX_TYPE].equalsIgnoreCase(CSV_HEADER[IDX_TYPE]);
    }
}
