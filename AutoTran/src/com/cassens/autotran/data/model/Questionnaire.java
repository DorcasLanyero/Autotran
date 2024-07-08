package com.cassens.autotran.data.model;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.List;

public class Questionnaire {
    public int id;
    public enum Type {
        PreloadAudit,
        Unknown;

        public static Type stringToType(String typeString) {
            if (PreloadAudit.name().equals(typeString)) {
                return PreloadAudit;
            }
            return Unknown;
        }
    };
    public Type type;
    public int version;
    public String prompts;

    public Questionnaire() {
    }

    public Questionnaire(int id, Type type, int version, String prompts) {
        this.id = id;
        this.type = type;
        this.version = version;
        this.prompts = prompts;
    }

    public List<Prompt> parsePrompt() {
        Gson gson = new Gson();
        List<Prompt> prompts = gson.fromJson(this.prompts, new TypeToken<List<Prompt>>(){}.getType() );
        return prompts;
    }
}
