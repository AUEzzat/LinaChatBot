package com.sourcey.linachatbot;

/**
 * Created by amrezzat on 6/28/2017.
 */

public class Note {
    private String title;

    public Note(String title, String text) {
        this.title = title;
        this.text = text;
    }

    private String text;

    public Note() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
