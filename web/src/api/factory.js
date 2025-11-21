import request from '@/utils/request'

// 厂区监控API

/**
 * 获取厂区列表（按app分组）
 */
export function getFactoryListSummary() {
  return request({
    method: 'get',
    url: '/api/factory/list/summary'
  })
}

/**
 * 获取厂区下的所有摄像头
 * @param {string} app - 厂区应用名
 */
export function getFactoryCameras(app) {
  return request({
    method: 'get',
    url: '/api/factory/cameras',
    params: { app }
  })
}

/**
 * 获取摄像头播放地址
 * @param {number} id - 拉流代理ID
 */
export function getCameraPlayUrl(id) {
  return request({
    method: 'get',
    url: '/api/factory/start',
    params: { id }
  })
}

/**
 * 停止摄像头播放
 * @param {number} id - 拉流代理ID
 */
export function stopCameraPlay(id) {
  return request({
    method: 'get',
    url: '/api/factory/stop',
    params: { id }
  })
}

/**
 * 批量启动摄像头
 * @param {Array} ids - 拉流代理ID数组
 */
export function batchStartCameras(ids) {
  return request({
    method: 'post',
    url: '/api/factory/batch/start',
    data: { ids }
  })
}

/**
 * 批量停止摄像头
 * @param {Array} ids - 拉流代理ID数组
 */
export function batchStopCameras(ids) {
  return request({
    method: 'post',
    url: '/api/factory/batch/stop',
    data: { ids }
  })
}
