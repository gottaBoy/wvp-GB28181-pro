package com.genersoft.iot.vmp.gb28181.service;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.gb28181.bean.Vehicle;
import com.genersoft.iot.vmp.gb28181.bean.VehicleCamera;
import com.genersoft.iot.vmp.gb28181.bean.dto.VehicleCamerasUpdateDTO;
import com.genersoft.iot.vmp.gb28181.bean.dto.VehicleHeartbeatDTO;
import com.genersoft.iot.vmp.gb28181.bean.dto.VehicleRegisterDTO;
import com.genersoft.iot.vmp.gb28181.bean.dto.VehicleUpdateDTO;

import java.util.List;

/**
 * 车辆服务接口
 * @author auto-generated
 */
public interface IVehicleService {

    /**
     * 注册车辆
     * @param registerDTO 车辆注册信息
     * @return 车辆信息
     */
    Vehicle registerVehicle(VehicleRegisterDTO registerDTO);

    /**
     * 更新车辆心跳
     * @param vehicleId 车辆ID
     * @param heartbeatDTO 心跳信息
     * @return 是否成功
     */
    boolean updateHeartbeat(String vehicleId, VehicleHeartbeatDTO heartbeatDTO);

    /**
     * 更新车辆相机列表
     * @param vehicleId 车辆ID
     * @param camerasUpdateDTO 相机列表更新信息
     * @return 是否成功
     */
    boolean updateCameras(String vehicleId, VehicleCamerasUpdateDTO camerasUpdateDTO);

    /**
     * 根据车辆ID查询车辆信息
     * @param vehicleId 车辆ID
     * @return 车辆信息
     */
    Vehicle getVehicleByVehicleId(String vehicleId);

    /**
     * 查询车辆的所有相机
     * @param vehicleId 车辆ID
     * @return 相机列表
     */
    List<VehicleCamera> getCamerasByVehicleId(String vehicleId);

    /**
     * 查询所有车辆
     * @return 车辆列表
     */
    List<Vehicle> getAllVehicles();

    /**
     * 启动相机推流
     * @param vehicleId 车辆ID
     * @param cameraId 相机ID
     * @return 是否成功
     */
    boolean startCameraStream(String vehicleId, String cameraId);

    /**
     * 停止相机推流
     * @param vehicleId 车辆ID
     * @param cameraId 相机ID
     * @return 是否成功
     */
    boolean stopCameraStream(String vehicleId, String cameraId);

    /**
     * 批量订阅车辆相机（通过HTTP API调用车辆端）
     * @param vehicleId 车辆ID
     * @param cameraIds 相机ID列表
     * @return 是否成功
     */
    boolean subscribeVehicleCameras(String vehicleId, List<String> cameraIds);

    /**
     * 批量取消订阅车辆相机（通过HTTP API调用车辆端）
     * @param vehicleId 车辆ID
     * @param cameraIds 相机ID列表
     * @return 是否成功
     */
    boolean unsubscribeVehicleCameras(String vehicleId, List<String> cameraIds);

    /**
     * 获取车辆已订阅的相机列表（通过HTTP API调用车辆端）
     * @param vehicleId 车辆ID
     * @return 相机ID列表
     */
    List<String> getVehicleSubscribedCameras(String vehicleId);

    /**
     * 获取车辆相机的WebRTC播放链接
     * @param vehicleId 车辆ID
     * @param cameraId 相机ID
     * @return WebRTC播放链接，如果推流未启动则返回null
     */
    String getVehicleCameraWebRTCUrl(String vehicleId, String cameraId);

    /**
     * 检查车辆HTTP API连接状态
     * @param vehicleId 车辆ID
     * @return 是否连接正常
     */
    boolean checkVehicleConnection(String vehicleId);

    /**
     * 直接调用车辆端启动相机推流
     * @param vehicleId 车辆ID
     * @param cameraId 相机ID
     * @return 流信息（包含播放链接），失败返回null
     */
    StreamInfo directStartCameraStream(String vehicleId, String cameraId);

    /**
     * 直接调用车辆端停止相机推流
     * @param vehicleId 车辆ID
     * @param cameraId 相机ID
     * @return 是否成功
     */
    boolean directStopCameraStream(String vehicleId, String cameraId);

    /**
     * 检查并处理心跳超时的车辆
     * 将1分钟内没有心跳的在线车辆状态修改为离线
     * @param timeoutSeconds 心跳超时时间（秒）
     * @return 处理的车辆数量
     */
    int processHeartbeatTimeout(int timeoutSeconds);

    /**
     * 更新车辆基本信息
     * IP地址只有在不为空时才会更新，避免意外清空IP地址
     * @param vehicleId 车辆ID
     * @param updateDTO 更新信息
     * @return 是否成功
     */
    boolean updateVehicleInfo(String vehicleId, VehicleUpdateDTO updateDTO);
}

