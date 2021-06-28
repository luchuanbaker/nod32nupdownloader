package com.clu.tools.utils;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.helpers.MessageFormatter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class StringUtils extends org.apache.commons.lang3.StringUtils {

    public static String intListToStr(List<Integer> list) {
        StringBuilder builder = new StringBuilder();
        for (Integer item : list) {
            builder.append(item).append(",");
        }
        return builder.substring(0, builder.length() - 1);
    }

    public static String longListToStr(List<Long> list) {
        StringBuilder builder = new StringBuilder();
        for (Long item : list) {
            builder.append(item).append(",");
        }
        return builder.substring(0, builder.length() - 1);
    }

    public static String encryptToMd5(String str) {
        MessageDigest messageDigest = null;
        try {
            messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.reset();
            messageDigest.update(str.getBytes("UTF-8"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        byte[] byteArray = messageDigest.digest();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < byteArray.length; i++) {
            if (Integer.toHexString(0xFF & byteArray[i]).length() == 1)
                builder.append("0").append(Integer.toHexString(0xFF & byteArray[i]));
            else
                builder.append(Integer.toHexString(0xFF & byteArray[i]));
        }
        return builder.toString();
    }

    /**
     * 判断字符串是否为空 null or "" return true; "  " return true; " pop" return false;
     * "  pop  " return false;
     * @param x
     * @return
     */
    public static boolean isEmpty(String x) {
        int len;
        if (x == null || (len = x.length()) == 0)
            return true;

        while (len-- > 0) {
            if (!Character.isWhitespace(x.charAt(len)))
                return false;
        }

        return true;
    }

    /**
     * 提供类似于slf4j的日志+参数自动合并的工具
     * @param message
     * @param params
     * @return
     */
    public static String format(String message, Object... params) {
        if (ArrayUtils.isEmpty(params)) {
            return message;
        }
        return MessageFormatter.arrayFormat(message, params).getMessage();
    }

    /**
     * 驼峰转下划线
     * @param input
     * @return
     */
    public static String camelToUnderline(CharSequence input) {
        if (input == null) return null; // garbage in, garbage out
        int length = input.length();
        StringBuilder result = new StringBuilder(length * 2);
        int resultLength = 0;
        boolean wasPrevTranslated = false;
        for (int i = 0; i < length; i++) {
            char c = input.charAt(i);
            if (i > 0 || c != '_') // skip first starting underscore
            {
                if (Character.isUpperCase(c)) {
                    if (!wasPrevTranslated && resultLength > 0 && result.charAt(resultLength - 1) != '_') {
                        result.append('_');
                        resultLength++;
                    }
                    c = Character.toLowerCase(c);
                    wasPrevTranslated = true;
                } else {
                    wasPrevTranslated = false;
                }
                result.append(c);
                resultLength++;
            }
        }
        return resultLength > 0 ? result.toString() : null;
    }

    public static String repeat(String value, int count, String seperator) {
        List<String> values = new ArrayList<String>();
        for (int i = 0; i < count; i++) {
            values.add(value);
        }
        return StringUtils.join(values, seperator);
    }

    public static <T> String join(Collection<T> collection) {
        return join(collection, new Function<T, String>() {
            @Override
            public String apply(T input) {
                return Objects.toString(input, "");
            }
        });
    }

    public static <T> String join(Collection<T> collection, final Function<T, String> function) {
        List<String> list = new ArrayList<>();
        for (T item : collection) {
            list.add(function.apply(item));
        }
        return join(list, ",");
    }

    public static String joinMySQLColumns(Collection<String> columns) {
        StringBuilder builder = new StringBuilder();
        for (String name : columns) {
            builder.append("`").append(name).append("`").append(",");
        }

        if (builder.length() > 0) {
            // 删除最后一个逗号
            builder.deleteCharAt(builder.length() - 1);
        }
        return builder.toString();
    }

    public static boolean isMobilePhoneNumber(String string) {
        return isNumeric(string) && string.trim().startsWith("1") && string.trim().length() == 11;
    }

    public static String getStackTrace(Throwable t) {
        if (t == null) {
            return null;
        }
        StringWriter writer = new StringWriter();
        t.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    /**
     * 将15s、3m、1h之类的格式转换为秒
     * @param timeStr
     * @return
     */
    public static long getTimeAsSeconds(String timeStr) {
        if (StringUtils.isBlank(timeStr)) {
            throw new IllegalArgumentException(StringUtils.format("非法的时间格式(1)：{}", timeStr));
        }
        // 去空格，全部转小写
        timeStr = timeStr.trim().toLowerCase();
        if (!timeStr.matches("\\d+[smh]")) {
            throw new IllegalArgumentException(StringUtils.format("非法的时间格式(2)：{}", timeStr));
        }
        int numberSegment = Integer.parseInt(timeStr.substring(0, timeStr.length() - 1));
        char c = timeStr.charAt(timeStr.length() - 1);
        if (c == 's') {
            return numberSegment;
        } else if (c == 'm') {
            return numberSegment * 60;
        } else if (c == 'h') {
            return numberSegment * 60 * 60;
        } {
            throw new RuntimeException(StringUtils.format("未知格式：{}", timeStr));
        }
    }

    /**
     * 把秒转换成友好的时间格式，比如：1d2h3m
     * @param seconds
     * @return
     */
    public static String toShortTimeStr(long seconds) {
        if (seconds == 0) {
            return "0s";
        }

        StringBuilder builder = new StringBuilder();
        long leftSeconds = seconds;

        int[] units = new int[]{
            60 * 60 * 24,
            60 * 60,
            60,
            1
        };
        char[] chars = new char[] {
            'd', 'h', 'm', 's'
        };

        int index = 0;
        while (leftSeconds > 0 && index < units.length) {
            int unit = units[index];
            long n = leftSeconds / unit;
            if (n > 0) {
                builder.append(n).append(chars[index]);
            }
            leftSeconds = seconds % unit;
            index++;
        }
        return builder.toString();
    }

//    public static void main(String[] args) {
//        long timeAsSeconds = getTimeAsSeconds("1s");
//        System.out.println(timeAsSeconds);
//        System.out.println(toShortTimeStr(timeAsSeconds));
//    }

}