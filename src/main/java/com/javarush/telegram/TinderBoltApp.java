package com.javarush.telegram;

import com.javarush.telegram.ChatGPTService;
import com.javarush.telegram.DialogMode;
import com.javarush.telegram.MultiSessionTelegramBot;
import com.javarush.telegram.UserInfo;
import org.glassfish.jersey.internal.util.collection.StringIgnoreCaseKeyComparator;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

public class TinderBoltApp extends MultiSessionTelegramBot {
    public static final String TELEGRAM_BOT_NAME = "javarush_dating_ai_bot"; //TODO: добавь имя бота в кавычках
    public static final String TELEGRAM_BOT_TOKEN; //TODO: добавь токен бота в кавычках
    public static final String OPEN_AI_TOKEN; //TODO: добавь токен ChatGPT в кавычках
    static{
        Properties prop = new Properties();
        try {
            //load a properties file from class path, inside static method
            prop.load(TinderBoltApp.class.getClassLoader().getResourceAsStream("config.properties"));
            TELEGRAM_BOT_TOKEN = prop.getProperty("telegram_bot_token");
            OPEN_AI_TOKEN = prop.getProperty("open_ai_token");
        }
        catch (IOException ex) {
            ex.printStackTrace();
            throw new RuntimeException("Не удалось загрузить токен из config.properties", ex);
        }
    }

    private ChatGPTService chatGPT = new ChatGPTService(OPEN_AI_TOKEN);
    private DialogMode currentMode = null;
    private ArrayList<String> list = new ArrayList<>();
    private UserInfo me;
    private UserInfo she;
    private int questionCount;

    public TinderBoltApp() {
        super(TELEGRAM_BOT_NAME, TELEGRAM_BOT_TOKEN);
    }

    @Override
    public void onUpdateEventReceived(Update update) {
        //TODO: основной функционал бота будем писать здесь

        String message = getMessageText(); //переслать сообщение пользователя

        if (message.equals("/start")){
            currentMode = DialogMode.MAIN;
            sendPhotoMessage("main");
            String text = loadMessage("main");//загружает текст из messages/main.txt
            sendTextMessage(text);

            showMainMenu("главное меню бота", "/start",
                    "генерация Tinder-профля \uD83D\uDE0E", "/profile",
                    "сообщение для знакомства \uD83E\uDD70", "/opener",
                    "переписка от вашего имени \uD83D\uDE08", "/message",
                    "переписка со звездами \uD83D\uDD25", "/date",
                    "задать вопрос чату GPT \uD83E\uDDE0", "/gpt");
            return;
        }

        if (message.equals("/gpt")){
            currentMode = DialogMode.GPT;
            sendPhotoMessage("gpt");
            String text = loadMessage("gpt");
            sendTextMessage(text);
            return;
        }

        if (message.equals("/date")){
            currentMode = DialogMode.DATE;
            sendPhotoMessage("date");
            String text = loadMessage("date");
            sendTextButtonsMessage(text,
                    "Ариана Гранде", "date_grande",
                    "Марго Робби", "date_robbie",
                    "Зендея", "date_zendaya",
                    "Райан Гослинг", "date_gosling",
                    "Том Харди", "date_hardy");
            return;
        }

        if (message.equals("/message")){
            currentMode = DialogMode.MESSAGE;
            sendPhotoMessage("message");
            sendTextButtonsMessage("Пришлите в чат вашу переписку",
                    "Next message", "message_next",
                    "Invite for a date", "message_date");
            return;
        }

        if (message.equals("/profile")){
            currentMode = DialogMode.PROFILE;
            sendPhotoMessage("profile");

            me = new UserInfo();
            questionCount = 1;
            sendTextMessage("Сколько вам лет?");
            return;
        }

        if (message.equals("/opener")){
            currentMode = DialogMode.OPENER;
            sendPhotoMessage("opener");
            she = new UserInfo();
            questionCount = 1;
            sendTextMessage("Имя девушки?");
            return;
        }

        if (currentMode == DialogMode.GPT && !isMessageCommand()){
            String prompt = loadPrompt("gpt");
            Message msg = sendTextMessage("Подождите пару секунд - ChatGPT думает...");
            String answer = chatGPT.sendMessage(prompt, message);
            updateTextMessage(msg, answer);
            return;
        }

        if (currentMode == DialogMode.DATE && !isMessageCommand()){
            String query = getCallbackQueryButtonKey();
            if (query.startsWith("date_")){
                sendPhotoMessage(query);
                sendTextMessage("Отличный выбор! \nТвоя задача - пригласить девушку/парня на свидание ❤ за 5 сообщений");

                String prompt = loadPrompt(query);
                chatGPT.setPrompt(prompt);
                return;
            }

            Message msg = sendTextMessage("Подождите, собеседник печатает...");
            String answer = chatGPT.addMessage(message); //добавление нового сообщения к текущему диалогу с GPT
            updateTextMessage(msg, answer);
            return;
        }

        if (currentMode == DialogMode.MESSAGE && !isMessageCommand()){
            String query = getCallbackQueryButtonKey();
            if (query.startsWith("message_")){
                String prompt = loadPrompt(query);
                String userChatHistory = String.join("\n\n", list);

                Message msg = sendTextMessage("Подождите пару секунд - ChatGPT думает...");
                String answer = chatGPT.sendMessage(prompt, userChatHistory); //10 sec
                updateTextMessage(msg, answer);
                return;
            }

            list.add(message);
            return;
        }

        if (currentMode == DialogMode.PROFILE && !isMessageCommand()){
            switch (questionCount){
                case 1:
                    me.age = message;
                    questionCount = 2;
                    sendTextMessage("Кем вы работаете?");
                    return;
                case 2:
                    me.occupation = message;
                    questionCount = 3;
                    sendTextMessage("У вас есть хобби?");
                    return;
                case 3:
                    me.hobby = message;
                    questionCount = 4;
                    sendTextMessage("Что вам не нравится в людях?");
                    return;
                case 4:
                    me.annoys = message;
                    questionCount = 5;
                    sendTextMessage("Цель знакомства?");
                    return;
                case 5:
                    me.goals = message;
                    String aboutMyself = me.toString();
                    String prompt = loadPrompt("profile");

                    Message msg = sendTextMessage("Подождите пару секунд - ChatGPT думает...");
                    String answer = chatGPT.sendMessage(prompt, aboutMyself); //10 sec
                    updateTextMessage(msg, answer);
                    return;
            }
            return;
        }

        if (currentMode == DialogMode.OPENER && !isMessageCommand()){
            switch (questionCount){
                case 1:
                    she.name = message;
                    questionCount = 2;
                    sendTextMessage("Сколько ей лет?");
                    return;
                case 2:
                    she.age = message;
                    questionCount = 3;
                    sendTextMessage("Есть ли у неё хобби и какие?");
                    return;
                case 3:
                    she.hobby = message;
                    questionCount = 4;
                    sendTextMessage("Кем она работает?");
                    return;
                case 4:
                    she.occupation = message;
                    questionCount = 5;
                    sendTextMessage("Цель знакомства?");
                    return;
                case 5:
                    she.goals = message;
                    String aboutFriend = she.toString();
                    String prompt = loadPrompt("opener");

                    Message msg = sendTextMessage("Подождите пару секунд - ChatGPT думает...");
                    String answer = chatGPT.sendMessage(prompt, aboutFriend); //10 sec
                    updateTextMessage(msg, answer);
                    return;
            }
            return;
        }





        sendTextMessage("_Hello_"); //курсив

        sendTextMessage("*You texted* " + message); //жирный текст
        sendTextButtonsMessage("Choose the mode",
                "Start", "start",
                "Stop", "stop");//текст, название кнопки, ее уникальное имя




    }

    public static void main(String[] args) throws TelegramApiException {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        telegramBotsApi.registerBot(new TinderBoltApp());
    }
}
