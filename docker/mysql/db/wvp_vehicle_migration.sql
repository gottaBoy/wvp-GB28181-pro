-- 车辆表迁移脚本：添加 remark 字段
-- 如果表已存在但缺少 remark 字段，执行此脚本添加
-- 
-- 使用方法：
-- 1. 如果使用 MySQL 8.0.19+，可以直接执行第一个 ALTER TABLE 语句
-- 2. 如果使用较旧版本的 MySQL，需要先检查字段是否存在，然后执行相应的语句

-- 方式1：MySQL 8.0.19+ 支持 IF NOT EXISTS
-- ALTER TABLE wvp_vehicle 
-- ADD COLUMN IF NOT EXISTS remark VARCHAR(500) COMMENT '备注' AFTER vehicle_name;

-- 方式2：兼容所有 MySQL 版本（推荐）
-- 先检查字段是否存在，如果不存在则添加
-- 可以通过以下 SQL 检查：SHOW COLUMNS FROM wvp_vehicle LIKE 'remark';
-- 如果查询结果为空，则执行下面的 ALTER TABLE 语句

ALTER TABLE wvp_vehicle 
ADD COLUMN remark VARCHAR(500) COMMENT '备注' AFTER vehicle_name;

-- 注意：如果 remark 字段已存在，上面的语句会报错，这是正常的
-- 如果报错 "Duplicate column name 'remark'"，说明字段已存在，可以忽略

