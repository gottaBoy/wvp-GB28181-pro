-- 车辆信息表
DROP TABLE IF EXISTS wvp_vehicle;
CREATE TABLE IF NOT EXISTS wvp_vehicle
(
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    vehicle_id          VARCHAR(50) NOT NULL,  -- 编码
    ip_address          VARCHAR(50),           -- IP地址
    status              VARCHAR(20) DEFAULT 'offline',  -- 状态：online/offline
    vehicle_name        VARCHAR(255),          -- 名称
    remark              VARCHAR(500),         -- 备注
    last_heartbeat      VARCHAR(50),          -- 最后心跳时间
    register_time       VARCHAR(50),          -- 注册时间
    create_time         VARCHAR(50) NOT NULL,  -- 创建时间
    update_time         VARCHAR(50) NOT NULL,  -- 更新时间
    CONSTRAINT uk_vehicle_vehicle_id UNIQUE (vehicle_id)
);

CREATE INDEX idx_vehicle_id ON wvp_vehicle(vehicle_id);
CREATE INDEX idx_vehicle_status ON wvp_vehicle(status);
CREATE INDEX idx_vehicle_last_heartbeat ON wvp_vehicle(last_heartbeat);

-- 车辆相机信息表
DROP TABLE IF EXISTS wvp_vehicle_camera;
CREATE TABLE IF NOT EXISTS wvp_vehicle_camera
(
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    vehicle_id          VARCHAR(50) NOT NULL,  -- 应用（车编码）
    camera_id           VARCHAR(50) NOT NULL,  -- 名称（推流ID）
    pushing             BOOLEAN DEFAULT FALSE,          -- 推流状态：是否正在推流
    topic               VARCHAR(500),          -- 流topic（原始流或压缩流）
    stream_push_id      INTEGER,                         -- 关联的推流ID（wvp_stream_push表，通过app+stream唯一查询，可能为空）
    push_time           VARCHAR(50),           -- 推流开始时间
    name                VARCHAR(255),         -- 相机名称
    description         VARCHAR(500),          -- 描述
    enabled             BOOLEAN DEFAULT TRUE,            -- 是否启用
    status              VARCHAR(20) DEFAULT 'inactive',  -- 状态：active/inactive
    fps                 INTEGER,                          -- 帧率
    bitrate             INTEGER,                         -- 比特率
    width               INTEGER,                          -- 宽度
    height              INTEGER,                          -- 高度
    quality             VARCHAR(20),           -- 质量
    create_time         VARCHAR(50) NOT NULL,  -- 创建时间
    update_time         VARCHAR(50) NOT NULL,  -- 更新时间
    CONSTRAINT uk_vehicle_camera UNIQUE (vehicle_id, camera_id)
);

CREATE INDEX idx_vehicle_camera_vehicle_id ON wvp_vehicle_camera(vehicle_id);
CREATE INDEX idx_vehicle_camera_camera_id ON wvp_vehicle_camera(camera_id);
CREATE INDEX idx_vehicle_camera_status ON wvp_vehicle_camera(status);
CREATE INDEX idx_vehicle_camera_stream_push_id ON wvp_vehicle_camera(stream_push_id);

