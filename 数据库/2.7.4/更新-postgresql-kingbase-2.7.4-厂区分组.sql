-- 厂区分组功能 - PostgreSQL/KingBase版本
-- 创建时间: 2024-12-03

-- 厂区分组表
DROP TABLE IF EXISTS wvp_factory_group;
CREATE TABLE IF NOT EXISTS wvp_factory_group
(
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    app         VARCHAR(255) NOT NULL,
    sort_order  INTEGER DEFAULT 0,
    create_time VARCHAR(50),
    update_time VARCHAR(50),
    CONSTRAINT uk_factory_group_app_name UNIQUE (app, name)
);

COMMENT ON TABLE wvp_factory_group IS '厂区分组表';
COMMENT ON COLUMN wvp_factory_group.id IS '主键ID';
COMMENT ON COLUMN wvp_factory_group.name IS '分组名称，如：退洗库、黑皮库';
COMMENT ON COLUMN wvp_factory_group.description IS '分组描述';
COMMENT ON COLUMN wvp_factory_group.app IS '所属厂区应用名';
COMMENT ON COLUMN wvp_factory_group.sort_order IS '排序顺序';
COMMENT ON COLUMN wvp_factory_group.create_time IS '创建时间';
COMMENT ON COLUMN wvp_factory_group.update_time IS '更新时间';

-- 厂区拉流代理分组关联表
DROP TABLE IF EXISTS wvp_factory_group_proxy;
CREATE TABLE IF NOT EXISTS wvp_factory_group_proxy
(
    id         SERIAL PRIMARY KEY,
    group_id   INTEGER NOT NULL,
    app        VARCHAR(255) NOT NULL,
    stream     VARCHAR(255) NOT NULL,
    create_time VARCHAR(50),
    CONSTRAINT uk_factory_group_proxy_app_stream UNIQUE (app, stream),
    CONSTRAINT fk_factory_group_proxy_group FOREIGN KEY (group_id) REFERENCES wvp_factory_group(id) ON DELETE CASCADE
);

COMMENT ON TABLE wvp_factory_group_proxy IS '厂区拉流代理分组关联表';
COMMENT ON COLUMN wvp_factory_group_proxy.id IS '主键ID';
COMMENT ON COLUMN wvp_factory_group_proxy.group_id IS '分组ID';
COMMENT ON COLUMN wvp_factory_group_proxy.app IS '拉流代理应用名';
COMMENT ON COLUMN wvp_factory_group_proxy.stream IS '拉流代理流ID';
COMMENT ON COLUMN wvp_factory_group_proxy.create_time IS '创建时间';

-- 创建索引
CREATE INDEX idx_factory_group_app ON wvp_factory_group(app);
CREATE INDEX idx_factory_group_proxy_group_id ON wvp_factory_group_proxy(group_id);
CREATE INDEX idx_factory_group_proxy_app ON wvp_factory_group_proxy(app);

