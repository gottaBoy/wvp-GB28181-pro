package com.genersoft.iot.vmp.gb28181.service;

import java.util.List;

/**
 * 车辆HTTP客户端服务接口
 * 用于调用车辆端的HTTP API，发送订阅/取消订阅指令
 * @author auto-generated
 */
public interface IVehicleHttpClientService {

    /**
     * 订阅车辆相机
     * @param vehicleIpAddress 车辆IP地址
     * @param port 车辆HTTP API端口
     * @param cameraIds 相机ID列表
     * @param apiKey API密钥（可选）
     * @return 是否成功
     */
    boolean subscribeCameras(String vehicleIpAddress, Integer port, List<String> cameraIds, String apiKey);

    /**
     * 取消订阅车辆相机
     * @param vehicleIpAddress 车辆IP地址
     * @param port 车辆HTTP API端口
     * @param cameraIds 相机ID列表
     * @param apiKey API密钥（可选）
     * @return 是否成功
     */
    boolean unsubscribeCameras(String vehicleIpAddress, Integer port, List<String> cameraIds, String apiKey);

    /**
     * 查询车辆已订阅的相机列表
     * @param vehicleIpAddress 车辆IP地址
     * @param port 车辆HTTP API端口
     * @param apiKey API密钥（可选）
     * @return 相机ID列表
     */
    List<String> getSubscribedCameras(String vehicleIpAddress, Integer port, String apiKey);

    /**
     * 检查车辆HTTP API健康状态
     * @param vehicleIpAddress 车辆IP地址
     * @param port 车辆HTTP API端口
     * @param apiKey API密钥（可选）
     * @return 是否健康
     */
    boolean checkHealth(String vehicleIpAddress, Integer port, String apiKey);

    /**
     * 直接调用车辆端启动相机推流
     * @param vehicleIpAddress 车辆IP地址
     * @param port 车辆HTTP API端口（通常为8081）
     * @param cameraId 相机ID
     * @param vehicleId 车辆ID
     * @param apiKey API密钥（可选）
     * @return 是否成功
     */
    boolean directSubscribeCamera(String vehicleIpAddress, Integer port, String cameraId, String vehicleId, String apiKey);

    /**
     * 直接调用车辆端停止相机推流
     * @param vehicleIpAddress 车辆IP地址
     * @param port 车辆HTTP API端口（通常为8081）
     * @param cameraId 相机ID
     * @param vehicleId 车辆ID
     * @param apiKey API密钥（可选）
     * @return 是否成功
     */
    boolean directUnsubscribeCamera(String vehicleIpAddress, Integer port, String cameraId, String vehicleId, String apiKey);
}