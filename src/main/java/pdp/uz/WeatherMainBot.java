package pdp.uz;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;

import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;


import pdp.uz.model.WeatherDTO;


import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;


/**
 * Created by Avazbek on 25/02/25 20:51
 */
public class WeatherMainBot extends TelegramLongPollingBot {
    private final Map<Long, String> userCountry = new HashMap<>();
    WeatherService weatherService = new WeatherService();

    public WeatherMainBot(String token) {
        super(token);
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            Message message = update.getMessage();


            if (message.hasLocation()) {
                Double latitude = message.getLocation().getLatitude();
                Double longitude = message.getLocation().getLongitude();

                weatherByLocation(message.getChatId().toString(), latitude, longitude);
            } else {

                processMessage(message);
            }
        } else if (update.hasCallbackQuery()) {
            String data = update.getCallbackQuery().getData();
            Message message = update.getCallbackQuery().getMessage();
            if ("update_weather".equals(data)) {
                editedWeather(message);
            }
        }
    }

    private void weatherByLocation(String chatId, Double latitude, Double longitude) {
        try {
            String latitudeString = latitude.toString();
            String longitudeString = longitude.toString();

            WeatherDTO weatherDTO = weatherService.getWeatherInfoByLocation(longitudeString, latitudeString, 2);

            String country = weatherDTO.getLocation().getCountry();

            String city = weatherDTO.getLocation().getName();

            double temp = weatherDTO.getCurrent().getTempC();

            LocalDateTime localtime = weatherDTO.getLocation().getLocaltime();
            DateTimeFormatter dayMonthFormatter = DateTimeFormatter.ofPattern("dd/MM");

            String date = dayMonthFormatter.format(localtime);

            double maxTemp = weatherDTO.getForecast().getForecastDay().stream()
                    .filter(forecastDayDTO -> LocalDate.parse(forecastDayDTO.getDate()).equals(LocalDate.now()))
                    .map(forecastDayDTO -> forecastDayDTO.getDay().getMaxTemp()).findFirst().orElse(0.0);

            double minTemp = weatherDTO.getForecast().getForecastDay().stream()
                    .filter(forecastDayDTO -> LocalDate.parse(forecastDayDTO.getDate()).equals(LocalDate.now()))
                    .map(forecastDayDTO -> forecastDayDTO.getDay().getMinTemp()).findFirst().orElse(0.0);

            String condition = weatherDTO.getCurrent().getCondition().getText();

            double windKph = weatherDTO.getCurrent().getWindKph();

            double humidity = weatherDTO.getCurrent().getHumidity();

            String sunRise = weatherDTO.getForecast().getForecastDay().stream()
                    .filter(forecastDayDTO -> LocalDate.parse(forecastDayDTO.getDate()).equals(LocalDate.now()))
                    .map(forecastDayDTO -> forecastDayDTO.getAstro().getSunrise()).findFirst().orElse("Нет данных");

            String sunSet = weatherDTO.getForecast().getForecastDay().stream()
                    .filter(forecastDayDTO -> LocalDate.parse(forecastDayDTO.getDate()).equals(LocalDate.now()))
                    .map(forecastDayDTO -> forecastDayDTO.getAstro().getSunset()).findFirst().orElse("Нет данных");
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

            String text = """
                    <b>📆 Дата:</b> %s
                    <b>⏰ Время:</b> %s
                    <b>🌍 Страна:</b> %s \s
                    <b>🌍 Город:</b> %s \s
                    <b>🌡 Температура:</b> %d°C \s
                    <b>📈 Макс:</b> %d°C  |  <b>📉 Мин:</b> %d°C \s
                    <b>☁️ Описание:</b> %s \s
                    <b>💨 Ветер:</b> %d км/ч \s
                    <b>💧 Влажность:</b> %d \s
                    <b>🌅 Восход:</b> %s \s
                    <b>🌇 Закат:</b> %s \s
                    """.formatted(date, time, country, city, (int) temp, (int) maxTemp, (int) minTemp, condition, (int) windKph, (int) humidity, sunRise, sunSet);

            InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
            InlineKeyboardButton updateButton = new InlineKeyboardButton();
            updateButton.setText("🔄 Обновить");
            updateButton.setCallbackData("update_weather");

            keyboard.add(List.of(updateButton));
            inlineKeyboard.setKeyboard(keyboard);
            sendTextMessage(chatId, text, inlineKeyboard);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    private void processMessage(Message message) {
        String text = message.getText();

        switch (text) {
            case "/start" -> userStart(message);
            case "🌤 Текущая погода" -> nowWeather(message);

            case "ℹ️ О боте" -> botInfo(message);
            case "🌍 Геолокация" -> {
            }
            default -> {
                String chatId = message.getChatId().toString();
                String textDef = """
                        ❌ <b>Неизвестная команда!</b>
                        Попробуйте использовать кнопки или команды:
                        - <b>/start</b> – Начать
                        - <b>🌤 Текущая погода</b> – Узнать погоду
                        - <b>ℹ️ О боте</b> – Информация
                        """;
                sendTextMessage(chatId, textDef);
            }
        }
    }

    private void botInfo(Message message) {
        String chatId = message.getChatId().toString();
        String text = """
                <b>🤖 О боте</b>
                
                Привет! Я твой погодный помощник. 🌤 \s
                Я использую <b>WeatherAPI</b> для получения точных данных о погоде. \s
                
                <b>Что я умею?</b> \s
                - 🌡 Показывать <b>текущую температуру</b> \s
                - 📅 Давать <b>прогноз на 3 дня</b> \s
                - ⏳ Отображать <b>почасовую погоду</b> \s
                - 🌍 Определять погоду <b>по твоей геолокации</b> \s
                
                <b>Как меня использовать?</b> \s
                Просто нажми на нужную кнопку в меню, и я отправлю тебе свежие данные! 📲 \s
                
                <b>Разработчик:</b> @Umarovich_712
                """;
        sendTextMessage(chatId, text);
    }

    private void editedWeather(Message message) {
        try {
            Integer messageId = message.getMessageId();

            WeatherDTO weatherDTO = weatherService.getWeatherInfo("Tashkent", 2);

            String chatId = message.getChatId().toString();

            String country = weatherDTO.getLocation().getCountry();

            String city = weatherDTO.getLocation().getName();

            double temp = weatherDTO.getCurrent().getTempC();

            LocalDateTime localtime = weatherDTO.getLocation().getLocaltime();
            DateTimeFormatter dayMonthFormatter = DateTimeFormatter.ofPattern("dd/MM");

            String date = dayMonthFormatter.format(localtime);

            double maxTemp = weatherDTO.getForecast().getForecastDay().stream()
                    .filter(forecastDayDTO -> LocalDate.parse(forecastDayDTO.getDate()).equals(LocalDate.now()))
                    .map(forecastDayDTO -> forecastDayDTO.getDay().getMaxTemp()).findFirst().orElse(0.0);

            double minTemp = weatherDTO.getForecast().getForecastDay().stream()
                    .filter(forecastDayDTO -> LocalDate.parse(forecastDayDTO.getDate()).equals(LocalDate.now()))
                    .map(forecastDayDTO -> forecastDayDTO.getDay().getMinTemp()).findFirst().orElse(0.0);

            String condition = weatherDTO.getCurrent().getCondition().getText();

            double windKph = weatherDTO.getCurrent().getWindKph();

            double humidity = weatherDTO.getCurrent().getHumidity();

            String sunRise = weatherDTO.getForecast().getForecastDay().stream()
                    .filter(forecastDayDTO -> LocalDate.parse(forecastDayDTO.getDate()).equals(LocalDate.now()))
                    .map(forecastDayDTO -> forecastDayDTO.getAstro().getSunrise()).findFirst().orElse("Нет данных");

            String sunSet = weatherDTO.getForecast().getForecastDay().stream()
                    .filter(forecastDayDTO -> LocalDate.parse(forecastDayDTO.getDate()).equals(LocalDate.now()))
                    .map(forecastDayDTO -> forecastDayDTO.getAstro().getSunset()).findFirst().orElse("Нет данных");

            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

            String text = """
                    <b>📆 Дата:</b> %s
                    <b>⏰ Последнее обновление:</b> %s
                    <b>🌍 Страна:</b> %s \s
                    <b>🌍 Город:</b> %s \s
                    <b>🌡 Температура:</b> %d°C \s
                    <b>📈 Макс:</b> %d°C  |  <b>📉 Мин:</b> %d°C \s
                    <b>☁️ Описание:</b> %s \s
                    <b>💨 Ветер:</b> %d км/ч \s
                    <b>💧 Влажность:</b> %d \s
                    <b>🌅 Восход:</b> %s \s
                    <b>🌇 Закат:</b> %s \s
                    """.formatted(date, time, country, city, (int) temp, (int) maxTemp, (int) minTemp, condition, (int) windKph, (int) humidity, sunRise, sunSet);
            InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

            InlineKeyboardButton updateButton = new InlineKeyboardButton();
            updateButton.setText("🔄 Обновить");
            updateButton.setCallbackData("update_weather");

            keyboard.add(List.of(updateButton));
            inlineKeyboard.setKeyboard(keyboard);


            editTextMessage(chatId, text, inlineKeyboard, messageId);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }


    private void nowWeather(Message message) {
        try {
            WeatherDTO weatherDTO = weatherService.getWeatherInfo("Tashkent", 2);

            String chatId = message.getChatId().toString();

            String country = weatherDTO.getLocation().getCountry();
            String city = weatherDTO.getLocation().getName();

            double temp = weatherDTO.getCurrent().getTempC();

            LocalDateTime localtime = weatherDTO.getLocation().getLocaltime();
            DateTimeFormatter dayMonthFormatter = DateTimeFormatter.ofPattern("dd/MM");

            String date = dayMonthFormatter.format(localtime);

            double maxTemp = weatherDTO.getForecast().getForecastDay().stream()
                    .filter(forecastDayDTO -> LocalDate.parse(forecastDayDTO.getDate()).equals(LocalDate.now()))
                    .map(forecastDayDTO -> forecastDayDTO.getDay().getMaxTemp()).findFirst().orElse(0.0);

            double minTemp = weatherDTO.getForecast().getForecastDay().stream()
                    .filter(forecastDayDTO -> LocalDate.parse(forecastDayDTO.getDate()).equals(LocalDate.now()))
                    .map(forecastDayDTO -> forecastDayDTO.getDay().getMinTemp()).findFirst().orElse(0.0);

            String condition = weatherDTO.getCurrent().getCondition().getText();

            double windKph = weatherDTO.getCurrent().getWindKph();

            double humidity = weatherDTO.getCurrent().getHumidity();

            String sunRise = weatherDTO.getForecast().getForecastDay().stream()
                    .filter(forecastDayDTO -> LocalDate.parse(forecastDayDTO.getDate()).equals(LocalDate.now()))
                    .map(forecastDayDTO -> forecastDayDTO.getAstro().getSunrise()).findFirst().orElse("Нет данных");

            String sunSet = weatherDTO.getForecast().getForecastDay().stream()
                    .filter(forecastDayDTO -> LocalDate.parse(forecastDayDTO.getDate()).equals(LocalDate.now()))
                    .map(forecastDayDTO -> forecastDayDTO.getAstro().getSunset()).findFirst().orElse("Нет данных");

            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

            String text = """
                    <b>📆 Дата:</b> %s
                    <b>⏰ Время:</b> %s
                    <b>🌍 Страна:</b> %s \s
                    <b>🌍 Город:</b> %s \s
                    <b>🌡 Температура:</b> %d°C \s
                    <b>📈 Макс:</b> %d °C  |  <b>📉 Мин:</b> %d °C \s
                    <b>☁️ Описание:</b> %s \s
                    <b>💨 Ветер:</b> %d км/ч \s
                    <b>💧 Влажность:</b> %d Процентов. \s
                    <b>🌅 Восход:</b> %s \s
                    <b>🌇 Закат:</b> %s \s
                    """.formatted(date, time, country, city, (int) temp, (int) maxTemp, (int) minTemp, condition, (int) windKph, (int) humidity, sunRise, sunSet);
            InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();


            InlineKeyboardButton updateButton = new InlineKeyboardButton();
            updateButton.setText("🔄 Обновить");
            updateButton.setCallbackData("update_weather");

            keyboard.add(List.of(updateButton));
            inlineKeyboard.setKeyboard(keyboard);


            sendTextMessage(chatId, text, inlineKeyboard);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void userStart(Message message) {
        String chatId = message.getChatId().toString();
        String text = """
                Привет! ☀️ Я бот, который поможет тебе узнать погоду! \s
                
                Я могу показать: \s
                - <b>🌡 Текущую температуру</b> \s
                - <b>📅 Прогноз на 3 дня</b> \s
                - <b>⏳ Почасовую погоду</b> (выбери <b>3, 6 или 12</b> часов) \s
                - <b>🌍 Погоду по твоей геолокации</b> \s
                
                Просто нажми на кнопку ниже и получи нужную информацию! 📲 \s
                """;
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setResizeKeyboard(true);

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("\uD83C\uDF24 Текущая погода"));
        row1.add(new KeyboardButton("\uD83D\uDCC5 Прогноз на 3 дня "));
        row1.add(new KeyboardButton("⏰ Прогноз по часам "));

        KeyboardRow row2 = new KeyboardRow();
        KeyboardButton locationButton = new KeyboardButton("\uD83C\uDF0D Геолокация");
        locationButton.setRequestLocation(true);
        row2.add(locationButton);

        row2.add(new KeyboardButton("⚙️ Настройки"));
        row2.add(new KeyboardButton("ℹ️ О боте"));
        List<KeyboardRow> rows = new ArrayList<>();
        rows.add(row1);
        rows.add(row2);
        markup.setKeyboard(rows);
        sendTextMessage(chatId, text, markup);
    }

    @Override
    public String getBotUsername() {
        return "@myweatherchecker_bot";
    }

    private void sendTextMessage(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setParseMode("HTML");

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendTextMessage(String chatId, String text, ReplyKeyboardMarkup keyboardMarkup) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setParseMode("HTML");
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendTextMessage(String chatId, String text, InlineKeyboardMarkup keyboardMarkup) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setParseMode("HTML");
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void editTextMessage(String chatId, String text, InlineKeyboardMarkup keyboardMarkup, Integer messageId) {
        EditMessageText message = new EditMessageText();
        message.setMessageId(messageId);
        message.setChatId(chatId);
        message.setText(text);
        message.setParseMode("HTML");
        message.setReplyMarkup(keyboardMarkup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

}
