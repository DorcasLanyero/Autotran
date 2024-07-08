package com.cassens.autotran.data.remote;

import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.data.model.lookup.ShuttleMove;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;


public class GsonTypeAdapters {
    public static class ShuttleMoveSerializer implements JsonSerializer<ShuttleMove> {
        @Override
        public JsonElement serialize(ShuttleMove src, Type typeOfSrc, JsonSerializationContext context) {
            if (src != null) {
                return new JsonPrimitive(src.shuttleMoveId);
            } else {
                return null;
            }
        }
    }

    public static class DateSerializer implements JsonSerializer<Date>, JsonDeserializer<Date> {

        private static final DateFormat ISO_DATE = Constants.dateFormatter();

        @Override
        public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext context) {
            if(src != null) {
                return new JsonPrimitive(ISO_DATE.format(src));
            }
            else {
                return null;
            }
        }

        @Override
        public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if(json == null) return null;

            String dateString = json.getAsString();
            try {
                return ISO_DATE.parse(dateString);
            } catch (ParseException e) {
                throw new JsonParseException("Unexpected date format " + json.toString());
            }
        }
    }
}
