package com.javarush.telegram;

import com.javarush.telegram.ChatGPTService;
import com.javarush.telegram.DialogMode;
import com.javarush.telegram.MultiSessionTelegramBot;
import com.javarush.telegram.UserInfo;
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
    static{
        TELEGRAM_BOT_TOKEN = getToken();
    }
    public static final String OPEN_AI_TOKEN = "chat-gpt-token"; //TODO: добавь токен ChatGPT в кавычках

    public static String getToken(){
        Properties prop = new Properties();
        try {
            //load a properties file from class path, inside static method
            prop.load(TinderBoltApp.class.getClassLoader().getResourceAsStream("config.properties"));
            //get the property value and print it out
            return prop.getProperty("token");
        }
        catch (IOException ex) {
            ex.printStackTrace();
            throw new RuntimeException("Не удалось загрузить token из config.properties", ex);
        }
    }

    public TinderBoltApp() {
        super(TELEGRAM_BOT_NAME, TELEGRAM_BOT_TOKEN);
    }

    @Override
    public void onUpdateEventReceived(Update update) {
        //TODO: основной функционал бота будем писать здесь

        String message = getMessageText(); //переслать сообщение пользователя

        if (message.equals("/start")){
            sendPhotoMessage("main");
            String text = loadMessage("main");//загружает текст из messages/main.txt
            sendTextMessage(text);
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
