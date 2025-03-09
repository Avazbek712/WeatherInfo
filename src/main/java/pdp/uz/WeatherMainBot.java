package pdp.uz;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;


import pdp.uz.model.ForecastDayDTO;
import pdp.uz.model.WeatherDTO;


import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;


/**
 * Created by Avazbek on 25/02/25 20:51
 */
public class WeatherMainBot extends TelegramLongPollingBot {
    private final Map<Long, Integer> userForecastOffsets = new HashMap<>();
    private final Map<Long, String> userCountry = new HashMap<>();
    private final Map<Long, Boolean> waitingForCity = new HashMap<>();
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
            } else if (data.equals("forecast_back") || data.equals("forecast_forward")) {
                handleForecastNavigation(update.getCallbackQuery());
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
        if (waitingForCity.containsKey(message.getChatId())) {
            changeCity(message);
            return;
        }


        switch (text) {
            case "/start" -> userStart(message);
            case "🌤 Текущая погода" -> nowWeather(message);

            case "ℹ️ О боте" -> botInfo(message);
            case "🌍 Геолокация" -> {
            }
            case "⚙️ Настройки" -> settings(message);
            case "\uD83C\uDF0D Изменить город" -> askForCity(message);
            case "\uD83D\uDD19 Назад в главное меню" -> mainMenu(message);
            case "\uD83D\uDCC5 Прогноз на 3 дня" -> {
                //TODO бот не отправляет сообщение когда нажимаешь эту кнопку
                userForecastOffsets.put(message.getChatId(), 0);
                threeDays(message);
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


    private void editedThreeDays(Message message) {
        try {
            Long chatId = message.getChatId();

            int daysOffSet = userForecastOffsets.get(chatId);
            LocalDate targetDate = LocalDate.now().plusDays(daysOffSet);

            String city = userCountry.getOrDefault(chatId, "Tashkent");
            WeatherDTO weather = weatherService.getWeatherInfo(city, 3);

            String country = weather.getLocation().getCountry();
            String cityName = weather.getLocation().getName();
            double maxTemp = weather.getForecast().getForecastDay().stream()
                    .filter(forecastDayDTO -> LocalDate.parse(forecastDayDTO.getDate()).equals(targetDate))
                    .map(forecastDayDTO -> forecastDayDTO.getDay().getMaxTemp()).findFirst().orElse(0.0);

            double minTemp = weather.getForecast().getForecastDay().stream()
                    .filter(forecastDayDTO -> LocalDate.parse(forecastDayDTO.getDate()).equals(targetDate))
                    .map(forecastDayDTO -> forecastDayDTO.getDay().getMinTemp()).findFirst().orElse(0.0);

            Double windSpeed = weather.getForecast().getForecastDay().stream()
                    .filter(forecastDayDTO -> LocalDate.parse(forecastDayDTO.getDate()).equals(targetDate))
                    .map(forecastDayDTO -> forecastDayDTO.getDay().getMaxWindKph()).findFirst().orElse(0.0);

            Double humidity = weather.getForecast().getForecastDay().stream()
                    .filter(forecastDayDTO -> LocalDate.parse(forecastDayDTO.getDate()).equals(targetDate))
                    .map(forecastDayDTO -> forecastDayDTO.getDay().getAvgHumidity()).findFirst().orElse(0.0);

            String sunSet = weather.getForecast().getForecastDay().stream()
                    .filter(forecastDayDTO -> LocalDate.parse(forecastDayDTO.getDate()).equals(targetDate))
                    .map(forecastDayDTO -> forecastDayDTO.getAstro().getSunset()).findFirst().orElse("Нет данных");

            String sunRise = weather.getForecast().getForecastDay().stream()
                    .filter(forecastDayDTO -> LocalDate.parse(forecastDayDTO.getDate()).equals(targetDate))
                    .map(forecastDayDTO -> forecastDayDTO.getAstro().getSunrise()).findFirst().orElse("Нет данных");

            StringBuilder textWeather = new StringBuilder();

            textWeather.append("📍 <b>").append(cityName).append(",").append(country).append("</b>\n")
                    .append("📅 <b>Прогноз погоды на ").append(targetDate).append("</b>\n\n")
                    .append("🌡 <b>Температура:</b> Макс ").append(maxTemp).append("°C | Мин ").append(minTemp).append("°C\n")
                    .append("💨 <b>Ветер:</b> ").append(windSpeed).append(" км/ч\n")
                    .append("💧 <b>Влажность:</b> ").append(humidity).append("%\n")
                    .append("🌅 <b>Восход:</b> ").append(sunRise).append("\n")
                    .append("🌇 <b>Закат:</b> ").append(sunSet).append("\n\n")
                    .append("🔄 Используйте кнопки ниже, чтобы переключаться между днями!");

            String messageText = textWeather.toString();


            InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

            List<InlineKeyboardButton> navigationRow = new ArrayList<>();

            if (daysOffSet > 0) { // Если это не первый день, добавляем кнопку "Назад"
                InlineKeyboardButton backButton = new InlineKeyboardButton("◀ Назад");
                backButton.setCallbackData("forecast_back");
                navigationRow.add(backButton);
            }

            InlineKeyboardButton dateButton = new InlineKeyboardButton("📅 " + targetDate);
            dateButton.setCallbackData("forecast_date");

            if (daysOffSet < 2) { // Если это не последний день, добавляем кнопку "Вперёд"
                InlineKeyboardButton forwardButton = new InlineKeyboardButton("▶ Вперёд");
                forwardButton.setCallbackData("forecast_forward");
                navigationRow.add(forwardButton);
            }

            if (!navigationRow.isEmpty()) {
                keyboard.add(navigationRow);
            }

            keyboard.add(List.of(dateButton));
            inlineKeyboard.setKeyboard(keyboard);


            editTextMessage(chatId.toString(), messageText, inlineKeyboard, message.getMessageId());


        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

        // todo нужно сделать красивым методы
    private void threeDays(Message message) {
        try {
            Long chatId = message.getChatId();

            int daysOffSet = userForecastOffsets.get(chatId);
            LocalDate targetDate = LocalDate.now().plusDays(daysOffSet);

            String city = userCountry.getOrDefault(chatId, "Tashkent");
            WeatherDTO weather = weatherService.getWeatherInfo(city, 3);

            String country = weather.getLocation().getCountry();
            String cityName = weather.getLocation().getName();
            double maxTemp = weather.getForecast().getForecastDay().stream()
                    .filter(forecastDayDTO -> LocalDate.parse(forecastDayDTO.getDate()).equals(targetDate))
                    .map(forecastDayDTO -> forecastDayDTO.getDay().getMaxTemp()).findFirst().orElse(0.0);

            double minTemp = weather.getForecast().getForecastDay().stream()
                    .filter(forecastDayDTO -> LocalDate.parse(forecastDayDTO.getDate()).equals(targetDate))
                    .map(forecastDayDTO -> forecastDayDTO.getDay().getMinTemp()).findFirst().orElse(0.0);

            Double windSpeed = weather.getForecast().getForecastDay().stream()
                    .filter(forecastDayDTO -> LocalDate.parse(forecastDayDTO.getDate()).equals(targetDate))
                    .map(forecastDayDTO -> forecastDayDTO.getDay().getMaxWindKph()).findFirst().orElse(0.0);

            Double humidity = weather.getForecast().getForecastDay().stream()
                    .filter(forecastDayDTO -> LocalDate.parse(forecastDayDTO.getDate()).equals(targetDate))
                    .map(forecastDayDTO -> forecastDayDTO.getDay().getAvgHumidity()).findFirst().orElse(0.0);

            String sunSet = weather.getForecast().getForecastDay().stream()
                    .filter(forecastDayDTO -> LocalDate.parse(forecastDayDTO.getDate()).equals(targetDate))
                    .map(forecastDayDTO -> forecastDayDTO.getAstro().getSunset()).findFirst().orElse("Нет данных");

            String sunRise = weather.getForecast().getForecastDay().stream()
                    .filter(forecastDayDTO -> LocalDate.parse(forecastDayDTO.getDate()).equals(targetDate))
                    .map(forecastDayDTO -> forecastDayDTO.getAstro().getSunrise()).findFirst().orElse("Нет данных");

            StringBuilder textWeather = new StringBuilder();

            textWeather.append("📍 <b>").append(cityName).append(",").append(country).append("</b>\n")
                    .append("📅 <b>Прогноз погоды на ").append(targetDate).append("</b>\n\n")
                    .append("🌡 <b>Температура:</b> Макс ").append(maxTemp).append("°C | Мин ").append(minTemp).append("°C\n")
                    .append("💨 <b>Ветер:</b> ").append(windSpeed).append(" км/ч\n")
                    .append("💧 <b>Влажность:</b> ").append(humidity).append("%\n")
                    .append("🌅 <b>Восход:</b> ").append(sunRise).append("\n")
                    .append("🌇 <b>Закат:</b> ").append(sunSet).append("\n\n")
                    .append("🔄 Используйте кнопки ниже, чтобы переключаться между днями!");

            String messageText = textWeather.toString();

            InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

            // 🔥 Добавляем кнопки
            List<InlineKeyboardButton> row = new ArrayList<>();
            if (daysOffSet > 0) {
                InlineKeyboardButton backButton = new InlineKeyboardButton("◀ Назад");
                backButton.setCallbackData("forecast_back");
                row.add(backButton);
            }

            InlineKeyboardButton dateButton = new InlineKeyboardButton("📅 " + targetDate);
            dateButton.setCallbackData("forecast_date");

            if (daysOffSet < 2) {
                InlineKeyboardButton forwardButton = new InlineKeyboardButton("▶ Вперёд");
                forwardButton.setCallbackData("forecast_forward");
                row.add(forwardButton);
            }

            keyboard.add(row);
            keyboard.add(List.of(dateButton));
            inlineKeyboard.setKeyboard(keyboard);


            sendTextMessage(chatId.toString(), messageText, inlineKeyboard);


        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private void handleForecastNavigation(CallbackQuery callbackQuery) {
        Long chatId = callbackQuery.getMessage().getChatId();
        String data = callbackQuery.getData();

        int daysOffset = userForecastOffsets.getOrDefault(chatId, 0);

        if ("forecast_back".equals(data) && daysOffset > 0) {
            userForecastOffsets.put(chatId, daysOffset - 1);
        } else if ("forecast_forward".equals(data) && daysOffset < 2) {
            userForecastOffsets.put(chatId, daysOffset + 1);
        }


        editedThreeDays(callbackQuery.getMessage());
    }

    private void mainMenu(Message message) {
        String chatId = message.getChatId().toString();

        String text = """
                \uD83C\uDFE0 <b>Вы вернулись в главное меню!</b> \s
                Выберите, что хотите сделать: \s
                
                \uD83C\uDF24 <b>Текущая погода</b> – Узнайте погоду в вашем городе. \s
                \uD83D\uDCC5 <b>Прогноз на 3 дня</b> – Посмотрите прогноз на ближайшие дни. \s
                ⏳ <b>Почасовая погода</b> – Получите детальный прогноз по часам. \s
                \uD83C\uDF0D <b>Погода по геолокации</b> – Узнайте погоду в вашем текущем местоположении. \s
                ⚙\uFE0F <b>Настройки</b> – Измените город или включите уведомления.""";
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

    private void askForCity(Message message) {
        Long chatId = message.getChatId();

        waitingForCity.put(chatId, true);

        String text = "✍️ <b>Введите название города</b> (например, Москва)";
        sendTextMessage(chatId.toString(), text);


    }

    private void changeCity(Message message) {
        try {
            Long chatId = message.getChatId();

            String city = message.getText();

            WeatherDTO weather = weatherService.getWeatherInfo(city, 2);
            if (Objects.nonNull(weather.getLocation())) {
                userCountry.put(chatId, city);

                waitingForCity.remove(chatId);
                String text = "✅ <b>Ваш город сохранён:</b> " + city;
                sendTextMessage(chatId.toString(), text);

            } else {
                sendTextMessage(chatId.toString(), "❌ <b>Город не найден, попробуйте снова.</b>");
            }
        } catch (InterruptedException e) {
            sendTextMessage(message.getChatId().toString(), "❌ <b>Ошибка запроса, попробуйте позже.</b>");
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

            String choosenCity = userCountry.getOrDefault(message.getChatId(), "Tashkent");

            WeatherDTO weatherDTO = weatherService.getWeatherInfo(choosenCity, 2);

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
            String choosenCity = userCountry.getOrDefault(message.getChatId(), "Tashkent");

            WeatherDTO weatherDTO = weatherService.getWeatherInfo(choosenCity, 2);

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

    private void settings(Message message) {
        String chatId = message.getChatId().toString();
        String text = """
                ⚙️ <b>Настройки</b> \s
                Выберите, что хотите изменить: \s
                🌍 <b>Изменить город</b> – задайте город, по которому будет показываться погода. \s
                🔙 <b>Назад</b> – вернуться в главное меню. \s
                """;

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setResizeKeyboard(true);
        KeyboardRow row1 = new KeyboardRow();
        row1.add("\uD83C\uDF0D Изменить город");

        KeyboardRow row3 = new KeyboardRow();
        row3.add("\uD83D\uDD19 Назад в главное меню");

        List<KeyboardRow> rows = new ArrayList<>();
        rows.add(row1);
        rows.add(row3);
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
