package com.clu.tools;

public class Constants {

    /**
     * 是否是Windows环境，即：是否是开发环境
     */
    public static final boolean IS_WIN;

    static {
        String os = System.getProperty("os.name");
        IS_WIN = os.toLowerCase().startsWith("win");
    }

}
