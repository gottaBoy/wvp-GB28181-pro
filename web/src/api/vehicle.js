import request from '@/utils/request'

// 车辆管理API

/**
 * 查询所有车辆列表
 */
export function getAllVehicles() {
  return request({
    method: 'get',
    url: '/api/vehicle/list'
  })
}

/**
 * 查询车辆信息
 */
export function getVehicle(vehicleId) {
  return request({
    method: 'get',
    url: `/api/vehicle/${vehicleId}`
  })
}

/**
 * 查询车辆相机列表
 */
export function getVehicleCameras(vehicleId) {
  return request({
    method: 'get',
    url: `/api/vehicle/${vehicleId}/cameras`
  })
}

/**
 * 启动相机推流
 */
export function startCameraStream(vehicleId, cameraId) {
  return request({
    method: 'post',
    url: `/api/vehicle/${vehicleId}/camera/${cameraId}/start`
  })
}

/**
 * 停止相机推流
 */
export function stopCameraStream(vehicleId, cameraId) {
  return request({
    method: 'post',
    url: `/api/vehicle/${vehicleId}/camera/${cameraId}/stop`
  })
}

/**
 * 批量订阅相机
 */
export function subscribeVehicleCameras(vehicleId, cameraIds) {
  return request({
    method: 'post',
    url: `/api/vehicle/${vehicleId}/cameras/subscribe`,
    data: cameraIds
  })
}

/**
 * 批量取消订阅相机
 */
export function unsubscribeVehicleCameras(vehicleId, cameraIds) {
  return request({
    method: 'post',
    url: `/api/vehicle/${vehicleId}/cameras/unsubscribe`,
    data: cameraIds
  })
}

/**
 * 获取车辆已订阅相机
 */
export function getVehicleSubscribedCameras(vehicleId) {
  return request({
    method: 'get',
    url: `/api/vehicle/${vehicleId}/cameras/subscribed`
  })
}

/**
 * 检查车辆连接状态
 */
export function checkVehicleConnection(vehicleId) {
  return request({
    method: 'get',
    url: `/api/vehicle/${vehicleId}/connection/check`
  })
}

/**
 * 直接启动相机推流（调用车辆端API）
 */
export function directStartCameraStream(vehicleId, cameraId) {
  return request({
    method: 'post',
    url: `/api/vehicle/${vehicleId}/camera/${cameraId}/direct/start`
  })
}

/**
 * 直接停止相机推流（调用车辆端API）
 */
export function directStopCameraStream(vehicleId, cameraId) {
  return request({
    method: 'post',
    url: `/api/vehicle/${vehicleId}/camera/${cameraId}/direct/stop`
  })
}

/**
 * 获取相机WebRTC播放链接
 */
export function getVehicleCameraWebRTCUrl(vehicleId, cameraId) {
  return request({
    method: 'get',
    url: `/api/vehicle/${vehicleId}/camera/${cameraId}/webrtc/play`
  })
}

