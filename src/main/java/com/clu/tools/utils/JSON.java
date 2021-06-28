package com.clu.tools.utils;

import com.google.gson.*;
import com.google.gson.internal.$Gson$Types;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;

public class JSON {


    private static final Gson GSON;

    static {
        GsonBuilder gb = new GsonBuilder();
        // 序列化
        gb.registerTypeAdapter(Date.class, new JsonSerializer<Date>() {
            public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext context) {
                if (src == null) {
                    return JsonNull.INSTANCE;
                }
                return new JsonPrimitive(src.getTime());
            }
        });

        // 反序列化
        gb.registerTypeAdapter(Date.class, new JsonDeserializer<Date>() {
            public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                if (json instanceof JsonNull) {
                    return null;
                }
                return new Date(json.getAsLong());
            }
        });

//        gb.registerTypeAdapter(Response.class, new JsonDeserializer<Response>() {
//            @Override
//            public Response deserialize(JsonElement json, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
//                if (json instanceof JsonNull) {
//                    return null;
//                }
//
//                Object code = null;
//                Object msg = null;
//                if (json instanceof JsonObject) {
//                    JsonObject jsonObject = (JsonObject) json;
//                    code = jsonObject.get("code");
//                    msg = jsonObject.get("msg");
//                } else {
//                    Map map = JSON.parse(json.toString(), HashMap.class);
//                    code = map.get("code");
//                    msg = map.get("msg");
//                }
//
//                return new Response(ConvertUtils.parseInt(code, 0), ConvertUtils.toString(msg, null));
//            }
//        });

        GSON = gb.create();
    }

    public static String stringify(Object o) {
        return GSON.toJson(o);
    }

    public static <T> T parse(String json, Class<T> type) throws JsonSyntaxException {
        return GSON.fromJson(json, type);
    }

    public static <T> T parse(String json, Type type) throws JsonSyntaxException {
        // JSON.parse(json, new TypeToken<List<GameBaseInfoVo>>(){}.getType());
        return GSON.fromJson(json, type);
    }

    public static String toJSONString(Object object) {
        return stringify(object);
    }

    public static List<Object> parseArray(String json) {
        return GSON.fromJson(json, new TypeToken<List<Object>>(){}.getType());
    }

    public static <T> List<T> parseArray(String json, Class<T> clazz) {
        return GSON.fromJson(json, $Gson$Types.newParameterizedTypeWithOwner(null, List.class, clazz));
    }

//    public static void main(String[] args) {
//        System.out.println(parseArray("[{\"name\":\"Tom\"}]", Map.class));
//    }

}
