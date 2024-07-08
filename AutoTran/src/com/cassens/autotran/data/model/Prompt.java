package com.cassens.autotran.data.model;

public class Prompt {
    String text;
    String type;
    String format;

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    @Override
    public String toString() {
        return "Prompt [text = " + text + ", type = " + type + ", format = " + format + "]";
    }
}

