package pdp.uz;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class Main {
    public static void main(String[] args) {
        String token = "7758175133:AAF02a_VcQZf5GltGY9FvEw9mWNQ4N07JZ8";
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            WeatherMainBot bot = new WeatherMainBot(token);
            botsApi.registerBot(bot);
            System.out.println("🤖 Бот успешно запущен!");
        } catch (TelegramApiException e) {
            System.err.println("❌ Ошибка запуска бота: " + e.getMessage());
            e.printStackTrace();
        }
    }
}