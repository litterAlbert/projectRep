CREATE DATABASE IF NOT EXISTS `smart_library` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `smart_library`;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
  `password` VARCHAR(255) NOT NULL COMMENT '密码',
  `gender` VARCHAR(10) DEFAULT NULL COMMENT '性别',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '电话',
  `role` VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT '角色: admin, user',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='用户表';

-- 图书表
CREATE TABLE IF NOT EXISTS `book` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `isbn` VARCHAR(20) NOT NULL UNIQUE COMMENT 'ISBN',
  `title` VARCHAR(100) NOT NULL COMMENT '书名',
  `author` VARCHAR(100) NOT NULL COMMENT '作者',
  `publisher` VARCHAR(100) DEFAULT NULL COMMENT '出版社',
  `publish_date` DATE DEFAULT NULL COMMENT '出版日期',
  `stock` INT NOT NULL DEFAULT 0 COMMENT '库存',
  `borrowed_count` INT NOT NULL DEFAULT 0 COMMENT '借出数量',
  `reserved_count` INT NOT NULL DEFAULT 0 COMMENT '预约数量',
  `location` VARCHAR(100) DEFAULT NULL COMMENT '馆藏位置',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='图书表';

-- 借阅记录表
CREATE TABLE IF NOT EXISTS `borrow_record` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `book_id` BIGINT NOT NULL COMMENT '图书ID',
  `borrow_date` DATETIME NOT NULL COMMENT '借阅日期',
  `return_date` DATETIME DEFAULT NULL COMMENT '归还日期',
  `status` VARCHAR(20) NOT NULL DEFAULT 'BORROWED' COMMENT '状态: BORROWED, RETURNED',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
  FOREIGN KEY (`book_id`) REFERENCES `book`(`id`)
) ENGINE=InnoDB COMMENT='借阅记录表';

-- 预约记录表
CREATE TABLE IF NOT EXISTS `reserve_record` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `book_id` BIGINT NOT NULL COMMENT '图书ID',
  `reserve_date` DATETIME NOT NULL COMMENT '预约日期',
  `status` VARCHAR(20) NOT NULL DEFAULT 'RESERVED' COMMENT '状态: RESERVED, FULFILLED, CANCELLED',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
  FOREIGN KEY (`book_id`) REFERENCES `book`(`id`)
) ENGINE=InnoDB COMMENT='预约记录表';

-- AI 会话表
CREATE TABLE IF NOT EXISTS `chat_session` (
  `id` VARCHAR(36) PRIMARY KEY COMMENT '会话ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `title` VARCHAR(100) NOT NULL COMMENT '会话标题',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB COMMENT='AI会话表';

-- AI 消息记录表
CREATE TABLE IF NOT EXISTS `chat_message` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `session_id` VARCHAR(36) NOT NULL COMMENT '会话ID',
  `role` VARCHAR(20) NOT NULL COMMENT '角色: user, ai',
  `content` TEXT NOT NULL COMMENT '消息内容',
  `source` TEXT DEFAULT NULL COMMENT '引用来源(JSON)',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`session_id`) REFERENCES `chat_session`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='AI消息记录表';

-- 插入测试数据

-- 密码使用明文 123456 进行测试（实际项目中应加密，由于未指定加密框架暂使用明文或简单MD5，这里先使用明文便于测试）
INSERT INTO `user` (`username`, `password`, `gender`, `phone`, `role`) VALUES
('admin', '123456', '男', '13800000000', 'admin'),
('user1', '123456', '女', '13800000001', 'user'),
('user2', '123456', '男', '13800000002', 'user'),
('user3', '123456', '女', '13800000003', 'user'),
('user4', '123456', '男', '13800000004', 'user'),
('user5', '123456', '女', '13800000005', 'user'),
('user6', '123456', '男', '13800000006', 'user'),
('user7', '123456', '女', '13800000007', 'user'),
('user8', '123456', '男', '13800000008', 'user'),
('user9', '123456', '女', '13800000009', 'user'),
('user10', '123456', '男', '13800000010', 'user');

INSERT INTO `book` (`isbn`, `title`, `author`, `publisher`, `publish_date`, `stock`, `borrowed_count`, `reserved_count`, `location`) VALUES
('9787111128069', 'C程序设计语言', 'Brian W.Kernighan', '机械工业出版社', '2004-01-01', 5, 2, 0, 'A区1架1层'),
('9787111213826', 'Java编程思想', 'Bruce Eckel', '机械工业出版社', '2007-09-01', 3, 5, 1, 'A区1架2层'),
('9787115249494', '算法（第4版）', 'Robert Sedgewick', '人民邮电出版社', '2012-10-01', 8, 1, 0, 'A区2架1层'),
('9787302423287', '机器学习', '周志华', '清华大学出版社', '2016-01-01', 10, 8, 2, 'B区1架1层'),
('9787111532644', '深度学习', 'Ian Goodfellow', '人民邮电出版社', '2017-08-01', 2, 6, 3, 'B区1架2层'),
('9787121289248', 'Python编程从入门到实践', 'Eric Matthes', '人民邮电出版社', '2016-07-01', 15, 10, 0, 'A区3架1层'),
('9787111583264', '重构：改善既有代码的设计', 'Martin Fowler', '人民邮电出版社', '2019-05-01', 4, 3, 0, 'A区2架2层'),
('9787111421900', '深入理解计算机系统', 'Randal E.Bryant', '机械工业出版社', '2016-11-01', 6, 4, 1, 'C区1架1层'),
('9787115352521', '设计模式', 'Erich Gamma', '机械工业出版社', '2000-09-01', 7, 2, 0, 'A区2架3层'),
('9787111631767', 'Effective Java', 'Bruce Eckel', '机械工业出版社', '2019-10-01', 5, 1, 0, 'A区1架3层'),
('9787302431268', '计算机网络', '谢希仁', '电子工业出版社', '2017-01-01', 12, 5, 0, 'C区2架1层');

-- 插入借阅记录
INSERT INTO `borrow_record` (`user_id`, `book_id`, `borrow_date`, `return_date`, `status`) VALUES
(2, 1, '2026-02-01 10:00:00', '2026-02-15 10:00:00', 'RETURNED'),
(3, 2, '2026-03-01 10:00:00', NULL, 'BORROWED'),
(4, 4, '2026-03-05 14:00:00', NULL, 'BORROWED'),
(5, 4, '2026-03-10 09:30:00', '2026-03-20 09:30:00', 'RETURNED'),
(6, 5, '2026-03-15 11:20:00', NULL, 'BORROWED'),
(7, 6, '2026-03-20 16:45:00', NULL, 'BORROWED'),
(8, 7, '2026-03-22 10:10:00', NULL, 'BORROWED'),
(9, 8, '2026-03-25 13:00:00', NULL, 'BORROWED'),
(10, 9, '2026-03-26 15:30:00', NULL, 'BORROWED'),
(2, 10, '2026-03-27 08:00:00', NULL, 'BORROWED');

-- 插入预约记录
INSERT INTO `reserve_record` (`user_id`, `book_id`, `reserve_date`, `status`) VALUES
(3, 5, '2026-03-20 10:00:00', 'RESERVED'),
(4, 2, '2026-03-21 11:00:00', 'RESERVED'),
(5, 4, '2026-03-22 12:00:00', 'RESERVED'),
(6, 8, '2026-03-23 14:00:00', 'RESERVED'),
(7, 5, '2026-03-24 15:00:00', 'RESERVED'),
(8, 5, '2026-03-25 16:00:00', 'RESERVED'),
(9, 4, '2026-03-26 09:00:00', 'RESERVED'),
(10, 2, '2026-03-27 10:00:00', 'RESERVED'),
(2, 8, '2026-03-28 11:00:00', 'RESERVED'),
(3, 4, '2026-03-28 12:00:00', 'RESERVED');
