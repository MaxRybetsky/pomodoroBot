package config;

public record Config(
    String botApiToken,
    int workDuration,
    int restDuration,
    // Необходимо добавить в решении для работы с JDBC
    String dbUrl,
    String dbUser,
    String dbPassword
) {
    public Config {
        if (botApiToken == null || botApiToken.isEmpty()) {
            throw new RuntimeException("Токен не задан!");
        }

        // Необходимо добавить в решении для работы с JDBC
        if (dbUrl == null || dbUrl.isEmpty()) {
            throw new RuntimeException("URL базы данных не задан!");
        }
        if (dbUser == null || dbUser.isEmpty()) {
            throw new RuntimeException("Пользователь базы данных не задан!");
        }
        if (dbPassword == null || dbPassword.isEmpty()) {
            throw new RuntimeException("Пароль базы данных не задан!");
        }
    }
}
