import bot.PomodoroBot;
import config.Config;
import config.ConfigReaderEnvironment;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import repository.JdbcRepository;
import service.PomodoroServiceImpl;

public class Main {
    public static void main(String[] args) {
        // Загружаем конфигурацию (токен, длительности и т.д.)
        Config config = new ConfigReaderEnvironment().read();
        String botToken = config.botApiToken();

        // Инициализируем зависимости
        var telegramClient = new OkHttpTelegramClient(botToken);

        // Необходимо добавить в решении для работы с JDBC
        // Просто меняем исходую строку и вместо CsvUserDataRepository используем JdbcRepository с config-ом
        var userDataRepository = new JdbcRepository(config);

        var pomodoroService = new PomodoroServiceImpl(userDataRepository, telegramClient, config);

        try (var botsApplication = new TelegramBotsLongPollingApplication()) {
            botsApplication.registerBot(botToken, new PomodoroBot(telegramClient, pomodoroService));
            System.out.println("Bot is running!");
            Thread.currentThread().join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
