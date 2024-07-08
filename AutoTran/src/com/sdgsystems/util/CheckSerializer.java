package com.sdgsystems.util;

import android.widget.EditText;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class CheckSerializer implements JsonSerializer<Check> {
    @Override
    public JsonElement serialize(Check check, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject result = new JsonObject();
        result.add("i", new JsonPrimitive(check.id));
        result.add("m", new JsonPrimitive(check.getMarked()));

//        String note = check.note;
//        if (note != null) {
//            result.add("n", new JsonPrimitive(note));
//        } else {
//            result.add("n", new JsonPrimitive(""));
//        }


        return result;
    }
}
