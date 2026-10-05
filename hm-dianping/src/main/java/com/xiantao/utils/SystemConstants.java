package com.xiantao.utils;

public class SystemConstants {
    // 图片上传目录：优先读环境变量 IMAGE_UPLOAD_DIR；默认相对仓库根目录（Docker / 从仓库根启动均适用）
    public static final String IMAGE_UPLOAD_DIR =
            System.getenv().getOrDefault("IMAGE_UPLOAD_DIR", "nginx-1.18.0/html/xiantao/imgs");
    public static final String USER_NICK_NAME_PREFIX = "user_";
    public static final int DEFAULT_PAGE_SIZE = 5;
    public static final int MAX_PAGE_SIZE = 10;
}
