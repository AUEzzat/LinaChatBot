package com.sourcey.linachatbot;

/**
 * Created by amrezzat on 3/4/2017.
 */

public class ChatMessage {
    private String messageText;
    private boolean humanUser;// 1-bot, 2-human user
//    private Integer messageColor = Color.parseColor("#CC2233");

    public ChatMessage(String messageText) {
        this.messageText = messageText;
        this.humanUser = false;
    }

    public boolean isHumanUser() {
        return humanUser;
    }

    public ChatMessage(String messageText, boolean humanUser) {
        this.messageText = messageText;
        this.humanUser = humanUser;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;

    }

    public String getMessageText() {
        return messageText;
    }
}
