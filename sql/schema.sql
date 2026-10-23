-- ============================================================
-- 校园二手书交易平台 · 核心表结构
-- DB: campus_book    Engine: InnoDB    Charset: utf8mb4
-- 适用: MySQL 8.0（5.7 亦可执行，仅默认表达式略有差异）
-- 执行: mysql -uroot -p < schema.sql
-- 表: user(用户) / book(书籍) / orders(订单)
-- ============================================================

CREATE DATABASE IF NOT EXISTS `campus_book`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE `campus_book`;

-- Bootstrap only: never drop existing tables or data. This script does not migrate existing schemas.

-- ------------------------------------------------------------
-- 1. 用户表：买家与卖家是同一类人，只是同一张表里的不同角色
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user` (
  `id`           BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `student_no`   VARCHAR(32)      NOT NULL                COMMENT '学号，登录账号，校园内唯一',
  `nickname`     VARCHAR(50)      NOT NULL                COMMENT '昵称',
  `password`     VARCHAR(100)     NOT NULL                COMMENT 'BCrypt 加密后的密码，禁止存明文',
  `real_name`    VARCHAR(50)      DEFAULT NULL            COMMENT '真实姓名，实名认证后回填',
  `phone`        VARCHAR(20)      DEFAULT NULL            COMMENT '手机号，用于联系与找回密码',
  `email`        VARCHAR(100)     DEFAULT NULL            COMMENT '邮箱',
  `campus`       VARCHAR(50)      DEFAULT NULL            COMMENT '校区，用于就近交易筛选',
  `avatar_url`   VARCHAR(255)     DEFAULT NULL            COMMENT '头像地址',
  `credit_score` INT              NOT NULL DEFAULT 100    COMMENT '信用分，违约/差评扣分',
  `role`         TINYINT UNSIGNED NOT NULL DEFAULT 0      COMMENT '角色：0-普通用户 1-管理员',
  `status`       TINYINT UNSIGNED NOT NULL DEFAULT 1      COMMENT '状态：1-正常 0-禁用',
  `created_at`   DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `updated_at`   DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`      TINYINT UNSIGNED NOT NULL DEFAULT 0      COMMENT '逻辑删除：0-未删 1-已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_no` (`student_no`),
  KEY `idx_phone` (`phone`),
  KEY `idx_campus_status` (`campus`, `status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户表';

-- ------------------------------------------------------------
-- 2. 书籍表：一本"正在出售的二手书"，既是商品也是库存
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `book` (
  `id`              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '书籍ID',
  `seller_id`       BIGINT UNSIGNED  NOT NULL                COMMENT '卖家ID -> user.id',
  `title`           VARCHAR(200)     NOT NULL                COMMENT '书名',
  `author`          VARCHAR(100)     DEFAULT NULL            COMMENT '作者',
  `isbn`            VARCHAR(20)      DEFAULT NULL            COMMENT 'ISBN-13，可空；有值时用于同版书匹配',
  `publisher`       VARCHAR(100)     DEFAULT NULL            COMMENT '出版社',
  `publish_date`    DATE             DEFAULT NULL            COMMENT '出版日期',
  `category`        VARCHAR(50)      DEFAULT NULL            COMMENT '分类：教材/考研/四六级/计算机/文学…',
  `original_price`  DECIMAL(10, 2)   DEFAULT NULL            COMMENT '原价（划线价）',
  `price`           DECIMAL(10, 2)   NOT NULL                COMMENT '售价，必须 >= 0',
  `condition_level` TINYINT UNSIGNED NOT NULL DEFAULT 9      COMMENT '成色 1-10，10=全新',
  `stock`           INT UNSIGNED     NOT NULL DEFAULT 1      COMMENT '库存，二手书通常为 1',
  `cover_url`       VARCHAR(255)     DEFAULT NULL            COMMENT '封面图',
  `description`     TEXT             DEFAULT NULL            COMMENT '品相描述、瑕疵说明',
  `status`          TINYINT UNSIGNED NOT NULL DEFAULT 1      COMMENT '0-已下架 1-在售 2-已预订 3-已售出',
  `view_count`      INT UNSIGNED     NOT NULL DEFAULT 0      COMMENT '浏览量',
  `version`         INT UNSIGNED     NOT NULL DEFAULT 0      COMMENT '乐观锁版本号，防并发改价/下单',
  `created_at`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上架时间',
  `updated_at`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`         TINYINT UNSIGNED NOT NULL DEFAULT 0      COMMENT '逻辑删除：0-未删 1-已删',
  PRIMARY KEY (`id`),
  KEY `idx_seller_status` (`seller_id`, `status`),
  KEY `idx_status_category` (`status`, `category`),
  KEY `idx_isbn` (`isbn`),
  KEY `idx_title` (`title`(50)),
  KEY `idx_created_at` (`created_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '书籍（二手书商品）表';

-- ------------------------------------------------------------
-- 3. 订单表：一次交易的凭证，同时记录买家和卖家两个方向
--    order 是 MySQL 保留字，表名用 orders
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `orders` (
  `id`            BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no`      VARCHAR(32)      NOT NULL                COMMENT '业务订单号，如 202610221530001234',
  `book_id`       BIGINT UNSIGNED  NOT NULL                COMMENT '书籍ID -> book.id',
  `buyer_id`      BIGINT UNSIGNED  NOT NULL                COMMENT '买家ID -> user.id',
  `seller_id`     BIGINT UNSIGNED  NOT NULL                COMMENT '卖家ID -> user.id，冗余存一份，便于"我卖出的"直接查',
  `book_title`    VARCHAR(200)     NOT NULL                COMMENT '下单时的书名快照，书被删改也不影响历史订单',
  `unit_price`    DECIMAL(10, 2)   NOT NULL                COMMENT '成交单价快照',
  `quantity`      INT UNSIGNED     NOT NULL DEFAULT 1      COMMENT '购买数量',
  `total_amount`  DECIMAL(10, 2)   NOT NULL                COMMENT '订单总额 = unit_price * quantity',
  `status`        TINYINT UNSIGNED NOT NULL DEFAULT 0      COMMENT '0-待支付 1-已支付待交接 2-已完成 3-已取消 4-已退款',
  `delivery_type` TINYINT UNSIGNED NOT NULL DEFAULT 1      COMMENT '交接方式：1-校内自提 2-快递',
  `delivery_addr` VARCHAR(255)     DEFAULT NULL            COMMENT '收货地址 / 自提点',
  `remark`        VARCHAR(255)     DEFAULT NULL            COMMENT '买家留言',
  `pay_time`      DATETIME         DEFAULT NULL            COMMENT '支付时间',
  `finish_time`   DATETIME         DEFAULT NULL            COMMENT '确认收货/完成时间',
  `cancel_reason` VARCHAR(255)     DEFAULT NULL            COMMENT '取消或退款原因',
  `created_at`    DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
  `updated_at`    DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_buyer_status` (`buyer_id`, `status`, `created_at`),
  KEY `idx_seller_status` (`seller_id`, `status`, `created_at`),
  KEY `idx_book` (`book_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '订单表';

-- ------------------------------------------------------------
-- 4.【可选】外键约束
--    课程设计 / 单库部署：建议打开，保证引用完整性、能画 ER 图
--    生产环境 / 分库分表 / 高并发：通常不建，靠应用层 + 索引保证
--    打开前先保证插入顺序：user -> book -> orders
-- ------------------------------------------------------------
-- ALTER TABLE `book`
--   ADD CONSTRAINT `fk_book_seller` FOREIGN KEY (`seller_id`) REFERENCES `user` (`id`);
-- ALTER TABLE `orders`
--   ADD CONSTRAINT `fk_orders_book`   FOREIGN KEY (`book_id`)   REFERENCES `book` (`id`),
--   ADD CONSTRAINT `fk_orders_buyer`  FOREIGN KEY (`buyer_id`)  REFERENCES `user` (`id`),
--   ADD CONSTRAINT `fk_orders_seller` FOREIGN KEY (`seller_id`) REFERENCES `user` (`id`);

-- ------------------------------------------------------------
-- 5.【可选】若要做购物车、一次下单买多本，再拆出订单明细表
--    此时 orders 只保留 order_no / buyer_id / total_amount / status 等主单字段
-- ------------------------------------------------------------
-- CREATE TABLE `order_item` (
--   `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '明细ID',
--   `order_id`     BIGINT UNSIGNED NOT NULL COMMENT '订单ID -> orders.id',
--   `book_id`      BIGINT UNSIGNED NOT NULL COMMENT '书籍ID -> book.id',
--   `book_title`   VARCHAR(200)    NOT NULL COMMENT '书名快照',
--   `unit_price`   DECIMAL(10, 2)  NOT NULL COMMENT '成交单价快照',
--   `quantity`     INT UNSIGNED    NOT NULL DEFAULT 1 COMMENT '购买数量',
--   `subtotal`     DECIMAL(10, 2)  NOT NULL COMMENT '小计',
--   PRIMARY KEY (`id`),
--   KEY `idx_order_id` (`order_id`),
--   KEY `idx_book_id` (`book_id`)
-- ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单明细表';

-- ------------------------------------------------------------
-- 6. 防"一本书被两个人同时买走"的写法（下单事务里执行）
--    受影响行数为 1 才允许插入订单，否则回滚并提示"已被抢走"
-- ------------------------------------------------------------
-- 注意：MySQL 单表 UPDATE 的 SET 赋值从左到右执行，
--       所以必须先判断 status、再扣 stock，否则 stock 已被扣减，判断会错。
-- UPDATE `book`
--    SET `status`  = IF(`stock` <= 1, 3, `status`),
--        `stock`   = `stock` - 1,
--        `version` = `version` + 1
--  WHERE `id` = ?
--    AND `status` = 1
--    AND `deleted` = 0
--    AND `stock` >= 1;

-- ------------------------------------------------------------
-- 7. 举报表：普通用户举报违规书籍，管理员处理
--    status: 0-待处理 1-已下架 2-已驳回
--    同一用户对同一本书只能有一条待处理举报
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `report` (
  `id`          BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '举报ID',
  `reporter_id` BIGINT UNSIGNED  NOT NULL                COMMENT '举报人ID -> user.id',
  `book_id`     BIGINT UNSIGNED  NOT NULL                COMMENT '被举报书籍ID -> book.id',
  `reason`      VARCHAR(255)     NOT NULL                COMMENT '举报原因',
  `status`      TINYINT UNSIGNED NOT NULL DEFAULT 0      COMMENT '0-待处理 1-已下架 2-已驳回',
  `handler_id`  BIGINT UNSIGNED  DEFAULT NULL            COMMENT '处理人ID -> user.id',
  `handled_at`  DATETIME         DEFAULT NULL            COMMENT '处理时间',
  `created_at`  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '举报时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_reporter_book_status` (`reporter_id`, `book_id`, `status`),
  KEY `idx_status` (`status`),
  KEY `idx_book` (`book_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '举报表';

-- ------------------------------------------------------------
-- 8. 评价表：订单完成后买家评价卖家，并据此调整卖家信用分
--    rating: 1-5 星；同一订单同一买家只能评价一次
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `review` (
  `id`          BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '评价ID',
  `order_id`    BIGINT UNSIGNED  NOT NULL                COMMENT '订单ID -> orders.id',
  `book_id`     BIGINT UNSIGNED  NOT NULL                COMMENT '书籍ID -> book.id',
  `reviewer_id` BIGINT UNSIGNED  NOT NULL                COMMENT '评价人（买家）-> user.id',
  `reviewee_id` BIGINT UNSIGNED  NOT NULL                COMMENT '被评价人（卖家）-> user.id',
  `rating`      TINYINT UNSIGNED NOT NULL                COMMENT '评分 1-5 星',
  `content`     VARCHAR(500)     DEFAULT NULL            COMMENT '评价内容',
  `created_at`  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评价时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_reviewer` (`order_id`, `reviewer_id`),
  KEY `idx_reviewee` (`reviewee_id`),
  KEY `idx_book` (`book_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '评价表';
