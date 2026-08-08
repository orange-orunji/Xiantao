package com.xiantao.utils;

public class RedisConstants {
    public static final String LOGIN_CODE_KEY = "login:code:";
    public static final Long LOGIN_CODE_TTL = 2L;
    public static final String LOGIN_USER_KEY = "login:token:";
    public static final Long LOGIN_USER_TTL = 36000L;

    public static final Long CACHE_NULL_TTL = 2L;

    public static final Long CACHE_GOODS_TTL = 30L;
    public static final String CACHE_GOODS_KEY = "cache:goods:";
    public static final String CASH_GOODS_TYPE_KEY = "cache:goodstype:list:";

    public static final String LOCK_GOODS_KEY = "lock:goods:";
    public static final Long LOCK_GOODS_TTL = 10L;

    public static final String FLASH_STOCK_KEY = "flash:stock:";
    public static final String NOTE_LIKED_KEY = "note:liked:";
    public static final String GOODS_WANT_KEY = "goods:want:";
    public static final String BROWSE_GOODS_KEY = "browse:goods:";
    public static final String FEED_KEY = "feed:";
    public static final String GOODS_GEO_KEY = "goods:geo:";
    public static final String USER_SIGN_KEY = "sign:";
}
