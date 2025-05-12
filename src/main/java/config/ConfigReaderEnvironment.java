package config;

public class ConfigReaderEnvironment {
    public Config read() {
        String token = System.getenv("BOT_API_TOKEN");
        // Читаем длительность рабочего и отдыха из переменных окружения (если не заданы, используются значения по умолчанию)
        String workDurationStr = System.getenv("WORK_DURATION");
        String restDurationStr = System.getenv("REST_DURATION");
        int workDuration = (workDurationStr != null) ? Integer.parseInt(workDurationStr) : 25;
        int restDuration = (restDurationStr != null) ? Integer.parseInt(restDurationStr) : 5;

        return new Config(token, workDuration, restDuration);
    }
}
