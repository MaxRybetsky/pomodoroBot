package repository;

import config.Config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// Необходимо добавить в решении для работы с JDBC
public class JdbcRepository implements UserDataRepository {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;

    public JdbcRepository(Config config) {
        this.dbUrl = config.dbUrl();
        this.dbUser = config.dbUser();
        this.dbPassword = config.dbPassword();
    }

    // SQL Запросы
    private static final String INSERT_SESSION = """
            INSERT INTO user_session (chat_id, type, duration, start_at)
            VALUES (?, ?, ?, ?)
            """;
            
    private static final String COMPLETE_SESSION = """
            UPDATE user_session
            SET stop_at = ?, completed = true
            WHERE chat_id = ? AND type = ? AND stop_at IS NULL
            """;
            
    private static final String STOP_SESSION = """
            UPDATE user_session
            SET stop_at = ?
            WHERE chat_id = ? AND stop_at IS NULL
            """;
            
    private static final String GET_STATISTICS = """
            SELECT type, duration, completed
            FROM user_session
            WHERE chat_id = ? AND stop_at IS NOT NULL
            """;
            
    private static final String GET_ACHIEVEMENTS = """
            SELECT achievement_name, achievement_description, achieved_at
            FROM user_achievements
            WHERE chat_id = ?
            ORDER BY achieved_at DESC
            """;
            
    private static final String EXPORT_STATISTICS = """
            SELECT type, duration, start_at, stop_at, completed
            FROM user_session
            WHERE chat_id = ?
            ORDER BY start_at DESC
            """;

    @Override
    public void recordSession(long chatId, String sessionType, int durationMinutes, LocalDateTime startAt) {
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(INSERT_SESSION, Statement.RETURN_GENERATED_KEYS)) {
            
            preparedStatement.setLong(1, chatId);
            preparedStatement.setString(2, sessionType);
            preparedStatement.setLong(3, durationMinutes);
            preparedStatement.setObject(4, startAt);

            preparedStatement.executeUpdate();
            
            System.out.println("Session recorded for chatId=" + chatId + ", type=" + sessionType);
        } catch (SQLException e) {
            System.out.println("Error recording session for chatId=" + chatId + ": " + e.getMessage());
            throw new RuntimeException("Failed to record session", e);
        }
    }

    @Override
    public void completeSession(long chatId, String sessionType, LocalDateTime stopAt) {
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(COMPLETE_SESSION)) {
            
            preparedStatement.setObject(1, stopAt);
            preparedStatement.setLong(2, chatId);
            preparedStatement.setString(3, sessionType);

            int rowsUpdated = preparedStatement.executeUpdate();
            
            if (rowsUpdated == 0) {
                System.out.println("No active session found for chatId=" + chatId + ", type=" + sessionType);
            } else {
                System.out.println("Session completed for chatId=" + chatId + ", type=" + sessionType);
            }
        } catch (SQLException e) {
            System.out.println("Error completing session for chatId=" + chatId + ": " + e.getMessage());
            throw new RuntimeException("Failed to complete session", e);
        }
    }

    @Override
    public void markSessionStopped(long chatId, LocalDateTime stopAt) {
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(STOP_SESSION)) {
            
            preparedStatement.setObject(1, stopAt);
            preparedStatement.setLong(2, chatId);

            int rowsUpdated = preparedStatement.executeUpdate();
            
            if (rowsUpdated == 0) {
                System.out.println("No active session found for chatId=" + chatId);
            } else {
                System.out.println("Session stopped for chatId=" + chatId);
            }
        } catch (SQLException e) {
            System.out.println("Error stopping session for chatId=" + chatId + ": " + e.getMessage());
            throw new RuntimeException("Failed to stop session", e);
        }
    }

    @Override
    public String getStatistics(long chatId) {
        int workMinutes = 0, restMinutes = 0, workCycles = 0, restCycles = 0;

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(GET_STATISTICS)) {
            
            preparedStatement.setLong(1, chatId);
            
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    String type = resultSet.getString("type");
                    int duration = resultSet.getInt("duration");
                    boolean completed = resultSet.getBoolean("completed");

                    if (completed) {
                        if ("WORK".equalsIgnoreCase(type)) {
                            workMinutes += duration;
                            workCycles++;
                        } else if ("REST".equalsIgnoreCase(type)) {
                            restMinutes += duration;
                            restCycles++;
                        }
                    }
                }
            }
            
            System.out.println("Statistics retrieved for chatId=" + chatId);
        } catch (SQLException e) {
            System.out.println("Error getting statistics for chatId=" + chatId + ": " + e.getMessage());
            throw new RuntimeException("Failed to get statistics", e);
        }

        return String.format("Статистика:%n" +
                "• Рабочее время: %d мин%n" +
                "• Отдых: %d мин%n" +
                "• Рабочих циклов: %d%n" +
                "• Циклов отдыха: %d", 
                workMinutes, restMinutes, workCycles, restCycles);
    }

    @Override
    public String getAchievements(long chatId) {
        StringBuilder achievements = new StringBuilder();
        
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(GET_ACHIEVEMENTS)) {
            
            preparedStatement.setLong(1, chatId);
            
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (!resultSet.next()) {
                    return "Достижений пока нет.";
                }
                
                do {
                    String name = resultSet.getString("achievement_name");
                    String description = resultSet.getString("achievement_description");
                    LocalDateTime achievedAt = resultSet.getTimestamp("achieved_at").toLocalDateTime();
                    
                    achievements.append("🏆 ").append(name).append("\n")
                              .append("   ").append(description).append("\n")
                              .append("   Получено: ").append(achievedAt.format(FORMATTER)).append("\n\n");
                } while (resultSet.next());
            }
            
            System.out.println("Achievements retrieved for chatId=" + chatId);
        } catch (SQLException e) {
            System.out.println("Error getting achievements for chatId=" + chatId + ": " + e.getMessage());
            throw new RuntimeException("Failed to get achievements", e);
        }
        
        return achievements.toString().trim();
    }

    @Override
    public byte[] exportStatistics(long chatId) {
        StringBuilder csvContent = new StringBuilder();
        csvContent.append("type,duration,start_at,stop_at,completed\n");
        
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(EXPORT_STATISTICS)) {
            
            preparedStatement.setLong(1, chatId);
            
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    String type = resultSet.getString("type");
                    int duration = resultSet.getInt("duration");
                    LocalDateTime startAt = resultSet.getTimestamp("start_at").toLocalDateTime();
                    LocalDateTime stopAt = resultSet.getTimestamp("stop_at") != null ? 
                                         resultSet.getTimestamp("stop_at").toLocalDateTime() : null;
                    boolean completed = resultSet.getBoolean("completed");
                    
                    csvContent.append(type).append(",")
                             .append(duration).append(",")
                             .append(startAt.format(FORMATTER)).append(",")
                             .append(stopAt != null ? stopAt.format(FORMATTER) : "").append(",")
                             .append(completed).append("\n");
                }
            }
            
            System.out.println("Statistics exported for chatId=" + chatId);
        } catch (SQLException e) {
            System.out.println("Error exporting statistics for chatId=" + chatId + ": " + e.getMessage());
            throw new RuntimeException("Failed to export statistics", e);
        }
        
        return csvContent.toString().getBytes();
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }
}
