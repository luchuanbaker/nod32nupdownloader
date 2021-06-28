package com.clu.tools.utils;

import org.apache.commons.lang3.ArrayUtils;

import java.util.Collection;

/**
 * 参数断言工具类
 * @author clu
 * @version 1.0
 * @date 2017-6-26上午11:51:01
 * @since 1.0.0
 */
public class Assert {

    public static void isTrue(boolean expression, String message) {
        if (!expression) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 1个或0个
     * @param coll
     * @param message
     * @param params
     */
    public static void onlyOneOrEmpty(Collection<?> coll, String message, Object... params) {
        if (!ArrayUtils.isEmpty(params)) {
            message = String.format(message, params);
        }
        isTrue(coll == null || coll.size() <= 1, message);
    }

    public static void notNull(Object o, String message) {
        if (o == null) {
            throw new IllegalArgumentException(message);
        }
    }
}
