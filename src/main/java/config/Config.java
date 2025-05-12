package config;

public record Config(String botApiToken, int workDuration, int restDuration) {
    public Config {
        if (botApiToken == null || botApiToken.isEmpty()) {
            throw new RuntimeException("Токен не задан!");
        }
    }
}
