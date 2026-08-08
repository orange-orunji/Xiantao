/*
 ============================================================
  闲淘（XianTao）· 校园二手闲置交易平台 - 数据库初始化脚本
  适用版本：MySQL 8.0+
  说明：执行本脚本会创建 xiantao 库并写入演示数据
 ============================================================
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `xiantao` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `xiantao`;

-- ----------------------------
-- 1. tb_goods_type 商品分类表
-- ----------------------------
DROP TABLE IF EXISTS `tb_goods_type`;
CREATE TABLE `tb_goods_type` (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(32) DEFAULT NULL COMMENT '分类名称',
  `icon` varchar(255) DEFAULT NULL COMMENT '图标',
  `sort` int(3) UNSIGNED DEFAULT NULL COMMENT '排序',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE = InnoDB AUTO_INCREMENT = 9 DEFAULT CHARSET = utf8mb4 COMMENT = '二手商品分类表';

INSERT INTO `tb_goods_type` VALUES (1, '数码家电', '/types/ms.png', 1, '2026-07-01 10:00:00', '2026-07-01 10:00:00');
INSERT INTO `tb_goods_type` VALUES (2, '图书教材', '/types/hpg.png', 2, '2026-07-01 10:00:00', '2026-07-01 10:00:00');
INSERT INTO `tb_goods_type` VALUES (3, '服饰鞋包', '/types/lrmf.png', 3, '2026-07-01 10:00:00', '2026-07-01 10:00:00');
INSERT INTO `tb_goods_type` VALUES (4, '运动户外', '/types/jsyd.png', 4, '2026-07-01 10:00:00', '2026-07-01 10:00:00');
INSERT INTO `tb_goods_type` VALUES (5, '美妆个护', '/types/spa.png', 5, '2026-07-01 10:00:00', '2026-07-01 10:00:00');
INSERT INTO `tb_goods_type` VALUES (6, '乐器', '/types/jiuba.png', 6, '2026-07-01 10:00:00', '2026-07-01 10:00:00');
INSERT INTO `tb_goods_type` VALUES (7, '家居日用', '/types/qzyl.png', 7, '2026-07-01 10:00:00', '2026-07-01 10:00:00');
INSERT INTO `tb_goods_type` VALUES (8, '其他闲置', '/types/mjmj.png', 8, '2026-07-01 10:00:00', '2026-07-01 10:00:00');

-- ----------------------------
-- 2. tb_user 用户表
-- ----------------------------
DROP TABLE IF EXISTS `tb_user`;
CREATE TABLE `tb_user` (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `phone` varchar(11) NOT NULL COMMENT '手机号码',
  `password` varchar(128) DEFAULT '' COMMENT '密码，加密存储',
  `nick_name` varchar(32) DEFAULT '' COMMENT '昵称',
  `icon` varchar(255) DEFAULT '' COMMENT '头像',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniqe_key_phone` (`phone`)
) ENGINE = InnoDB AUTO_INCREMENT = 3 DEFAULT CHARSET = utf8mb4 COMMENT = '用户表';

INSERT INTO `tb_user` VALUES (1, '13686869696', '', '小鱼同学', '/imgs/notes/note1.jpg', '2026-07-01 10:10:00', '2026-07-01 10:10:00');
INSERT INTO `tb_user` VALUES (2, '13838411438', '', '可可今天不吃肉', '/imgs/icons/kkjtbcr.jpg', '2026-07-01 10:11:00', '2026-07-01 10:11:00');

-- ----------------------------
-- 3. tb_user_info 用户详情表
-- ----------------------------
DROP TABLE IF EXISTS `tb_user_info`;
CREATE TABLE `tb_user_info` (
  `user_id` bigint(20) UNSIGNED NOT NULL COMMENT '主键，用户id',
  `city` varchar(64) DEFAULT '' COMMENT '城市名称',
  `introduce` varchar(128) DEFAULT NULL COMMENT '个人介绍',
  `fans` int(8) UNSIGNED DEFAULT 0 COMMENT '粉丝数量',
  `followee` int(8) UNSIGNED DEFAULT 0 COMMENT '关注数',
  `gender` tinyint(1) UNSIGNED DEFAULT 0 COMMENT '性别，0：男，1：女',
  `birthday` date DEFAULT NULL COMMENT '生日',
  `credits` int(8) UNSIGNED DEFAULT 0 COMMENT '积分',
  `level` tinyint(1) UNSIGNED DEFAULT 0 COMMENT '会员级别，0~9级',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户详情表';

INSERT INTO `tb_user_info` VALUES (1, '杭州', '毕业党，闲置好物出清中~', 2, 1, 1, '2002-06-01', 100, 2, '2026-07-01 10:10:00', '2026-07-01 10:10:00');
INSERT INTO `tb_user_info` VALUES (2, '杭州', '二手淘换星人，喜欢淘宝贝', 1, 1, 0, '2003-03-15', 60, 1, '2026-07-01 10:11:00', '2026-07-01 10:11:00');

-- ----------------------------
-- 4. tb_goods 二手商品表
-- ----------------------------
DROP TABLE IF EXISTS `tb_goods`;
CREATE TABLE `tb_goods` (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(128) NOT NULL COMMENT '商品名称',
  `type_id` bigint(20) UNSIGNED NOT NULL COMMENT '商品分类id',
  `images` varchar(1024) NOT NULL COMMENT '商品图片，多个以逗号隔开',
  `area` varchar(128) DEFAULT NULL COMMENT '所在区域',
  `address` varchar(255) DEFAULT NULL COMMENT '交易地址（自提/当面交易）',
  `x` double UNSIGNED NOT NULL COMMENT '经度',
  `y` double UNSIGNED NOT NULL COMMENT '纬度',
  `price` bigint(10) UNSIGNED DEFAULT NULL COMMENT '售价（元）',
  `sold` int(10) UNSIGNED NOT NULL DEFAULT 0 COMMENT '成交数量',
  `comments` int(10) UNSIGNED NOT NULL DEFAULT 0 COMMENT '留言/想要数量',
  `score` int(2) UNSIGNED NOT NULL DEFAULT 50 COMMENT '卖家信用评分，1~5分乘10保存',
  `condition` varchar(64) DEFAULT NULL COMMENT '成色描述，例如 95新、轻微使用痕迹',
  `seller_id` bigint(20) UNSIGNED NOT NULL COMMENT '卖家用户id',
  `status` tinyint(1) UNSIGNED NOT NULL DEFAULT 1 COMMENT '商品状态，1：在售；2：已售；3：下架',
  `description` varchar(2048) DEFAULT NULL COMMENT '商品描述',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_type_id` (`type_id`),
  KEY `idx_seller_id` (`seller_id`)
) ENGINE = InnoDB AUTO_INCREMENT = 13 DEFAULT CHARSET = utf8mb4 COMMENT = '二手商品表';

INSERT INTO `tb_goods` VALUES (1, '罗技 G102 游戏鼠标', 1, '/imgs/notes/note1.jpg,/imgs/notes/note2.jpg', '大学城', '文一西路 100 号宿舍楼下自提', 120.149192, 30.316078, 45, 3, 8, 48, '95新，几乎无使用痕迹', 1, 1, '去年双十一购入，手感很好，毕业出闲置。附赠一根编织数据线，盒子还在。', '2026-07-02 09:00:00', '2026-07-10 18:00:00');
INSERT INTO `tb_goods` VALUES (2, '高数教材全套 5 本', 2, '/imgs/notes/note3.jpg', '大学城', '图书馆东门门口交易', 120.150526, 30.325231, 30, 1, 5, 50, '8成新，有少量笔记', 2, 1, '高数上下册+线性代数+概率论+习题册，都有重点勾画，期末复习必备。', '2026-07-02 10:00:00', '2026-07-09 15:30:00');
INSERT INTO `tb_goods` VALUES (3, '小米手环 6 标准版', 1, '/imgs/notes/note4.jpg', '大学城', '生活区 5 号楼楼下', 120.151505, 30.333422, 80, 2, 12, 49, '9成新，表带无磨损', 1, 1, '换了新手表所以出掉，功能一切正常，续航一周，配件齐全。', '2026-07-03 11:00:00', '2026-07-08 20:00:00');
INSERT INTO `tb_goods` VALUES (4, 'AirPods 2 代（有线充电盒）', 1, '/imgs/notes/note5.jpg', '大学城', '体育馆南门', 120.151954, 30.324970, 320, 1, 15, 47, '9成新，电池健康', 2, 1, '用了半年，音质正常，盒子有些许划痕，不影响使用。', '2026-07-03 14:00:00', '2026-07-12 12:00:00');
INSERT INTO `tb_goods` VALUES (5, '考研英语词汇书', 2, '/imgs/notes/note6.jpg', '大学城', '快递站附近', 120.146659, 30.312742, 12, 0, 3, 50, '9成新，无折痕', 1, 1, '考完研出，红宝书+便携版，都包了书皮，很干净。', '2026-07-04 09:30:00', '2026-07-11 16:00:00');
INSERT INTO `tb_goods` VALUES (6, '尤克里里 23 寸桃花芯木', 6, '/imgs/notes/note7.jpg', '大学城', '艺术楼前广场', 120.157780, 30.310633, 150, 1, 6, 48, '85新，面板有一处磕碰', 2, 1, '音色不错，附赠调音器和琴包，适合新手入门。', '2026-07-04 15:00:00', '2026-07-13 10:00:00');
INSERT INTO `tb_goods` VALUES (7, '迪卡侬山地自行车 26 寸', 4, '/imgs/notes/note8.jpg', '大学城', '东门车棚', 120.148603, 30.318618, 480, 2, 20, 49, '8成新，刚做完全车保养', 1, 1, '毕业出手，变速顺畅，刹车灵敏，骑行通勤利器，带锁和车灯。', '2026-07-05 08:00:00', '2026-07-14 19:00:00');
INSERT INTO `tb_goods` VALUES (8, '冬季羽绒服（175cm）', 3, '/imgs/notes/note9.jpg', '大学城', '女生宿舍 2 栋楼下', 120.124691, 30.336819, 120, 0, 4, 45, '9成新，洗过一次', 2, 1, '穿了一冬，羽绒蓬松，码数 175，适合男生或者宽松穿法的女生。', '2026-07-05 13:00:00', '2026-07-10 09:00:00');
INSERT INTO `tb_goods` VALUES (9, '宜家 LED 护眼台灯', 7, '/imgs/notes/note1.jpg', '大学城', '研究生公寓门口', 120.150598, 30.325251, 25, 1, 7, 50, '95新，无划痕', 1, 1, '三档调光，宿舍学习必备，毕业出闲置。', '2026-07-06 10:00:00', '2026-07-15 21:00:00');
INSERT INTO `tb_goods` VALUES (10, '雅马哈 F310 民谣吉他', 6, '/imgs/notes/note2.jpg', '大学城', '社团活动中心', 120.149093, 30.324666, 380, 1, 9, 49, '85新，琴颈笔直', 2, 1, '初学神器，弦距合适不伤手，附赠变调夹和教材。', '2026-07-06 16:00:00', '2026-07-16 14:00:00');
INSERT INTO `tb_goods` VALUES (11, '迷你电饭煲 2L', 7, '/imgs/notes/note3.jpg', '大学城', '西门快递柜旁', 120.158530, 30.310002, 60, 0, 2, 48, '9成新，内胆无刮痕', 1, 1, '宿舍违规电器，毕业出掉，煮饭煮粥都可以，很实用。', '2026-07-07 09:00:00', '2026-07-17 18:00:00');
INSERT INTO `tb_goods` VALUES (12, '机械键盘 青轴 87 键', 1, '/imgs/notes/note4.jpg', '大学城', '信息楼大厅', 120.149830, 30.312110, 150, 2, 11, 50, '95新，键帽无打油', 2, 1, '茶轴换青轴退烧，手感清脆，适合打字党，带拔键器。', '2026-07-07 14:00:00', '2026-07-18 20:00:00');

-- ----------------------------
-- 5. tb_flash_sale 捡漏活动表（原代金券业务改造）
-- ----------------------------
DROP TABLE IF EXISTS `tb_flash_sale`;
CREATE TABLE `tb_flash_sale` (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `goods_id` bigint(20) UNSIGNED DEFAULT NULL COMMENT '商品id',
  `title` varchar(255) NOT NULL COMMENT '捡漏活动标题',
  `sub_title` varchar(255) DEFAULT NULL COMMENT '副标题',
  `rules` varchar(1024) DEFAULT NULL COMMENT '交易说明/规则',
  `price` bigint(10) UNSIGNED NOT NULL COMMENT '捡漏价，单位是分。例如 3000 代表 30 元',
  `market_value` bigint(10) NOT NULL COMMENT '市场价，单位是分',
  `type` tinyint(1) UNSIGNED NOT NULL DEFAULT 0 COMMENT '类型，0：普通捡漏；1：限时抢购',
  `status` tinyint(1) UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态，1：上架；2：下架',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_goods_id` (`goods_id`)
) ENGINE = InnoDB AUTO_INCREMENT = 3 DEFAULT CHARSET = utf8mb4 COMMENT = '捡漏活动表';

INSERT INTO `tb_flash_sale` VALUES (1, 1, '罗技 G102 捡漏专场', '限量 100 件，先到先得', '限大学城内自提\\n拍下后 24 小时内完成交易\\n非质量问题不退换', 3000, 4500, 1, 1, '2026-07-08 10:00:00', '2026-07-08 10:00:00');
INSERT INTO `tb_flash_sale` VALUES (2, 7, '毕业季甩卖·山地车捡漏', '毕业清仓，价格美丽', '限当面验货后交易\\n支持试骑\\n售出不退不换', 38000, 48000, 0, 1, '2026-07-08 10:00:00', '2026-07-08 10:00:00');

-- ----------------------------
-- 6. tb_flash_stock 捡漏库存表（与捡漏活动一对一）
-- ----------------------------
DROP TABLE IF EXISTS `tb_flash_stock`;
CREATE TABLE `tb_flash_stock` (
  `flash_id` bigint(20) UNSIGNED NOT NULL COMMENT '关联的捡漏活动id',
  `stock` int(8) NOT NULL COMMENT '库存',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `begin_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生效时间',
  `end_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '失效时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`flash_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '捡漏库存表';

INSERT INTO `tb_flash_stock` VALUES (1, 100, '2026-07-08 10:00:00', '2026-07-08 10:00:00', '2026-12-31 23:59:59', '2026-07-08 10:00:00');
INSERT INTO `tb_flash_stock` VALUES (2, 1, '2026-07-08 10:00:00', '2026-07-08 10:00:00', '2026-12-31 23:59:59', '2026-07-08 10:00:00');

-- ----------------------------
-- 7. tb_flash_order 捡漏订单表
-- ----------------------------
DROP TABLE IF EXISTS `tb_flash_order`;
CREATE TABLE `tb_flash_order` (
  `id` bigint(20) NOT NULL COMMENT '主键（分布式ID）',
  `user_id` bigint(20) UNSIGNED NOT NULL COMMENT '下单用户id',
  `flash_id` bigint(20) UNSIGNED NOT NULL COMMENT '购买的捡漏活动id',
  `goods_id` bigint(20) UNSIGNED DEFAULT NULL COMMENT '商品id',
  `pay_type` tinyint(1) UNSIGNED NOT NULL DEFAULT 1 COMMENT '支付方式 1：余额支付；2：支付宝；3：微信',
  `status` tinyint(1) UNSIGNED NOT NULL DEFAULT 1 COMMENT '订单状态，1：未支付；2：已支付；3：已确认收货；4：已取消；5：退款中；6：已退款',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
  `pay_time` timestamp NULL DEFAULT NULL COMMENT '支付时间',
  `use_time` timestamp NULL DEFAULT NULL COMMENT '确认收货时间',
  `refund_time` timestamp NULL DEFAULT NULL COMMENT '退款时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '捡漏订单表';

-- ----------------------------
-- 8. tb_goods_note 好物笔记表（原探店博客业务改造）
-- ----------------------------
DROP TABLE IF EXISTS `tb_goods_note`;
CREATE TABLE `tb_goods_note` (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `goods_id` bigint(20) NOT NULL COMMENT '关联商品id',
  `user_id` bigint(20) UNSIGNED NOT NULL COMMENT '用户id',
  `title` varchar(255) NOT NULL COMMENT '标题',
  `images` varchar(2048) NOT NULL COMMENT '图片，最多9张，多张以逗号隔开',
  `content` varchar(2048) NOT NULL COMMENT '晒物描述',
  `liked` int(8) UNSIGNED DEFAULT 0 COMMENT '点赞数量',
  `comments` int(8) UNSIGNED DEFAULT 0 COMMENT '评论数量',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_goods_id` (`goods_id`)
) ENGINE = InnoDB AUTO_INCREMENT = 5 DEFAULT CHARSET = utf8mb4 COMMENT = '好物笔记表';

INSERT INTO `tb_goods_note` VALUES (1, 1, 1, '毕业清仓｜罗技 G102 鼠标出闲置', '/imgs/notes/note1.jpg,/imgs/notes/note2.jpg', '鼠标手感不错，适合手小的同学，毕业清仓价 45 元，宿舍楼下自提～', 3, 2, '2026-07-02 09:30:00', '2026-07-02 09:30:00');
INSERT INTO `tb_goods_note` VALUES (2, 3, 2, '小米手环 6 出闲置，续航一周', '/imgs/notes/note4.jpg', '换了新手表，旧手环 80 出，功能一切正常，配件齐全，需要的同学私信我～', 5, 3, '2026-07-03 11:30:00', '2026-07-03 11:30:00');
INSERT INTO `tb_goods_note` VALUES (3, 7, 1, '毕业了，我的山地车找个新主人', '/imgs/notes/note8.jpg', '跟了我四年的迪卡侬山地车，刚做完保养，变速刹车都调试过了，480 出，附赠车锁车灯～', 8, 4, '2026-07-05 08:30:00', '2026-07-05 08:30:00');
INSERT INTO `tb_goods_note` VALUES (4, 2, 2, '高数教材全套 5 本 30 元，有笔记', '/imgs/notes/note3.jpg', '都是重点勾画过的教材，期末复习神器，学弟学妹们快来捡漏～', 2, 1, '2026-07-02 10:30:00', '2026-07-02 10:30:00');

-- ----------------------------
-- 9. tb_note_comment 笔记评论表
-- ----------------------------
DROP TABLE IF EXISTS `tb_note_comment`;
CREATE TABLE `tb_note_comment` (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint(20) UNSIGNED NOT NULL COMMENT '用户id',
  `note_id` bigint(20) UNSIGNED NOT NULL COMMENT '笔记id',
  `parent_id` bigint(20) UNSIGNED NOT NULL DEFAULT 0 COMMENT '关联的1级评论id，一级评论为0',
  `answer_id` bigint(20) UNSIGNED NOT NULL DEFAULT 0 COMMENT '回复的评论id',
  `content` varchar(255) NOT NULL COMMENT '回复的内容',
  `liked` int(8) UNSIGNED DEFAULT 0 COMMENT '点赞数',
  `status` tinyint(1) UNSIGNED DEFAULT 0 COMMENT '状态，0：正常，1：被举报，2：禁止查看',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_note_id` (`note_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '笔记评论表';

-- ----------------------------
-- 10. tb_follow 关注表
-- ----------------------------
DROP TABLE IF EXISTS `tb_follow`;
CREATE TABLE `tb_follow` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint(20) UNSIGNED NOT NULL COMMENT '用户id',
  `follow_user_id` bigint(20) UNSIGNED NOT NULL COMMENT '被关注的用户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '关注表';

-- ----------------------------
-- 11. tb_sign 签到表
-- ----------------------------
DROP TABLE IF EXISTS `tb_sign`;
CREATE TABLE `tb_sign` (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint(20) UNSIGNED NOT NULL COMMENT '用户id',
  `year` year NOT NULL COMMENT '签到的年',
  `month` tinyint(2) NOT NULL COMMENT '签到的月',
  `date` date NOT NULL COMMENT '签到的日期',
  `is_backup` tinyint(1) UNSIGNED DEFAULT NULL COMMENT '是否补签',
  PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '签到表';

-- ----------------------------
-- 12. tb_message 私信表
-- ----------------------------
DROP TABLE IF EXISTS `tb_message`;
CREATE TABLE `tb_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `sender_id` bigint NOT NULL COMMENT '发送者用户id',
  `receiver_id` bigint NOT NULL COMMENT '接收者用户id',
  `content` varchar(500) NOT NULL COMMENT '消息内容',
  `is_read` tinyint(1) DEFAULT '0' COMMENT '是否已读，0未读 1已读',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  PRIMARY KEY (`id`),
  KEY `idx_sender_receiver` (`sender_id`, `receiver_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '私信表';

-- ----------------------------
-- 13. tb_notification 通知表
-- ----------------------------
DROP TABLE IF EXISTS `tb_notification`;
CREATE TABLE `tb_notification` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '接收通知的用户id',
  `from_user_id` bigint NOT NULL COMMENT '触发通知的用户id',
  `type` tinyint NOT NULL COMMENT '通知类型：1点赞 2评论 3关注',
  `related_id` bigint DEFAULT NULL COMMENT '关联id（笔记id或评论id）',
  `content` varchar(255) DEFAULT NULL COMMENT '通知摘要内容',
  `is_read` tinyint(1) DEFAULT '0' COMMENT '是否已读，0未读 1已读',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '通知表';

-- ----------------------------
-- 14. tb_goods_want 商品想要记录表（个人主页"我的想要"）
-- ----------------------------
DROP TABLE IF EXISTS `tb_goods_want`;
CREATE TABLE `tb_goods_want` (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint(20) UNSIGNED NOT NULL COMMENT '用户id',
  `goods_id` bigint(20) UNSIGNED NOT NULL COMMENT '商品id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '想要时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_goods` (`user_id`, `goods_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商品想要记录表';

SET FOREIGN_KEY_CHECKS = 1;
