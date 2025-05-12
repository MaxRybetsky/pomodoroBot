import bot.PomodoroBot;
import config.Config;
import config.ConfigReaderEnvironment;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import repository.CsvUserDataRepository;
import service.PomodoroServiceImpl;

public class Main {
    public static void main(String[] args) {
        // Загружаем конфигурацию (токен, длительности и т.д.)
        Config config = new ConfigReaderEnvironment().read();
        String botToken = config.botApiToken();

        // Инициализируем зависимости
        var telegramClient = new OkHttpTelegramClient(botToken);
        var userDataRepository = new CsvUserDataRepository();
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
