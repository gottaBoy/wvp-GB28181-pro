# WVP后端集成说明

本文档说明如何使用相机推流系统与WVP后端集成，实现车辆和相机的集中管理。

## 架构概览

```
┌─────────────────┐
│   WVP 后端      │
│  (Java服务)     │
└────────┬────────┘
         │ HTTP API
         │ (注册/心跳/查询)
         │
         │ HTTP API
         │ (订阅/取消订阅指令)
         │
┌────────▼──────────────────────────────────────────┐
│              车辆端 (ROS2)                        │
│                                                   │
│  ┌──────────────────────────────┐                │
│  │ camera_info_reporter         │                │
│  │ (定期上报车辆和相机信息)      │                │
│  └──────────────────────────────┘                │
│                                                   │
│  ┌──────────────────────────────┐                │
│  │ camera_stream_http_api       │                │
│  │ (接收WVP控制指令)            │                │
│  └────────────┬─────────────────┘                │
│               │ ROS2 Service                     │
│  ┌────────────▼─────────────────┐                │
│  │ dynamic_camera_streamer      │                │
│  │ (动态订阅管理)               │                │
│  └────────────┬─────────────────┘                │
│               │                                  │
│  ┌────────────▼─────────────────┐                │
│  │ zlmediakit_streamer          │                │
│  │ (推流节点，每个相机一个)      │                │
│  └──────────────────────────────┘                │
└───────────────────────────────────────────────────┘
```

## 核心组件

### 1. camera_info_reporter.py
**功能**：定期向WVP后端上报车辆信息和相机列表

**特性**：
- 车辆自动注册
- 定期上报相机列表和状态
- 心跳机制保持连接
- 自动获取本机IP地址

**API端点**（上报到WVP后端）：
- `POST /api/vehicle/register` - 注册车辆
- `POST /api/vehicle/{vehicle_id}/heartbeat` - 发送心跳
- `PUT /api/vehicle/{vehicle_id}/cameras` - 上报相机列表

### 2. camera_stream_http_api.py
**功能**：提供HTTP API接口供WVP后端调用，接收订阅/取消订阅指令

**特性**：
- RESTful API设计
- 支持API密钥认证
- CORS支持（跨域）
- 转发指令到ROS2 Service

**API端点**（车辆端提供）：
- `GET /health` - 健康检查
- `POST /api/camera/subscribe` - 订阅相机
- `POST /api/camera/unsubscribe` - 取消订阅相机
- `GET /api/camera/list` - 列出已订阅相机

### 3. dynamic_camera_streamer.py
**功能**：动态订阅管理器，提供ROS2 Service接口

**ROS2 Service**：
- `camera_stream/subscribe` - 订阅相机
- `camera_stream/unsubscribe` - 取消订阅相机
- `camera_stream/list` - 列出已订阅相机

## 配置

### 1. WVP后端配置 (config/wvp_config.yaml)

```yaml
# WVP后端API配置
wvp:
   api_base_url: "http://localhost:18080"
   api_key: ""  # 可选
   enable_auto_register: true
   report_interval: 30.0  # 上报间隔（秒）
   heartbeat_interval: 10.0  # 心跳间隔（秒）

# HTTP API服务配置
http_api:
   host: "0.0.0.0"
   port: 8080
   api_key: ""  # 可选
   enable_cors: true

# 车辆信息配置
vehicle:
   vehicle_id: "DP003"
   vehicle_name: ""
```

### 2. 相机配置 (config/zlmediakit_streaming.yaml)

相机配置中的topic支持两种格式：
- 新格式：`/zeron/driver/camera/cam_b_18/image_raw/compressed`
- 旧格式：`/zeron/driver/camera/cam_b_18/raw_image/compressed`

系统会自动检测并使用可用的格式。

## 使用方法

### 1. 启动完整系统

```bash
ros2 launch camera_stream camera_stream_with_wvp.launch.py \
  vehicle_no:=DP003 \
  wvp_api_base_url:=http://wvp-server:18080 \
  http_host:=0.0.0.0 \
  http_port:=8080
```

### 2. WVP后端API调用示例

#### 订阅相机
```bash
curl -X POST http://vehicle-ip:8080/api/camera/subscribe \
  -H "Content-Type: application/json" \
  -d '{
    "camera_ids": ["cam_b_18", "cam_f_2"]
  }'

# 订阅全部相机
curl -X POST http://vehicle-ip:8080/api/camera/subscribe \
  -H "Content-Type: application/json" \
  -d '{
    "camera_ids": ["all"]
  }'
```

#### 取消订阅相机
```bash
curl -X POST http://vehicle-ip:8080/api/camera/unsubscribe \
  -H "Content-Type: application/json" \
  -d '{
    "camera_ids": ["cam_b_18"]
  }'
```

#### 查询已订阅相机
```bash
curl http://vehicle-ip:8080/api/camera/list
```

### 3. WVP后端数据格式

#### 车辆注册数据格式
```json
{
   "vehicle_id": "DP003",
   "vehicle_name": "车辆003",
   "ip_address": "192.168.1.100",
   "status": "online",
   "last_heartbeat": "2024-01-01T12:00:00",
   "cameras": [
      {
         "camera_id": "cam_b_18",
         "name": "back_camera_18",
         "description": "后方摄像头18",
         "topic_compressed": "/zeron/driver/camera/cam_b_18/image_raw/compressed",
         "topic_raw": "/zeron/driver/camera/cam_b_18/image_raw",
         "enabled": true,
         "fps": 10,
         "bitrate": 2000000,
         "width": 1920,
         "height": 1080,
         "quality": "high",
         "status": "inactive"
      }
   ]
}
```

#### 心跳数据格式
```json
{
   "vehicle_id": "DP003",
   "ip_address": "192.168.1.100",
   "status": "online",
   "last_heartbeat": "2024-01-01T12:00:00"
}
```

#### 相机列表更新数据格式
```json
{
   "vehicle_id": "DP003",
   "cameras": [
      {
         "camera_id": "cam_b_18",
         "name": "back_camera_18",
         "status": "active",
         ...
      }
   ],
   "last_update": "2024-01-01T12:00:00"
}
```

## 工作流程

### 1. 系统启动流程
1. 启动`dynamic_camera_streamer` - 动态订阅管理器
2. 启动`camera_info_reporter` - 自动注册车辆到WVP后端
3. 启动`camera_stream_http_api` - 启动HTTP API服务
4. 定期上报相机信息（默认30秒间隔）
5. 定期发送心跳（默认10秒间隔）

### 2. 订阅流程
1. WVP后端调用 `POST /api/camera/subscribe`
2. HTTP API服务接收请求
3. 调用ROS2 Service `camera_stream/subscribe`
4. 动态订阅管理器启动对应的`zlmediakit_streamer`进程
5. 推流开始，相机状态更新为"active"
6. 下次上报时，WVP后端会看到相机状态变化

### 3. 取消订阅流程
1. WVP后端调用 `POST /api/camera/unsubscribe`
2. HTTP API服务接收请求
3. 调用ROS2 Service `camera_stream/unsubscribe`
4. 动态订阅管理器停止对应的推流进程
5. 相机状态更新为"inactive"
6. 下次上报时，WVP后端会看到相机状态变化

## 安全配置

### API密钥认证

如果启用了API密钥，需要在请求头中包含：

```bash
curl -X POST http://vehicle-ip:8080/api/camera/subscribe \
  -H "Authorization: Bearer your_api_key" \
  -H "Content-Type: application/json" \
  -d '{...}'
```

在launch文件中配置：
```python
wvp_api_key:="your_wvp_api_key" \
http_api_key:="your_http_api_key"
```

## 故障排查

### 1. 车辆未在WVP后端显示
- 检查`wvp_api_base_url`配置是否正确
- 检查网络连接
- 查看`camera_info_reporter`日志

### 2. 订阅失败
- 检查`dynamic_camera_streamer`是否运行
- 检查相机ID是否正确
- 查看ROS2 Service状态：`ros2 service list | grep camera_stream`

### 3. HTTP API无响应
- 检查`camera_stream_http_api`是否运行
- 检查端口是否被占用
- 查看日志输出

## 开发说明

### WVP后端需要实现的API

#### 1. 车辆注册接口
```
POST /api/vehicle/register
Content-Type: application/json

Body: 车辆信息JSON（见上方数据格式）
```

#### 2. 心跳接口
```
POST /api/vehicle/{vehicle_id}/heartbeat
Content-Type: application/json

Body: 心跳数据JSON（见上方数据格式）
```

#### 3. 相机列表更新接口
```
PUT /api/vehicle/{vehicle_id}/cameras
Content-Type: application/json

Body: 相机列表更新数据JSON（见上方数据格式）
```

### 扩展开发

如需扩展功能，可以：
1. 在`camera_info_reporter.py`中添加更多上报信息
2. 在`camera_stream_http_api.py`中添加更多HTTP端点
3. 在Service定义中添加更多服务接口

## 注意事项

1. **防重复订阅**：默认不允许重复订阅同一topic，可通过`allow_duplicate_subscription`参数控制
2. **Topic格式**：系统会自动检测`/compressed`和`/image_raw`两种格式
3. **进程管理**：推流进程异常退出时，系统会自动清理
4. **网络延迟**：上报间隔和心跳间隔可根据网络情况调整

