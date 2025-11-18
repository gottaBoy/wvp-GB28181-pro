package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.Vehicle;
import com.genersoft.iot.vmp.gb28181.bean.VehicleCamera;
import com.genersoft.iot.vmp.gb28181.bean.dto.VehicleCamerasUpdateDTO;
import com.genersoft.iot.vmp.gb28181.bean.dto.VehicleCameraDTO;
import com.genersoft.iot.vmp.gb28181.bean.dto.VehicleHeartbeatDTO;
import com.genersoft.iot.vmp.gb28181.bean.dto.VehicleRegisterDTO;
import com.genersoft.iot.vmp.gb28181.bean.dto.VehicleUpdateDTO;
import com.genersoft.iot.vmp.gb28181.dao.VehicleMapper;
import com.genersoft.iot.vmp.gb28181.service.IVehicleHttpClientService;
import com.genersoft.iot.vmp.gb28181.service.IVehicleService;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.streamPush.bean.StreamPush;
import com.genersoft.iot.vmp.streamPush.service.IStreamPushPlayService;
import com.genersoft.iot.vmp.streamPush.service.IStreamPushService;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.common.StreamInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 车辆服务实现类
 * @author auto-generated
 */
@Slf4j
@Service
public class VehicleServiceImpl implements IVehicleService {

    @Autowired
    private VehicleMapper vehicleMapper;

    @Autowired
    private IStreamPushService streamPushService;

    @Autowired
    private IStreamPushPlayService streamPushPlayService;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private IVehicleHttpClientService vehicleHttpClientService;

    private static final int DEFAULT_HTTP_API_PORT = 8081;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Vehicle registerVehicle(VehicleRegisterDTO registerDTO) {
        log.info("[车辆注册] vehicleId: {}, vehicleName: {}, ipAddress: {}", 
                registerDTO.getVehicleId(), registerDTO.getVehicleName(), registerDTO.getIpAddress());

        String currentTime = DateUtil.getNow();
        Vehicle existingVehicle = vehicleMapper.getVehicleByVehicleId(registerDTO.getVehicleId());

        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleId(registerDTO.getVehicleId());
        vehicle.setVehicleName(registerDTO.getVehicleName());
        vehicle.setIpAddress(registerDTO.getIpAddress());
        
        log.debug("[车辆注册] 设置的IP地址: {}", vehicle.getIpAddress());
        vehicle.setStatus(StringUtils.hasText(registerDTO.getStatus()) ? registerDTO.getStatus() : "online");
        vehicle.setRemark(registerDTO.getRemark());
        vehicle.setLastHeartbeat(StringUtils.hasText(registerDTO.getLastHeartbeat()) 
                ? registerDTO.getLastHeartbeat() : currentTime);
        vehicle.setRegisterTime(currentTime);
        vehicle.setCreateTime(currentTime);
        vehicle.setUpdateTime(currentTime);

        if (existingVehicle == null) {
            // 新注册车辆
            vehicleMapper.insertVehicle(vehicle);
            log.info("[车辆注册] 新车辆注册成功: {}", registerDTO.getVehicleId());
        } else {
            // 更新已存在车辆，但不允许修改IP地址（IP地址只能通过心跳上报更新）
            log.info("[车辆注册] 车辆已存在，只更新基本信息（保留原IP地址）: {}", registerDTO.getVehicleId());
            vehicleMapper.updateVehicleBasicInfo(
                    registerDTO.getVehicleId(),
                    registerDTO.getVehicleName(),
                    vehicle.getStatus(),
                    registerDTO.getRemark(),
                    currentTime
            );
            log.info("[车辆注册] 车辆基本信息更新成功，IP地址保持不变: {}", registerDTO.getVehicleId());
        }

        // 如果有相机信息，同时更新相机列表
        if (registerDTO.getCameras() != null && !registerDTO.getCameras().isEmpty()) {
            updateVehicleCameras(registerDTO.getVehicleId(), registerDTO.getCameras());
        }

        return vehicleMapper.getVehicleByVehicleId(registerDTO.getVehicleId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateHeartbeat(String vehicleId, VehicleHeartbeatDTO heartbeatDTO) {
        log.debug("[车辆心跳] vehicleId: {}", vehicleId);

        String currentTime = DateUtil.getNow();
        // 收到心跳时，强制设置状态为在线
        String status = "online";
        String lastHeartbeat = StringUtils.hasText(heartbeatDTO.getLastHeartbeat()) 
                ? heartbeatDTO.getLastHeartbeat() : currentTime;

        // 先检查车辆是否存在以及当前状态
        Vehicle existingVehicle = vehicleMapper.getVehicleByVehicleId(vehicleId);
        
        String ipAddress;
        if (existingVehicle != null) {
            // 车辆已存在，只有当传入的IP地址不为空时才更新IP地址
            if (StringUtils.hasText(heartbeatDTO.getIpAddress())) {
                ipAddress = heartbeatDTO.getIpAddress();
                log.debug("[车辆心跳] 更新IP地址: vehicleId={}, 新IP={}, 原IP={}", 
                         vehicleId, ipAddress, existingVehicle.getIpAddress());
            } else {
                // 保持原有IP地址不变
                ipAddress = existingVehicle.getIpAddress();
                log.debug("[车辆心跳] 保持原IP地址: vehicleId={}, IP={}", vehicleId, ipAddress);
            }
        } else {
            // 新车辆，使用传入的IP地址或空字符串
            ipAddress = StringUtils.hasText(heartbeatDTO.getIpAddress()) 
                    ? heartbeatDTO.getIpAddress() : "";
            log.debug("[车辆心跳] 新车辆IP地址: vehicleId={}, IP={}", vehicleId, ipAddress);
        }
        
        int result = vehicleMapper.updateVehicleHeartbeat(vehicleId, ipAddress, status, lastHeartbeat, currentTime);
        
        // 如果车辆之前是离线状态，记录上线日志
        if (existingVehicle != null && "offline".equals(existingVehicle.getStatus())) {
            log.info("[车辆心跳] 车辆重新上线: vehicleId={}, ipAddress={}", vehicleId, ipAddress);
        }
        
        if (result > 0) {
            log.debug("[车辆心跳] 更新成功: {}", vehicleId);
            return true;
        } else {
            // 车辆不存在，自动创建车辆
            log.info("[车辆心跳] 车辆不存在，自动创建车辆: {}", vehicleId);
            
            Vehicle newVehicle = new Vehicle();
            newVehicle.setVehicleId(vehicleId);
            newVehicle.setVehicleName(vehicleId);
            newVehicle.setIpAddress(ipAddress);
            newVehicle.setStatus(status);
            newVehicle.setLastHeartbeat(lastHeartbeat);
            newVehicle.setRegisterTime(currentTime);
            newVehicle.setCreateTime(currentTime);
            newVehicle.setUpdateTime(currentTime);
            
            try {
                vehicleMapper.insertVehicle(newVehicle);
                log.info("[车辆心跳] 自动创建车辆成功: {}", vehicleId);
                return true;
            } catch (Exception e) {
                log.error("[车辆心跳] 自动创建车辆失败: {}", vehicleId, e);
                return false;
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateCameras(String vehicleId, VehicleCamerasUpdateDTO camerasUpdateDTO) {
        log.info("[更新相机列表] vehicleId: {}, cameraCount: {}", 
                vehicleId, 
                camerasUpdateDTO.getCameras() != null ? camerasUpdateDTO.getCameras().size() : 0);

        String currentTime = DateUtil.getNow();
        
        // 验证车辆是否存在，如果不存在则自动创建
        Vehicle vehicle = vehicleMapper.getVehicleByVehicleId(vehicleId);
        if (vehicle == null) {
            log.info("[更新相机列表] 车辆不存在，自动创建车辆: {}", vehicleId);
            Vehicle newVehicle = new Vehicle();
            newVehicle.setVehicleId(vehicleId);
            newVehicle.setVehicleName("自动创建-" + vehicleId);
            // 获取客户端IP地址，如果无法获取则设为空
            String clientIp = getClientIpFromRequest();
            newVehicle.setIpAddress(clientIp != null ? clientIp : "");
            log.info("[更新相机列表] 自动创建车辆使用IP: {}", clientIp);
            newVehicle.setStatus("online");
            newVehicle.setLastHeartbeat(currentTime);
            newVehicle.setRegisterTime(currentTime);
            newVehicle.setCreateTime(currentTime);
            newVehicle.setUpdateTime(currentTime);
            
            try {
                vehicleMapper.insertVehicle(newVehicle);
                vehicle = vehicleMapper.getVehicleByVehicleId(vehicleId);
                log.info("[更新相机列表] 自动创建车辆成功: {}", vehicleId);
            } catch (Exception e) {
                log.error("[更新相机列表] 自动创建车辆失败: {}", vehicleId, e);
                return false;
            }
        }

        updateVehicleCameras(vehicleId, camerasUpdateDTO.getCameras());

        // 只更新车辆的更新时间，不更新其他信息（特别是IP地址）
        vehicleMapper.updateVehicleUpdateTime(vehicleId, currentTime);

        return true;
    }

    /**
     * 更新车辆相机列表
     */
    private void updateVehicleCameras(String vehicleId, List<VehicleCameraDTO> cameraDTOs) {
        if (cameraDTOs == null || cameraDTOs.isEmpty()) {
            return;
        }

        String currentTime = DateUtil.getNow();
        List<VehicleCamera> existingCameras = vehicleMapper.getCamerasByVehicleId(vehicleId);

        // 将现有相机转换为Map，便于查找
        java.util.Map<String, VehicleCamera> existingCameraMap = existingCameras.stream()
                .collect(Collectors.toMap(VehicleCamera::getCameraId, camera -> camera));

        // 处理每个相机
        for (VehicleCameraDTO cameraDTO : cameraDTOs) {
            // 添加调试日志检查相机数据
            log.debug("[更新相机列表] 处理相机: cameraId={}, name={}, status={}", 
                    cameraDTO.getCameraId(), cameraDTO.getName(), cameraDTO.getStatus());
            
            // 检查相机ID是否为空
            if (cameraDTO.getCameraId() == null || cameraDTO.getCameraId().trim().isEmpty()) {
                log.warn("[更新相机列表] 跳过相机ID为空的相机: name={}, topic={}", 
                        cameraDTO.getName(), cameraDTO.getTopic());
                continue;
            }
            
            VehicleCamera camera = existingCameraMap.get(cameraDTO.getCameraId());

            VehicleCamera newCamera = new VehicleCamera();
            newCamera.setVehicleId(vehicleId);
            newCamera.setCameraId(cameraDTO.getCameraId());
            newCamera.setName(cameraDTO.getName());
            newCamera.setDescription(cameraDTO.getDescription());
            newCamera.setTopic(cameraDTO.getTopic());
            newCamera.setEnabled(cameraDTO.getEnabled() != null ? cameraDTO.getEnabled() : true);
            newCamera.setFps(cameraDTO.getFps());
            newCamera.setBitrate(cameraDTO.getBitrate());
            newCamera.setWidth(cameraDTO.getWidth());
            newCamera.setHeight(cameraDTO.getHeight());
            newCamera.setQuality(cameraDTO.getQuality());
            newCamera.setStatus(StringUtils.hasText(cameraDTO.getStatus()) 
                    ? cameraDTO.getStatus() : "inactive");
            newCamera.setUpdateTime(currentTime);

            if (camera == null) {
                // 新增相机
                newCamera.setCreateTime(currentTime);
                newCamera.setPushing(false);
                vehicleMapper.insertCamera(newCamera);
                log.debug("[更新相机列表] 新增相机: vehicleId={}, cameraId={}, dbId={}", 
                        vehicleId, cameraDTO.getCameraId(), newCamera.getId());
                
//                // 自动创建推流记录（在事务内，如果失败会回滚）
//                if (newCamera.getId() != null) {
//                    createStreamPushForCamera(vehicleId, cameraDTO.getCameraId(), newCamera.getId());
//                } else {
//                    log.warn("[更新相机列表] 相机插入后ID为空，无法创建推流记录: vehicleId={}, cameraId={}",
//                            vehicleId, cameraDTO.getCameraId());
//                }
            } else {
                // 更新已存在相机
                newCamera.setId(camera.getId());
                newCamera.setCreateTime(camera.getCreateTime());
                newCamera.setStreamPushId(camera.getStreamPushId());
                newCamera.setPushing(camera.getPushing() != null ? camera.getPushing() : false);
                newCamera.setPushTime(camera.getPushTime());
                vehicleMapper.updateCamera(newCamera);
                log.debug("[更新相机列表] 更新相机: vehicleId={}, cameraId={}", vehicleId, cameraDTO.getCameraId());
                
//                // 如果还没有推流记录，创建推流记录
//                if (camera.getStreamPushId() == null) {
//                    createStreamPushForCamera(vehicleId, cameraDTO.getCameraId(), camera.getId());
//                }
            }
        }

        // 删除不在新列表中的相机（可选，根据业务需求决定）
        // 这里暂时不删除，保留历史相机记录
    }

    @Override
    public Vehicle getVehicleByVehicleId(String vehicleId) {
        return vehicleMapper.getVehicleByVehicleId(vehicleId);
    }

    @Override
    public List<VehicleCamera> getCamerasByVehicleId(String vehicleId) {
        return vehicleMapper.getCamerasByVehicleId(vehicleId);
    }

    @Override
    public List<Vehicle> getAllVehicles() {
        return vehicleMapper.getAllVehicles();
    }

    /**
     * 为相机创建推流记录
     * app = vehicle_id, stream = camera_id
     */
    private void createStreamPushForCamera(String vehicleId, String cameraId, Integer cameraDbId) {
        try {
            // 检查是否已存在推流记录
            StreamPush existingPush = streamPushService.getPush(vehicleId, cameraId);
            if (existingPush != null) {
                log.debug("[创建推流记录] 推流记录已存在: app={}, stream={}", vehicleId, cameraId);
                // 更新相机关联的推流ID（重新查询确保获取最新数据）
                List<VehicleCamera> cameras = vehicleMapper.getCamerasByVehicleId(vehicleId);
                VehicleCamera camera = cameras.stream()
                        .filter(c -> c.getCameraId().equals(cameraId))
                        .findFirst()
                        .orElse(null);
                if (camera != null && camera.getStreamPushId() == null) {
                    camera.setStreamPushId(existingPush.getId());
                    vehicleMapper.updateCamera(camera);
                    log.debug("[创建推流记录] 已关联已存在的推流ID: vehicleId={}, cameraId={}, streamPushId={}", 
                            vehicleId, cameraId, existingPush.getId());
                }
                return;
            }

            // 创建新的推流记录
            StreamPush streamPush = new StreamPush();
            streamPush.setApp(vehicleId);  // 车辆ID作为应用名
            streamPush.setStream(cameraId);  // 相机ID作为流ID
            streamPush.setGbDeviceId(vehicleId + "_" + cameraId);  // 国标设备ID
            streamPush.setGbName(vehicleId + "-" + cameraId);  // 国标名称
            
            // 设置默认媒体服务器
            try {
                MediaServer defaultMediaServer = mediaServerService.getDefaultMediaServer();
                String mediaServerId = defaultMediaServer != null 
                        ? defaultMediaServer.getId() 
                        : "auto";
                streamPush.setMediaServerId(mediaServerId);
            } catch (Exception e) {
                log.warn("[创建推流记录] 获取默认媒体服务器失败，使用auto: {}", e.getMessage());
                streamPush.setMediaServerId("auto");
            }
            
            if (userSetting != null && userSetting.getServerId() != null) {
                streamPush.setServerId(userSetting.getServerId());
            } else {
                log.warn("[创建推流记录] userSetting或serverId为空");
                streamPush.setServerId("default");
            }
            
            streamPush.setPushing(false);
            streamPush.setStartOfflinePush(true);  // 允许离线推流
            streamPush.setCreateTime(DateUtil.getNow());
            streamPush.setUpdateTime(DateUtil.getNow());

            if (streamPushService.add(streamPush)) {
                log.info("[创建推流记录] 成功: app={}, stream={}, pushId={}", 
                        vehicleId, cameraId, streamPush.getId());
                
                // 更新相机关联的推流ID（重新查询确保获取最新数据）
                List<VehicleCamera> cameras = vehicleMapper.getCamerasByVehicleId(vehicleId);
                VehicleCamera camera = cameras.stream()
                        .filter(c -> c.getCameraId().equals(cameraId))
                        .findFirst()
                        .orElse(null);
                if (camera != null) {
                    camera.setStreamPushId(streamPush.getId());
                    vehicleMapper.updateCamera(camera);
                    log.debug("[创建推流记录] 已关联推流ID: vehicleId={}, cameraId={}, streamPushId={}", 
                            vehicleId, cameraId, streamPush.getId());
                } else {
                    log.warn("[创建推流记录] 未找到相机，无法关联推流ID: vehicleId={}, cameraId={}", 
                            vehicleId, cameraId);
                }
            } else {
                log.warn("[创建推流记录] 添加推流记录失败: app={}, stream={}", vehicleId, cameraId);
            }
        } catch (Exception e) {
            log.error("[创建推流记录] 异常: app={}, stream={}", vehicleId, cameraId, e);
        }
    }

    /**
     * 启动相机推流
     */
    public boolean startCameraStream(String vehicleId, String cameraId) {
        log.info("[启动推流] 开始处理: vehicleId={}, cameraId={}", vehicleId, cameraId);
        
        // 检查车辆是否存在
        Vehicle vehicle = vehicleMapper.getVehicleByVehicleId(vehicleId);
        if (vehicle == null) {
            log.error("[启动推流] 车辆不存在: vehicleId={}", vehicleId);
            return false;
        }
        
        List<VehicleCamera> cameras = vehicleMapper.getCamerasByVehicleId(vehicleId);
        log.debug("[启动推流] 找到相机数量: {}", cameras.size());
        
        VehicleCamera camera = cameras.stream()
                .filter(c -> c.getCameraId().equals(cameraId))
                .findFirst()
                .orElse(null);
        
        if (camera == null) {
            log.error("[启动推流] 相机不存在: vehicleId={}, cameraId={}", vehicleId, cameraId);
            return false;
        }
        
        log.info("[启动推流] 找到相机: vehicleId={}, cameraId={}, cameraDbId={}", 
                vehicleId, cameraId, camera.getId());

        // 确保推流记录存在（如果不存在则创建）
        StreamPush streamPush = streamPushService.getPush(vehicleId, cameraId);
        if (streamPush == null) {
            log.info("[启动推流] 推流记录不存在，先创建: vehicleId={}, cameraId={}", vehicleId, cameraId);
            createStreamPushForCamera(vehicleId, cameraId, camera.getId());
            // 重新查询推流记录
            streamPush = streamPushService.getPush(vehicleId, cameraId);
            if (streamPush == null) {
                log.error("[启动推流] 创建推流记录失败: vehicleId={}, cameraId={}", vehicleId, cameraId);
                return false;
            }
        }
        
        log.info("[启动推流] 使用推流记录: pushId={}, app={}, stream={}", 
                streamPush.getId(), streamPush.getApp(), streamPush.getStream());

        try {
            // 调用推流服务启动推流（使用推流ID）
            log.info("[启动推流] 调用推流服务: pushId={}", streamPush.getId());
            streamPushPlayService.start(streamPush.getId(), (code, msg, streamInfo) -> {
                String currentTime = DateUtil.getNow();
                log.info("[启动推流] 回调结果: code={}, msg={}, streamInfo={}", code, msg, streamInfo);
                if (code == 0 && streamInfo != null) {
                    // 推流成功，更新相机状态
                    vehicleMapper.updateCameraPushStatus(vehicleId, cameraId, true, "active", currentTime, currentTime);
                    log.info("[启动推流] 成功并更新状态: vehicleId={}, cameraId={}", vehicleId, cameraId);
                } else {
                    // 推流失败
                    vehicleMapper.updateCameraPushStatus(vehicleId, cameraId, false, "inactive", null, currentTime);
                    log.warn("[启动推流] 失败并更新状态: vehicleId={}, cameraId={}, code={}, msg={}", 
                            vehicleId, cameraId, code, msg);
                }
            }, null, null);
            log.info("[启动推流] 推流服务调用成功，等待异步回调");
            return true;
        } catch (Exception e) {
            log.error("[启动推流] 推流服务调用异常: vehicleId={}, cameraId={}", vehicleId, cameraId, e);
            return false;
        }
    }

    /**
     * 停止相机推流
     */
    public boolean stopCameraStream(String vehicleId, String cameraId) {
        log.info("[停止推流] vehicleId={}, cameraId={}", vehicleId, cameraId);
        
        VehicleCamera camera = vehicleMapper.getCamerasByVehicleId(vehicleId).stream()
                .filter(c -> c.getCameraId().equals(cameraId))
                .findFirst()
                .orElse(null);
        
        if (camera == null) {
            log.warn("[停止推流] 相机不存在: vehicleId={}, cameraId={}", vehicleId, cameraId);
            return false;
        }

        try {
            // 调用推流服务停止推流（使用app+stream，不依赖stream_push_id）
            streamPushPlayService.stop(vehicleId, cameraId);
            
            // 注意：推流停止是异步的，实际状态会通过MediaDepartureEvent事件监听自动更新
            // 这里先更新状态，如果推流服务失败，事件监听会再次更新
            String currentTime = DateUtil.getNow();
            vehicleMapper.updateCameraPushStatus(vehicleId, cameraId, false, "inactive", null, currentTime);
            log.info("[停止推流] 已发送停止指令: vehicleId={}, cameraId={}", vehicleId, cameraId);
            return true;
        } catch (Exception e) {
            log.error("[停止推流] 异常: vehicleId={}, cameraId={}", vehicleId, cameraId, e);
            return false;
        }
    }

    @Override
    public boolean subscribeVehicleCameras(String vehicleId, List<String> cameraIds) {
        log.info("[车辆相机订阅] 开始处理订阅请求: vehicleId={}, cameraIds={}", vehicleId, cameraIds);
        
        if (!StringUtils.hasText(vehicleId) || cameraIds == null || cameraIds.isEmpty()) {
            log.warn("[车辆相机订阅] 参数无效: vehicleId={}, cameraIds={}", vehicleId, cameraIds);
            return false;
        }

        Vehicle vehicle = vehicleMapper.getVehicleByVehicleId(vehicleId);
        if (vehicle == null) {
            log.error("[车辆相机订阅] 车辆不存在: vehicleId={}", vehicleId);
            return false;
        }

        String ipAddress = vehicle.getIpAddress();
        if (!StringUtils.hasText(ipAddress)) {
            log.error("[车辆相机订阅] 车辆IP地址为空: vehicleId={}, 车辆信息: {}", vehicleId, vehicle);
            return false;
        }

        log.info("[车辆相机订阅] 准备调用车端API:");
        log.info("  - 车辆ID: {}", vehicleId);
        log.info("  - 车辆IP地址: {}", ipAddress);
        log.info("  - API端口: {}", DEFAULT_HTTP_API_PORT);
        log.info("  - 相机ID列表: {}", cameraIds);
        log.info("  - 请求URL: http://{}:{}/api/cameras/subscribe", ipAddress, DEFAULT_HTTP_API_PORT);

        try {
            log.info("[车辆相机订阅] 开始调用车辆端HTTP API");
            
            // 调用车辆端HTTP API
            boolean success = vehicleHttpClientService.subscribeCameras(
                ipAddress, 
                DEFAULT_HTTP_API_PORT, 
                cameraIds, 
                null // 暂时不使用API密钥，后续可从配置获取
            );
            
            log.info("[车辆相机订阅] 车端API调用完成: success={}", success);

            if (success) {
                // 为每个相机创建推流记录并更新状态
                String currentTime = DateUtil.getNow();
                for (String cameraId : cameraIds) {
                    // 首先为相机创建推流记录
                    VehicleCamera camera = vehicleMapper.getCamerasByVehicleId(vehicleId).stream()
                            .filter(c -> c.getCameraId().equals(cameraId))
                            .findFirst()
                            .orElse(null);
                    
                    if (camera != null) {
                        createStreamPushForCamera(vehicleId, cameraId, camera.getId());
                        // 更新相机状态为active
                        vehicleMapper.updateCameraPushStatus(vehicleId, cameraId, true, "active", currentTime, currentTime);
                    }
                }
                log.info("[车辆相机订阅] 订阅成功: vehicleId={}, cameraIds={}", vehicleId, cameraIds);
            } else {
                log.warn("[车辆相机订阅] 订阅失败: vehicleId={}, cameraIds={}", vehicleId, cameraIds);
            }

            return success;
        } catch (Exception e) {
            log.error("[车辆相机订阅] 异常: vehicleId={}, cameraIds={}", vehicleId, cameraIds, e);
            return false;
        }
    }

    @Override
    public boolean unsubscribeVehicleCameras(String vehicleId, List<String> cameraIds) {
        if (!StringUtils.hasText(vehicleId) || cameraIds == null || cameraIds.isEmpty()) {
            log.warn("[车辆相机取消订阅] 参数无效: vehicleId={}, cameraIds={}", vehicleId, cameraIds);
            return false;
        }

        Vehicle vehicle = vehicleMapper.getVehicleByVehicleId(vehicleId);
        if (vehicle == null) {
            log.warn("[车辆相机取消订阅] 车辆不存在: vehicleId={}", vehicleId);
            return false;
        }

        String ipAddress = vehicle.getIpAddress();
        if (!StringUtils.hasText(ipAddress)) {
            log.warn("[车辆相机取消订阅] 车辆IP地址为空: vehicleId={}", vehicleId);
            return false;
        }

        try {
            // 调用车辆端HTTP API
            boolean success = vehicleHttpClientService.unsubscribeCameras(
                ipAddress, 
                DEFAULT_HTTP_API_PORT, 
                cameraIds, 
                null // 暂时不使用API密钥，后续可从配置获取
            );

            if (success) {
                // 停止推流并更新相机状态为inactive
                String currentTime = DateUtil.getNow();
                for (String cameraId : cameraIds) {
                    // 尝试停止推流
                    try {
                        streamPushPlayService.stop(vehicleId, cameraId);
                        log.debug("[车辆相机取消订阅] 已停止推流: vehicleId={}, cameraId={}", vehicleId, cameraId);
                    } catch (Exception e) {
                        log.warn("[车辆相机取消订阅] 停止推流失败: vehicleId={}, cameraId={}", vehicleId, cameraId, e);
                    }
                    
                    // 更新相机状态为inactive
                    vehicleMapper.updateCameraPushStatus(vehicleId, cameraId, false, "inactive", null, currentTime);
                }
                log.info("[车辆相机取消订阅] 取消订阅成功: vehicleId={}, cameraIds={}", vehicleId, cameraIds);
            } else {
                log.warn("[车辆相机取消订阅] 取消订阅失败: vehicleId={}, cameraIds={}", vehicleId, cameraIds);
            }

            return success;
        } catch (Exception e) {
            log.error("[车辆相机取消订阅] 异常: vehicleId={}, cameraIds={}", vehicleId, cameraIds, e);
            return false;
        }
    }

    @Override
    public List<String> getVehicleSubscribedCameras(String vehicleId) {
        if (!StringUtils.hasText(vehicleId)) {
            log.warn("[获取车辆订阅列表] 参数无效: vehicleId={}", vehicleId);
            return Collections.emptyList();
        }

        Vehicle vehicle = vehicleMapper.getVehicleByVehicleId(vehicleId);
        if (vehicle == null) {
            log.warn("[获取车辆订阅列表] 车辆不存在: vehicleId={}", vehicleId);
            return Collections.emptyList();
        }

        String ipAddress = vehicle.getIpAddress();
        if (!StringUtils.hasText(ipAddress)) {
            log.warn("[获取车辆订阅列表] 车辆IP地址为空: vehicleId={}", vehicleId);
            return Collections.emptyList();
        }

        try {
            // 调用车辆端HTTP API
            List<String> subscribedCameras = vehicleHttpClientService.getSubscribedCameras(
                ipAddress, 
                DEFAULT_HTTP_API_PORT, 
                null // 暂时不使用API密钥，后续可从配置获取
            );

            log.debug("[获取车辆订阅列表] 成功: vehicleId={}, cameras={}", vehicleId, subscribedCameras);
            return subscribedCameras;
        } catch (Exception e) {
            log.error("[获取车辆订阅列表] 异常: vehicleId={}", vehicleId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public String getVehicleCameraWebRTCUrl(String vehicleId, String cameraId) {
        log.debug("[获取WebRTC播放链接] vehicleId={}, cameraId={}", vehicleId, cameraId);
        
        try {
            // 根据车辆ID和相机ID查找对应的推流记录
            // 推流记录中：app = vehicleId, stream = cameraId
            StreamPush streamPush = streamPushService.getPush(vehicleId, cameraId);
            if (streamPush == null) {
                log.warn("[获取WebRTC播放链接] 未找到推流记录: vehicleId={}, cameraId={}", vehicleId, cameraId);
                return null;
            }
            
            // 检查推流是否在线
            if (!streamPush.isPushing()) {
                log.warn("[获取WebRTC播放链接] 推流未启动: vehicleId={}, cameraId={}, pushing={}", 
                        vehicleId, cameraId, streamPush.isPushing());
                return null;
            }
            
            // 获取媒体服务器
            MediaServer mediaServer = mediaServerService.getOne(streamPush.getMediaServerId());
            if (mediaServer == null) {
                log.warn("[获取WebRTC播放链接] 未找到媒体服务器: mediaServerId={}", streamPush.getMediaServerId());
                return null;
            }
            
            // 获取流信息以生成WebRTC播放链接
            StreamInfo streamInfo = mediaServerService.getStreamInfoByAppAndStream(
                mediaServer, 
                vehicleId,  // app 
                cameraId,   // stream
                null,       // mediaInfo
                null        // callId
            );
            
            if (streamInfo == null) {
                log.warn("[获取WebRTC播放链接] 未找到流信息: vehicleId={}, cameraId={}", vehicleId, cameraId);
                return null;
            }
            
            // 返回WebRTC播放链接
            if (streamInfo.getRtc() != null) {
                String webrtcUrl = streamInfo.getRtc().getUrl();
                log.info("[获取WebRTC播放链接] 成功: vehicleId={}, cameraId={}, url={}", 
                        vehicleId, cameraId, webrtcUrl);
                return webrtcUrl;
            } else if (streamInfo.getRtcs() != null) {
                String webrtcUrl = streamInfo.getRtcs().getUrl();
                log.info("[获取WebRTC播放链接] 成功(HTTPS): vehicleId={}, cameraId={}, url={}", 
                        vehicleId, cameraId, webrtcUrl);
                return webrtcUrl;
            } else {
                log.warn("[获取WebRTC播放链接] 流信息中无WebRTC链接: vehicleId={}, cameraId={}", vehicleId, cameraId);
                return null;
            }
            
        } catch (Exception e) {
            log.error("[获取WebRTC播放链接] 异常: vehicleId={}, cameraId={}", vehicleId, cameraId, e);
            return null;
        }
    }

    @Override
    public boolean checkVehicleConnection(String vehicleId) {
        if (!StringUtils.hasText(vehicleId)) {
            return false;
        }

        Vehicle vehicle = vehicleMapper.getVehicleByVehicleId(vehicleId);
        if (vehicle == null) {
            return false;
        }

        String ipAddress = vehicle.getIpAddress();
        if (!StringUtils.hasText(ipAddress)) {
            return false;
        }

        try {
            // 调用车辆端HTTP API健康检查
            return vehicleHttpClientService.checkHealth(
                ipAddress, 
                DEFAULT_HTTP_API_PORT, 
                null // 暂时不使用API密钥，后续可从配置获取
            );
        } catch (Exception e) {
            log.debug("[车辆连接检查] 异常: vehicleId={}", vehicleId, e);
            return false;
        }
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIpFromRequest() {
        try {
            // 尝试从RequestContextHolder获取当前请求
            RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
            if (attrs instanceof ServletRequestAttributes) {
                HttpServletRequest request = ((ServletRequestAttributes) attrs).getRequest();
                return getClientIpAddress(request);
            }
            return null;
        } catch (Exception e) {
            log.debug("[获取客户端IP] 异常: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从HTTP请求中提取客户端真实IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headers = {
            "X-Forwarded-For",
            "X-Real-IP", 
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // 多级代理的情况，取第一个IP
                if (ip.contains(",")) {
                    ip = ip.substring(0, ip.indexOf(",")).trim();
                }
                if (isValidIpAddress(ip)) {
                    return ip;
                }
            }
        }

        // 最后尝试getRemoteAddr()
        String ip = request.getRemoteAddr();
        return isValidIpAddress(ip) ? ip : null;
    }

    /**
     * 简单的IP地址格式验证
     */
    private boolean isValidIpAddress(String ip) {
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            return false;
        }
        
        // IPv4格式基本检查
        if (ip.matches("^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$")) {
            return true;
        }
        
        // IPv6格式基本检查（简化版）
        if (ip.contains(":") && ip.length() <= 45) {
            return true;
        }
        
        return false;
    }

    @Override
    public boolean directStartCameraStream(String vehicleId, String cameraId) {
        log.info("[直接启动推流] vehicleId={}, cameraId={}", vehicleId, cameraId);

        if (!StringUtils.hasText(vehicleId) || !StringUtils.hasText(cameraId)) {
            log.warn("[直接启动推流] 参数为空: vehicleId={}, cameraId={}", vehicleId, cameraId);
            return false;
        }

        Vehicle vehicle = vehicleMapper.getVehicleByVehicleId(vehicleId);
        if (vehicle == null) {
            log.warn("[直接启动推流] 车辆不存在: vehicleId={}", vehicleId);
            return false;
        }

        String ipAddress = vehicle.getIpAddress();
        if (!StringUtils.hasText(ipAddress)) {
            log.warn("[直接启动推流] 车辆IP地址为空: vehicleId={}", vehicleId);
            return false;
        }

        try {
            // 直接调用车辆端HTTP API启动推流 (http://ipaddress:8081/api/camera/subscribe)
            boolean success = vehicleHttpClientService.directSubscribeCamera(
                ipAddress, 
                8081, 
                cameraId,
                vehicleId,
                null // 暂时不使用API密钥
            );

            if (success) {
                log.info("[直接启动推流] 成功: vehicleId={}, cameraId={}, ipAddress={}", vehicleId, cameraId, ipAddress);
                
                // 可选：更新本地相机状态为推流中（如果需要同步状态）
                String currentTime = DateUtil.getNow();
                vehicleMapper.updateCameraPushStatus(vehicleId, cameraId, true, "active", currentTime, currentTime);
                
                return true;
            } else {
                log.warn("[直接启动推流] 失败: vehicleId={}, cameraId={}, ipAddress={}", vehicleId, cameraId, ipAddress);
                return false;
            }
        } catch (Exception e) {
            log.error("[直接启动推流] 异常: vehicleId={}, cameraId={}, ipAddress={}", vehicleId, cameraId, ipAddress, e);
            return false;
        }
    }

    @Override
    public boolean directStopCameraStream(String vehicleId, String cameraId) {
        log.info("[直接停止推流] vehicleId={}, cameraId={}", vehicleId, cameraId);

        if (!StringUtils.hasText(vehicleId) || !StringUtils.hasText(cameraId)) {
            log.warn("[直接停止推流] 参数为空: vehicleId={}, cameraId={}", vehicleId, cameraId);
            return false;
        }

        Vehicle vehicle = vehicleMapper.getVehicleByVehicleId(vehicleId);
        if (vehicle == null) {
            log.warn("[直接停止推流] 车辆不存在: vehicleId={}", vehicleId);
            return false;
        }

        String ipAddress = vehicle.getIpAddress();
        if (!StringUtils.hasText(ipAddress)) {
            log.warn("[直接停止推流] 车辆IP地址为空: vehicleId={}", vehicleId);
            return false;
        }

        try {
            // 直接调用车辆端HTTP API停止推流 (http://ipaddress:8081/api/camera/unsubscribe)
            boolean success = vehicleHttpClientService.directUnsubscribeCamera(
                ipAddress, 
                8081, 
                cameraId,
                vehicleId,
                null // 暂时不使用API密钥
            );

            if (success) {
                log.info("[直接停止推流] 成功: vehicleId={}, cameraId={}, ipAddress={}", vehicleId, cameraId, ipAddress);
                
                // 可选：更新本地相机状态为停止推流（如果需要同步状态）
                String currentTime = DateUtil.getNow();
                vehicleMapper.updateCameraPushStatus(vehicleId, cameraId, false, "inactive", null, currentTime);
                
                return true;
            } else {
                log.warn("[直接停止推流] 失败: vehicleId={}, cameraId={}, ipAddress={}", vehicleId, cameraId, ipAddress);
                return false;
            }
        } catch (Exception e) {
            log.error("[直接停止推流] 异常: vehicleId={}, cameraId={}, ipAddress={}", vehicleId, cameraId, ipAddress, e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int processHeartbeatTimeout(int timeoutSeconds) {
        log.debug("[车辆心跳检测] 开始检查心跳超时车辆，超时时间: {}秒", timeoutSeconds);
        
        try {
            // 查询心跳超时的在线车辆
            List<Vehicle> timeoutVehicles = vehicleMapper.getHeartbeatTimeoutVehicles(timeoutSeconds);
            
            if (timeoutVehicles == null || timeoutVehicles.isEmpty()) {
                log.debug("[车辆心跳检测] 没有发现心跳超时的车辆");
                return 0;
            }
            
            String currentTime = DateUtil.getNow();
            int processedCount = 0;
            
            // 逐个处理超时车辆
            for (Vehicle vehicle : timeoutVehicles) {
                try {
                    int result = vehicleMapper.updateVehicleStatus(vehicle.getVehicleId(), "offline", currentTime);
                    if (result > 0) {
                        log.info("[车辆心跳检测] 车辆状态已更新为离线: vehicleId={}, lastHeartbeat={}", 
                                vehicle.getVehicleId(), vehicle.getLastHeartbeat());
                        processedCount++;
                    }
                } catch (Exception e) {
                    log.error("[车辆心跳检测] 更新车辆状态失败: vehicleId={}", vehicle.getVehicleId(), e);
                }
            }
            
            log.info("[车辆心跳检测] 处理完成，共处理 {} 个超时车辆", processedCount);
            return processedCount;
            
        } catch (Exception e) {
            log.error("[车辆心跳检测] 检查心跳超时异常", e);
            return 0;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateVehicleInfo(String vehicleId, VehicleUpdateDTO updateDTO) {
        log.info("[更新车辆信息] vehicleId={}, vehicleName={}, ipAddress={}, status={}", 
                vehicleId, updateDTO.getVehicleName(), updateDTO.getIpAddress(), updateDTO.getStatus());

        // 检查车辆是否存在
        Vehicle existingVehicle = vehicleMapper.getVehicleByVehicleId(vehicleId);
        if (existingVehicle == null) {
            log.warn("[更新车辆信息] 车辆不存在: {}", vehicleId);
            return false;
        }

        String currentTime = DateUtil.getNow();
        
        // 准备更新参数
        String vehicleName = StringUtils.hasText(updateDTO.getVehicleName()) 
                ? updateDTO.getVehicleName() : existingVehicle.getVehicleName();
        String status = StringUtils.hasText(updateDTO.getStatus()) 
                ? updateDTO.getStatus() : existingVehicle.getStatus();
        String remark = updateDTO.getRemark() != null 
                ? updateDTO.getRemark() : existingVehicle.getRemark();

        try {
            // 检查是否需要更新IP地址
            if (StringUtils.hasText(updateDTO.getIpAddress())) {
                // IP地址不为空，更新包括IP地址在内的所有信息
                log.info("[更新车辆信息] 更新IP地址: vehicleId={}, 原IP={}, 新IP={}", 
                        vehicleId, existingVehicle.getIpAddress(), updateDTO.getIpAddress());
                        
                int result = vehicleMapper.updateVehicleHeartbeat(
                        vehicleId, 
                        updateDTO.getIpAddress(), 
                        status, 
                        existingVehicle.getLastHeartbeat(), 
                        currentTime);
                        
                if (result > 0) {
                    // 如果IP地址更新成功，还需要更新其他基本信息
                    vehicleMapper.updateVehicleBasicInfo(vehicleId, vehicleName, status, remark, currentTime);
                }
                return result > 0;
            } else {
                // IP地址为空，只更新基本信息，不修改IP地址
                log.info("[更新车辆信息] 不更新IP地址，仅更新基本信息: vehicleId={}", vehicleId);
                int result = vehicleMapper.updateVehicleBasicInfo(vehicleId, vehicleName, status, remark, currentTime);
                return result > 0;
            }
        } catch (Exception e) {
            log.error("[更新车辆信息] 更新异常: vehicleId={}", vehicleId, e);
            return false;
        }
    }
}

