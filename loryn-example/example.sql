-- 用户表（单表操作主要对象）
CREATE TABLE users
(
    id         INT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username   VARCHAR(50) NOT NULL COMMENT '用户名',
    created_at DATETIME    NOT NULL COMMENT '注册时间',
    last_login DATETIME COMMENT '最后登录时间'
) COMMENT '用户信息表';

-- 商品表（单表操作+一对多关联）
CREATE TABLE products
(
    id           INT PRIMARY KEY AUTO_INCREMENT COMMENT '商品ID',
    product_name VARCHAR(100)   NOT NULL COMMENT '商品名称',
    price        DECIMAL(10, 2) NOT NULL COMMENT '销售价格',
    stock        INT            NOT NULL COMMENT '库存数量'
) COMMENT '商品信息表';

-- 订单表（多表关联枢纽）
CREATE TABLE orders
(
    id         INT PRIMARY KEY AUTO_INCREMENT COMMENT '订单ID',
    user_id    INT         NOT NULL COMMENT '关联用户ID',
    product_id INT         NOT NULL COMMENT '关联商品ID',
    quantity   INT         NOT NULL COMMENT '购买数量',
    order_time DATETIME    NOT NULL COMMENT '下单时间',
    status     VARCHAR(20) NOT NULL COMMENT '订单状态'
) COMMENT '订单记录表';

-- 模拟数据
INSERT INTO users (id, username, created_at, last_login)
VALUES (101, '林若曦', '2024-03-01 09:00:00', '2024-04-15 09:15:00'),
       (102, '沈墨白', '2024-02-15 14:30:00', '2024-04-14 18:20:00'),
       (103, '陆清歌', '2024-01-10 10:00:00', '2024-04-13 16:45:00');

INSERT INTO products (product_name, price, stock)
VALUES ('无线降噪耳机', 899.00, 50),
       ('智能手表Pro', 1299.00, 30),
       ('便携充电宝', 199.00, 100);

INSERT INTO orders (user_id, product_id, quantity, order_time, status)
VALUES (101, 201, 1, '2024-04-11 11:00:00', 'completed'),
       (101, 203, 2, '2024-04-13 15:45:00', 'cancelled'),
       (102, 201, 2, '2024-04-10 14:30:00', 'completed'),
       (102, 202, 1, '2024-04-12 09:15:00', 'completed'),
       (102, 203, 3, '2024-04-14 16:20:00', 'pending'),
       (103, 202, 1, '2024-04-09 17:30:00', 'completed');
