CREATE DATABASE IF NOT EXISTS store 
 DEFAULT CHARACTER SET utf8mb4 
 COLLATE utf8mb4_unicode_ci; 

USE store; 

CREATE TABLE IF NOT EXISTS user ( 
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID', 
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名', 
    password VARCHAR(100) NOT NULL COMMENT '密码', 
    phone VARCHAR(20) UNIQUE COMMENT '手机号', 
    email VARCHAR(100) UNIQUE COMMENT '邮箱', 
    status TINYINT DEFAULT 1 COMMENT '状态 1正常 0禁用', 
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP, 
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表'; 


CREATE TABLE IF NOT EXISTS product ( 
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '商品ID', 
    name VARCHAR(100) NOT NULL COMMENT '商品名称', 
    price DECIMAL(10,2) NOT NULL COMMENT '价格', 
    description TEXT COMMENT '商品描述', 
    status TINYINT DEFAULT 1 COMMENT '状态 1上架 0下架', 
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP, 
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品表'; 


CREATE TABLE IF NOT EXISTS inventory ( 
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '库存ID', 
    product_id BIGINT NOT NULL COMMENT '商品ID', 
    stock INT NOT NULL DEFAULT 0 COMMENT '库存数量', 
    lock_stock INT NOT NULL DEFAULT 0 COMMENT '锁定库存', 
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP, 
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, 
    UNIQUE KEY uk_product_id (product_id), 
    CONSTRAINT fk_inventory_product FOREIGN KEY (product_id) REFERENCES product(id) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存表'; 

CREATE TABLE IF NOT EXISTS orders ( 
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '订单ID', 
    order_no VARCHAR(32) NOT NULL COMMENT '订单编号',
    user_id BIGINT NOT NULL COMMENT '用户ID', 
    total_amount DECIMAL(10,2) NOT NULL COMMENT '订单总金额', 
    status TINYINT DEFAULT 0 COMMENT '订单状态 0待支付 1已支付 2已取消', 
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP, 
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, 
    INDEX idx_user_id (user_id), 
    UNIQUE KEY uk_order_no (order_no), 
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES user(id) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表'; 

CREATE TABLE IF NOT EXISTS order_item ( 
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '订单项ID', 
    order_id BIGINT NOT NULL COMMENT '订单ID', 
    product_id BIGINT NOT NULL COMMENT '商品ID', 
    quantity INT NOT NULL COMMENT '数量', 
    price DECIMAL(10,2) NOT NULL COMMENT '单价', 
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP, 
    INDEX idx_order_id (order_id), 
    INDEX idx_product_id (product_id), 
    CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE, 
    CONSTRAINT fk_order_item_product FOREIGN KEY (product_id) REFERENCES product(id) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单项表';