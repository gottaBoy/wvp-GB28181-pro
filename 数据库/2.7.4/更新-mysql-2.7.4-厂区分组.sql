-- 厂区分组功能 - MySQL版本
-- 创建时间: 2024-12-03

-- 厂区分组表
DROP TABLE IF EXISTS wvp_factory_group;
CREATE TABLE IF NOT EXISTS wvp_factory_group
(
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(255) NOT NULL COMMENT '分组名称，如：退洗库、黑皮库',
    description VARCHAR(500) COMMENT '分组描述',
    app         VARCHAR(255) NOT NULL COMMENT '所属厂区应用名',
    sort_order  INT DEFAULT 0 COMMENT '排序顺序',
    create_time VARCHAR(50) COMMENT '创建时间',
    update_time VARCHAR(50) COMMENT '更新时间',
    CONSTRAINT uk_factory_group_app_name UNIQUE (app, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='厂区分组表';

-- 厂区拉流代理分组关联表
DROP TABLE IF EXISTS wvp_factory_group_proxy;
CREATE TABLE IF NOT EXISTS wvp_factory_group_proxy
(
    id         INT AUTO_INCREMENT PRIMARY KEY,
    group_id   INT NOT NULL COMMENT '分组ID',
    app        VARCHAR(255) NOT NULL COMMENT '拉流代理应用名',
    stream     VARCHAR(255) NOT NULL COMMENT '拉流代理流ID',
    create_time VARCHAR(50) COMMENT '创建时间',
    CONSTRAINT uk_factory_group_proxy_app_stream UNIQUE (app, stream),
    CONSTRAINT fk_factory_group_proxy_group FOREIGN KEY (group_id) REFERENCES wvp_factory_group(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='厂区拉流代理分组关联表';

-- 创建索引
CREATE INDEX idx_factory_group_app ON wvp_factory_group(app);
CREATE INDEX idx_factory_group_proxy_group_id ON wvp_factory_group_proxy(group_id);
CREATE INDEX idx_factory_group_proxy_app ON wvp_factory_group_proxy(app);

