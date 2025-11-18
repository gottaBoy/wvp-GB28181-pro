package com.genersoft.iot.vmp.gb28181.controller;

import com.genersoft.iot.vmp.gb28181.bean.Vehicle;
import com.genersoft.iot.vmp.gb28181.bean.VehicleCamera;
import com.genersoft.iot.vmp.gb28181.bean.dto.VehicleCameraDTO;
import com.genersoft.iot.vmp.gb28181.bean.dto.VehicleCamerasUpdateDTO;
import com.genersoft.iot.vmp.gb28181.bean.dto.VehicleHeartbeatDTO;
import com.genersoft.iot.vmp.gb28181.bean.dto.VehicleRegisterDTO;
import com.genersoft.iot.vmp.gb28181.service.IVehicleService;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import com.google.common.collect.Lists;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * 车辆管理控制器
 * @author auto-generated
 */
@Slf4j
@Tag(name = "车辆管理")
@RestController
@RequestMapping("/api/vehicle")
public class VehicleController {

    @Autowired
    private IVehicleService vehicleService;

    @Operation(summary = "车辆注册", description = "车辆向WVP后端注册，包含车辆基本信息和相机列表")
    @PostMapping("/register")
    public WVPResult<Vehicle> registerVehicle(@RequestBody VehicleRegisterDTO registerDTO) {
        log.info("[车辆注册] 收到注册请求: vehicleId={}, vehicleName={}, ipAddress={}", 
                registerDTO.getVehicleId(), registerDTO.getVehicleName(), registerDTO.getIpAddress());
        
        // 详细调试信息
        log.debug("[车辆注册] 详细数据: vehicleId='{}', vehicleName='{}', ipAddress='{}', status='{}', cameras={}",
                registerDTO.getVehicleId(), 
                registerDTO.getVehicleName(),
                registerDTO.getIpAddress(),
                registerDTO.getStatus(),
                registerDTO.getCameras() != null ? registerDTO.getCameras().size() : "null");

        if (!StringUtils.hasText(registerDTO.getVehicleId())) {
            log.warn("[车辆注册] 车辆ID为空或null: '{}'", registerDTO.getVehicleId());
            return WVPResult.fail(ErrorCode.ERROR400.getCode(), "车辆ID不能为空");
        }

        if (!StringUtils.hasText(registerDTO.getIpAddress())) {
            return WVPResult.fail(ErrorCode.ERROR400.getCode(), "IP地址不能为空");
        }

        try {
            Vehicle vehicle = vehicleService.registerVehicle(registerDTO);
            return WVPResult.success(vehicle);
        } catch (Exception e) {
            log.error("[车辆注册] 注册失败: vehicleId={}", registerDTO.getVehicleId(), e);
            return WVPResult.fail(ErrorCode.ERROR500.getCode(), "车辆注册失败: " + e.getMessage());
        }
    }

    @Operation(summary = "车辆心跳", description = "车辆定期发送心跳信息，保持在线状态")
    @PostMapping("/{vehicleId}/heartbeat")
    public WVPResult<Void> heartbeat(
            @Parameter(description = "车辆ID", required = true) @PathVariable String vehicleId,
            @RequestBody VehicleHeartbeatDTO heartbeatDTO) {
        log.debug("[车辆心跳] 收到心跳: vehicleId={}", vehicleId);

        if (!StringUtils.hasText(vehicleId)) {
            return WVPResult.fail(ErrorCode.ERROR400.getCode(), "车辆ID不能为空");
        }

        // 确保vehicleId一致
        heartbeatDTO.setVehicleId(vehicleId);

        try {
            boolean success = vehicleService.updateHeartbeat(vehicleId, heartbeatDTO);
            if (success) {
                return WVPResult.<Void>success(null);
            } else {
                return WVPResult.fail(ErrorCode.ERROR404.getCode(), "车辆不存在");
            }
        } catch (Exception e) {
            log.error("[车辆心跳] 更新失败: vehicleId={}", vehicleId, e);
            return WVPResult.fail(ErrorCode.ERROR500.getCode(), "心跳更新失败: " + e.getMessage());
        }
    }

    @Operation(summary = "更新车辆相机列表", description = "更新指定车辆的所有相机信息")
    @PutMapping("/{vehicleId}/cameras")
    public WVPResult<Void> updateCameras(
            @Parameter(description = "车辆ID", required = true) @PathVariable String vehicleId,
            @RequestBody VehicleCamerasUpdateDTO camerasUpdateDTO) {
        log.info("[更新相机列表] 收到更新请求: vehicleId={}, cameraCount={}", 
                vehicleId, 
                camerasUpdateDTO.getCameras() != null ? camerasUpdateDTO.getCameras().size() : 0);

        // 详细打印相机数据用于调试
        if (camerasUpdateDTO.getCameras() != null) {
            for (int i = 0; i < Math.min(camerasUpdateDTO.getCameras().size(), 3); i++) {
                VehicleCameraDTO camera = camerasUpdateDTO.getCameras().get(i);
                log.debug("[更新相机列表] 相机数据[{}]: cameraId={}, name={}, topic={}, status={}", 
                        i, camera.getCameraId(), camera.getName(), camera.getTopic(), camera.getStatus());
            }
        }

        if (!StringUtils.hasText(vehicleId)) {
            return WVPResult.fail(ErrorCode.ERROR400.getCode(), "车辆ID不能为空");
        }

        if (camerasUpdateDTO.getCameras() == null) {
            return WVPResult.fail(ErrorCode.ERROR400.getCode(), "相机列表不能为空");
        }

        // 确保vehicleId一致
        camerasUpdateDTO.setVehicleId(vehicleId);

        try {
            boolean success = vehicleService.updateCameras(vehicleId, camerasUpdateDTO);
            if (success) {
                return WVPResult.<Void>success(null);
            } else {
                return WVPResult.fail(ErrorCode.ERROR404.getCode(), "车辆不存在");
            }
        } catch (Exception e) {
            log.error("[更新相机列表] 更新失败: vehicleId={}", vehicleId, e);
            return WVPResult.fail(ErrorCode.ERROR500.getCode(), "相机列表更新失败: " + e.getMessage());
        }
    }

    @Operation(summary = "查询车辆信息", description = "根据车辆ID查询车辆详细信息")
    @GetMapping("/{vehicleId}")
    public WVPResult<Vehicle> getVehicle(
            @Parameter(description = "车辆ID", required = true) @PathVariable String vehicleId) {
        log.debug("[查询车辆] vehicleId={}", vehicleId);

        if (!StringUtils.hasText(vehicleId)) {
            return WVPResult.fail(ErrorCode.ERROR400.getCode(), "车辆ID不能为空");
        }

        try {
            Vehicle vehicle = vehicleService.getVehicleByVehicleId(vehicleId);
            if (vehicle != null) {
                // 加载相机列表
                List<VehicleCamera> cameras = vehicleService.getCamerasByVehicleId(vehicleId);
                vehicle.setCameras(cameras);
                return WVPResult.success(vehicle);
            } else {
                return WVPResult.fail(ErrorCode.ERROR404.getCode(), "车辆不存在");
            }
        } catch (Exception e) {
            log.error("[查询车辆] 查询失败: vehicleId={}", vehicleId, e);
            return WVPResult.fail(ErrorCode.ERROR500.getCode(), "查询失败: " + e.getMessage());
        }
    }

    @Operation(summary = "查询车辆相机列表", description = "查询指定车辆的所有相机")
    @GetMapping("/{vehicleId}/cameras")
    public WVPResult<List<VehicleCamera>> getCameras(
            @Parameter(description = "车辆ID", required = true) @PathVariable String vehicleId) {
        log.debug("[查询相机列表] vehicleId={}", vehicleId);

        if (!StringUtils.hasText(vehicleId)) {
            return WVPResult.fail(ErrorCode.ERROR400.getCode(), "车辆ID不能为空");
        }

        try {
            List<VehicleCamera> cameras = vehicleService.getCamerasByVehicleId(vehicleId);
            return WVPResult.success(cameras);
        } catch (Exception e) {
            log.error("[查询相机列表] 查询失败: vehicleId={}", vehicleId, e);
            return WVPResult.fail(ErrorCode.ERROR500.getCode(), "查询失败: " + e.getMessage());
        }
    }

    @Operation(summary = "查询所有车辆", description = "查询所有已注册的车辆列表")
    @GetMapping("/list")
    public WVPResult<List<Vehicle>> getAllVehicles() {
        log.debug("[查询所有车辆]");

        try {
            List<Vehicle> vehicles = vehicleService.getAllVehicles();
            return WVPResult.success(vehicles);
        } catch (Exception e) {
            log.error("[查询所有车辆] 查询失败", e);
            return WVPResult.fail(ErrorCode.ERROR500.getCode(), "查询失败: " + e.getMessage());
        }
    }

    @Operation(summary = "启动相机推流", description = "启动指定车辆的相机推流")
    @PostMapping("/{vehicleId}/camera/{cameraId}/start")
    public WVPResult<Void> startCameraStream(
            @Parameter(description = "车辆ID", required = true) @PathVariable String vehicleId,
            @Parameter(description = "相机ID", required = true) @PathVariable String cameraId) {
        log.info("[启动推流] vehicleId={}, cameraId={}", vehicleId, cameraId);

        if (!StringUtils.hasText(vehicleId) || !StringUtils.hasText(cameraId)) {
            return WVPResult.fail(ErrorCode.ERROR400.getCode(), "车辆ID和相机ID不能为空");
        }

//        try {
//             boolean success = vehicleService.startCameraStream(vehicleId, cameraId);
//                return WVPResult.<Void>success(null);
//            } else {
//                return WVPResult.fail(ErrorCode.ERROR100.getCode(), "启动推流失败");
//            }
//        } catch (Exception e) {
//            log.error("[启动推流] 异常: vehicleId={}, cameraId={}", vehicleId, cameraId, e);
//            return WVPResult.fail(ErrorCode.ERROR500.getCode(), "启动推流异常: " + e.getMessage());
//        }
        List<String> cameraIds = new ArrayList<>();
        cameraIds.add(cameraId);
        return this.subscribeVehicleCameras(vehicleId, cameraIds);
    }

    @Operation(summary = "停止相机推流", description = "停止指定车辆的相机推流")
    @PostMapping("/{vehicleId}/camera/{cameraId}/stop")
    public WVPResult<Void> stopCameraStream(
            @Parameter(description = "车辆ID", required = true) @PathVariable String vehicleId,
            @Parameter(description = "相机ID", required = true) @PathVariable String cameraId) {
        log.info("[停止推流] vehicleId={}, cameraId={}", vehicleId, cameraId);

        if (!StringUtils.hasText(vehicleId) || !StringUtils.hasText(cameraId)) {
            return WVPResult.fail(ErrorCode.ERROR400.getCode(), "车辆ID和相机ID不能为空");
        }

//        try {
//            boolean success = vehicleService.stopCameraStream(vehicleId, cameraId);
//            if (success) {
//                return WVPResult.<Void>success(null);
//            } else {
//                return WVPResult.fail(ErrorCode.ERROR100.getCode(), "停止推流失败");
//            }
//        } catch (Exception e) {
//            log.error("[停止推流] 异常: vehicleId={}, cameraId={}", vehicleId, cameraId, e);
//            return WVPResult.fail(ErrorCode.ERROR500.getCode(), "停止推流异常: " + e.getMessage());
//        }
        List<String> cameraIds = new ArrayList<>();
        cameraIds.add(cameraId);
        return this.unsubscribeVehicleCameras(vehicleId, cameraIds);
    }

    @Operation(summary = "批量订阅相机", description = "向车辆端发送批量订阅相机指令")
    @PostMapping("/{vehicleId}/cameras/subscribe")
    public WVPResult<Void> subscribeVehicleCameras(
            @Parameter(description = "车辆ID", required = true) @PathVariable String vehicleId,
            @RequestBody List<String> cameraIds) {
        log.info("[批量订阅相机] vehicleId={}, cameraIds={}", vehicleId, cameraIds);

        if (!StringUtils.hasText(vehicleId) || cameraIds == null || cameraIds.isEmpty()) {
            return WVPResult.fail(ErrorCode.ERROR400.getCode(), "车辆ID和相机ID列表不能为空");
        }

        try {
            boolean success = vehicleService.subscribeVehicleCameras(vehicleId, cameraIds);
            if (success) {
                return WVPResult.<Void>success(null);
            } else {
                return WVPResult.fail(ErrorCode.ERROR100.getCode(), "订阅相机失败");
            }
        } catch (Exception e) {
            log.error("[批量订阅相机] 异常: vehicleId={}, cameraIds={}", vehicleId, cameraIds, e);
            return WVPResult.fail(ErrorCode.ERROR500.getCode(), "订阅相机异常: " + e.getMessage());
        }
    }

    @Operation(summary = "批量取消订阅相机", description = "向车辆端发送批量取消订阅相机指令")
    @PostMapping("/{vehicleId}/cameras/unsubscribe")
    public WVPResult<Void> unsubscribeVehicleCameras(
            @Parameter(description = "车辆ID", required = true) @PathVariable String vehicleId,
            @RequestBody List<String> cameraIds) {
        log.info("[批量取消订阅相机] vehicleId={}, cameraIds={}", vehicleId, cameraIds);

        if (!StringUtils.hasText(vehicleId) || cameraIds == null || cameraIds.isEmpty()) {
            return WVPResult.fail(ErrorCode.ERROR400.getCode(), "车辆ID和相机ID列表不能为空");
        }

        try {
            boolean success = vehicleService.unsubscribeVehicleCameras(vehicleId, cameraIds);
            if (success) {
                return WVPResult.<Void>success(null);
            } else {
                return WVPResult.fail(ErrorCode.ERROR100.getCode(), "取消订阅相机失败");
            }
        } catch (Exception e) {
            log.error("[批量取消订阅相机] 异常: vehicleId={}, cameraIds={}", vehicleId, cameraIds, e);
            return WVPResult.fail(ErrorCode.ERROR500.getCode(), "取消订阅相机异常: " + e.getMessage());
        }
    }

    @Operation(summary = "获取车辆已订阅相机", description = "从车辆端查询已订阅的相机列表")
    @GetMapping("/{vehicleId}/cameras/subscribed")
    public WVPResult<List<String>> getVehicleSubscribedCameras(
            @Parameter(description = "车辆ID", required = true) @PathVariable String vehicleId) {
        log.debug("[获取车辆订阅列表] vehicleId={}", vehicleId);

        if (!StringUtils.hasText(vehicleId)) {
            return WVPResult.fail(ErrorCode.ERROR400.getCode(), "车辆ID不能为空");
        }

        try {
            List<String> subscribedCameras = vehicleService.getVehicleSubscribedCameras(vehicleId);
            return WVPResult.success(subscribedCameras);
        } catch (Exception e) {
            log.error("[获取车辆订阅列表] 异常: vehicleId={}", vehicleId, e);
            return WVPResult.fail(ErrorCode.ERROR500.getCode(), "查询异常: " + e.getMessage());
        }
    }

    @Operation(summary = "获取单个相机WebRTC播放链接", description = "获取指定车辆单个相机的WebRTC播放链接")
    @GetMapping("/{vehicleId}/camera/{cameraId}/webrtc/play")
    public WVPResult<String> getSingleCameraWebRTCUrl(
            @Parameter(description = "车辆ID", required = true) @PathVariable String vehicleId,
            @Parameter(description = "相机ID", required = true) @PathVariable String cameraId) {
        log.info("[获取单个相机WebRTC播放链接] vehicleId={}, cameraId={}", vehicleId, cameraId);

        if (!StringUtils.hasText(vehicleId) || !StringUtils.hasText(cameraId)) {
            return WVPResult.fail(ErrorCode.ERROR400.getCode(), "车辆ID和相机ID不能为空");
        }

        try {
            String webrtcUrl = vehicleService.getVehicleCameraWebRTCUrl(vehicleId, cameraId);
            if (webrtcUrl != null) {
                return WVPResult.success(webrtcUrl);
            } else {
                return WVPResult.fail(ErrorCode.ERROR404.getCode(), "未找到相机推流或推流未启动");
            }
        } catch (Exception e) {
            log.error("[获取单个相机WebRTC播放链接] 异常: vehicleId={}, cameraId={}", vehicleId, cameraId, e);
            return WVPResult.fail(ErrorCode.ERROR500.getCode(), "获取播放链接异常: " + e.getMessage());
        }
    }

    @Operation(summary = "获取多个相机WebRTC播放链接", description = "通过查询参数获取多个相机的WebRTC播放链接")
    @GetMapping("/{vehicleId}/cameras/webrtc/play")
    public WVPResult<Map<String, String>> getMultipleCamerasWebRTCUrls(
            @Parameter(description = "车辆ID", required = true) @PathVariable String vehicleId,
            @Parameter(description = "相机ID列表，多个ID用逗号分隔", required = true) 
            @RequestParam("cameraIds") String cameraIds) {
        log.info("[获取多个相机WebRTC播放链接] vehicleId={}, cameraIds={}", vehicleId, cameraIds);

        if (!StringUtils.hasText(vehicleId) || !StringUtils.hasText(cameraIds)) {
            return WVPResult.fail(ErrorCode.ERROR400.getCode(), "车辆ID和相机ID列表不能为空");
        }

        try {
            String[] cameraIdArray = cameraIds.split(",");
            Map<String, String> webrtcUrls = new HashMap<>();
            
            for (String cameraId : cameraIdArray) {
                String trimmedId = cameraId.trim();
                if (StringUtils.hasText(trimmedId)) {
                    String webrtcUrl = vehicleService.getVehicleCameraWebRTCUrl(vehicleId, trimmedId);
                    webrtcUrls.put(trimmedId, webrtcUrl);
                }
            }
            
            log.info("[获取多个相机WebRTC播放链接] 处理完成: vehicleId={}, 处理相机数量={}", vehicleId, webrtcUrls.size());
            return WVPResult.success(webrtcUrls);
        } catch (Exception e) {
            log.error("[获取多个相机WebRTC播放链接] 异常: vehicleId={}, cameraIds={}", vehicleId, cameraIds, e);
            return WVPResult.fail(ErrorCode.ERROR500.getCode(), "获取播放链接异常: " + e.getMessage());
        }
    }

    @Operation(summary = "批量获取相机WebRTC播放链接", description = "通过POST请求体批量获取多个相机的WebRTC播放链接")
    @PostMapping("/{vehicleId}/cameras/webrtc/play")
    public WVPResult<Map<String, String>> batchGetVehicleCameraWebRTCUrls(
            @Parameter(description = "车辆ID", required = true) @PathVariable String vehicleId,
            @RequestBody List<String> cameraIds) {
        log.info("[批量获取WebRTC播放链接] vehicleId={}, cameraIds={}", vehicleId, cameraIds);

        if (!StringUtils.hasText(vehicleId)) {
            return WVPResult.fail(ErrorCode.ERROR400.getCode(), "车辆ID不能为空");
        }

        if (cameraIds == null || cameraIds.isEmpty()) {
            return WVPResult.fail(ErrorCode.ERROR400.getCode(), "相机ID列表不能为空");
        }

        try {
            Map<String, String> webrtcUrls = new HashMap<>();
            
            for (String cameraId : cameraIds) {
                if (StringUtils.hasText(cameraId)) {
                    String webrtcUrl = vehicleService.getVehicleCameraWebRTCUrl(vehicleId, cameraId.trim());
                    webrtcUrls.put(cameraId.trim(), webrtcUrl);
                }
            }
            
            log.info("[批量获取WebRTC播放链接] 处理完成: vehicleId={}, 处理相机数量={}", vehicleId, webrtcUrls.size());
            return WVPResult.success(webrtcUrls);
        } catch (Exception e) {
            log.error("[批量获取WebRTC播放链接] 异常: vehicleId={}, cameraIds={}", vehicleId, cameraIds, e);
            return WVPResult.fail(ErrorCode.ERROR500.getCode(), "批量获取播放链接异常: " + e.getMessage());
        }
    }

    @Operation(summary = "检查车辆连接", description = "检查与车辆端HTTP API的连接状态")
    @GetMapping("/{vehicleId}/connection/check")
    public WVPResult<Boolean> checkVehicleConnection(
            @Parameter(description = "车辆ID", required = true) @PathVariable String vehicleId) {
        log.debug("[检查车辆连接] vehicleId={}", vehicleId);

        if (!StringUtils.hasText(vehicleId)) {
            return WVPResult.fail(ErrorCode.ERROR400.getCode(), "车辆ID不能为空");
        }

        try {
            boolean connected = vehicleService.checkVehicleConnection(vehicleId);
            return WVPResult.success(connected);
        } catch (Exception e) {
            log.error("[检查车辆连接] 异常: vehicleId={}", vehicleId, e);
            return WVPResult.fail(ErrorCode.ERROR500.getCode(), "检查异常: " + e.getMessage());
        }
    }

    @Operation(summary = "直接调用车辆端启动相机推流", description = "直接调用车辆端HTTP API启动相机推流")
    @PostMapping("/{vehicleId}/camera/{cameraId}/direct/start")
    public WVPResult<Void> directStartCameraStream(
            @Parameter(description = "车辆ID", required = true) @PathVariable String vehicleId,
            @Parameter(description = "相机ID", required = true) @PathVariable String cameraId) {
        log.info("[直接启动推流] vehicleId={}, cameraId={}", vehicleId, cameraId);

        if (!StringUtils.hasText(vehicleId) || !StringUtils.hasText(cameraId)) {
            return WVPResult.fail(ErrorCode.ERROR400.getCode(), "车辆ID和相机ID不能为空");
        }

        try {
            boolean success = vehicleService.directStartCameraStream(vehicleId, cameraId);
            if (success) {
                return WVPResult.<Void>success(null);
            } else {
                return WVPResult.fail(ErrorCode.ERROR100.getCode(), "直接启动推流失败");
            }
        } catch (Exception e) {
            log.error("[直接启动推流] 异常: vehicleId={}, cameraId={}", vehicleId, cameraId, e);
            return WVPResult.fail(ErrorCode.ERROR500.getCode(), "直接启动推流异常: " + e.getMessage());
        }
    }

    @Operation(summary = "直接调用车辆端停止相机推流", description = "直接调用车辆端HTTP API停止相机推流")
    @PostMapping("/{vehicleId}/camera/{cameraId}/direct/stop")
    public WVPResult<Void> directStopCameraStream(
            @Parameter(description = "车辆ID", required = true) @PathVariable String vehicleId,
            @Parameter(description = "相机ID", required = true) @PathVariable String cameraId) {
        log.info("[直接停止推流] vehicleId={}, cameraId={}", vehicleId, cameraId);

        if (!StringUtils.hasText(vehicleId) || !StringUtils.hasText(cameraId)) {
            return WVPResult.fail(ErrorCode.ERROR400.getCode(), "车辆ID和相机ID不能为空");
        }

        try {
            boolean success = vehicleService.directStopCameraStream(vehicleId, cameraId);
            if (success) {
                return WVPResult.<Void>success(null);
            } else {
                return WVPResult.fail(ErrorCode.ERROR100.getCode(), "直接停止推流失败");
            }
        } catch (Exception e) {
            log.error("[直接停止推流] 异常: vehicleId={}, cameraId={}", vehicleId, cameraId, e);
            return WVPResult.fail(ErrorCode.ERROR500.getCode(), "直接停止推流异常: " + e.getMessage());
        }
    }
}

