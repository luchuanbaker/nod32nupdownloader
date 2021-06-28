package com.clu.tools.utils;

import java.util.*;
import java.util.function.Function;

/**
 * 统一的JSONObject
 */
public class JSONObject extends LinkedHashMap<String, Object> {

    public JSONObject() {
    }

    public static JSONObject error(int errorCode, String errorMessage) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("errorCode", errorCode);
        jsonObject.put("errorMessage", errorMessage);
        return jsonObject;
    }

    public static JSONObject error(Integer action, int errorCode, String errorMessage) {
        return error(errorCode, errorMessage).extend(
            "action", action
        );
    }


    public static JSONObject of(String key, Object value) {
        return JSONObject.of(new Object[]{key, value});
    }

    @SuppressWarnings("unchecked")
    public static JSONObject of(Object... params) {
        // 兼容单个Map类型的参数
        if (params.length == 1 && params[0] instanceof Map) {
            return JSONObject.of((Map) params[0]);
        }
        Assert.isTrue(params.length % 2 == 0, "参数个数必须为偶数");
        return extend(new JSONObject(), params);
    }

    public static JSONObject of(Map<String, Object> map) {
        JSONObject jsonObject = new JSONObject();
        return jsonObject.extend(map);
    }

    public static <T, V> List<V> ofList(Collection<T> collection, Function<T, V> function) {
        List<V> result = new ArrayList<>(collection.size());
        for (T item : collection) {
            result.add(function.apply(item));
        }
        return result;
    }

    /**
     * 将一个list转换成Map
     * @param collection 列表
     * @param function   转换方法，把一个item转成一个entry
     * @param <T>        列表的元素类型
     * @param <K>        key类型
     * @return
     */
    public static <T, K, V> Map<K, V> list2Map(Collection<T> collection, Function<T, Entry<K, V>> function) {
        Map<K, V> result = new LinkedHashMap<>();
        for (T item : collection) {
            Entry<K, V> entry = function.apply(item);
            Assert.notNull(entry, "function的返回值不能为null！");
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    public static <KI, VI, VO> List<VO> map2List(Map<KI, VI> map, Function<Map.Entry<KI, VI>, VO> function) {
        List<VO> result = new ArrayList<>();
        for (Map.Entry<KI, VI> entry : map.entrySet()) {
            result.add(function.apply(entry));
        }
        return result;
    }

    /**
     * 转换Map的value得到一个新的Map
     * @param sourceMap
     * @param function  返回null表示舍弃该value
     * @return
     */
    public static <KI, VI, KO, VO> Map<KO, VO> ofMap(Map<KI, VI> sourceMap, Function<Entry<KI, VI>, Entry<KO, VO>> function) {
        Map<KO, VO> result = new LinkedHashMap<>();
        for (Map.Entry<KI, VI> entryIn : sourceMap.entrySet()) {
            Entry<KO, VO> entryOut = function.apply(Entry.of(entryIn));
            if (entryOut != null) {
                result.put(entryOut.getKey(), entryOut.getValue());
            }
        }
        return result;
    }


    /*public static <K, T, V> Map<K, V> ofMap(Map<K, T> sourceMap, Function<T, V> function) {
        Map<K, V> result = Maps.newLinkedHashMap();
        for (Map.Entry<K, T> entry : sourceMap.entrySet()) {
            result.put(entry.getKey(), function.apply(entry.getValue()));
        }
        return result;
    }*/

    private static JSONObject extend(JSONObject jsonObject, Object... params) {
        Assert.isTrue(params.length % 2 == 0, "参数个数必须为偶数");
        if (params.length > 0) {
            for (int pairIndex = 0; pairIndex < params.length; pairIndex += 2) {
                String key;
                Object keyObject = params[pairIndex];
                if (keyObject.getClass().isPrimitive() || keyObject instanceof CharSequence) {
                    key = params[pairIndex].toString();
                } else {
                    throw new IllegalArgumentException(StringUtils.format("key不能为{}类型", keyObject.getClass().getName()));
                }
                Object value = params[pairIndex + 1];
                jsonObject.put(key, value);
            }
        }
        return jsonObject;
    }

    /**
     * 扩展，K、V。。。
     * @param params
     * @return
     */
    public JSONObject extend(Object... params) {
        return extend(this, params);
    }

    public JSONObject extend(Map<String, Object> map) {
        this.putAll(map);
        return this;
    }


//    @SuppressWarnings("unchecked")
//    public <T> T get(String key, TypeReference<T> type) {
//        return (T) get(key);
//    }

    public String getNullableString(String key) {
        return Objects.toString(this.get(key), null);
    }

    public String getNullableString(String key, String defaultValue) {
        return Objects.toString(this.get(key), defaultValue);
    }

    public String getString(String key) throws IllegalArgumentException {
        String value = this.getNullableString(key);
        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException("参数无效:" + key);
        }
        return value;
    }

    public Integer getNullableInt(String key) {
        if (this.get(key) == null) {
            return null;
        }
        return getInt(key);
    }

    public int getInt(String key) throws IllegalArgumentException {
        try {
            return Integer.parseInt(this.getNullableString(key));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("参数" + key + "无效！");
        }
    }

    public long getLong(String key) throws IllegalArgumentException {
        try {
            return Long.parseLong(this.getNullableString(key));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("参数" + key + "无效！");
        }
    }

    public Long getNullableLong(String key) {
        if (StringUtils.isBlank(this.getNullableString(key))) {
            return null;
        }
        return getLong(key);
    }

    public String toJSONString() {
        return JSON.stringify(this);
    }

    @Override
    public String toString() {
        return this.toJSONString();
    }

    public String toMapString() {
        return super.toString();
    }
}
