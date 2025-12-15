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
 * @param {boolean} groupByWarehouse - 是否按分组显示
 */
export function getFactoryCameras(app, groupByWarehouse = false) {
  return request({
    method: 'get',
    url: '/api/factory/cameras',
    params: { app, groupByWarehouse }
  })
}

/**
 * 获取摄像头播放地址
 * @param {number} id - 拉流代理ID
 */
export function getCameraPlayUrl(id) {
  return request({
    method: 'get',
    url: '/api/factory/cameras/start',
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
    url: '/api/factory/cameras/stop',
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
    url: '/api/factory/cameras/batch/start',
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
    url: '/api/factory/cameras/batch/stop',
    data: { ids }
  })
}

// ==================== 分组管理API ====================

/**
 * 获取厂区的所有分组
 * @param {string} app - 厂区应用名
 */
export function getFactoryGroups(app) {
  return request({
    method: 'get',
    url: '/api/factory/groups',
    params: { app }
  })
}

/**
 * 添加分组
 * @param {Object} group - 分组信息 {name, description, app, sortOrder}
 */
export function addFactoryGroup(group) {
  return request({
    method: 'post',
    url: '/api/factory/groups',
    data: group
  })
}

/**
 * 更新分组
 * @param {Object} group - 分组信息 {id, name, description, sortOrder}
 */
export function updateFactoryGroup(group) {
  return request({
    method: 'put',
    url: '/api/factory/groups',
    data: group
  })
}

/**
 * 删除分组
 * @param {number} id - 分组ID
 */
export function deleteFactoryGroup(id) {
  return request({
    method: 'delete',
    url: `/api/factory/groups/${id}`
  })
}

/**
 * 将拉流代理添加到分组
 * @param {number} groupId - 分组ID
 * @param {string} app - 拉流代理应用名
 * @param {string} stream - 拉流代理流ID
 */
export function addProxyToGroup(groupId, app, stream) {
  return request({
    method: 'post',
    url: `/api/factory/groups/${groupId}/proxies`,
    params: { app, stream }
  })
}

/**
 * 从分组中移除拉流代理
 * @param {number} groupId - 分组ID
 * @param {string} app - 拉流代理应用名
 * @param {string} stream - 拉流代理流ID
 */
export function removeProxyFromGroup(groupId, app, stream) {
  return request({
    method: 'delete',
    url: `/api/factory/groups/${groupId}/proxies`,
    params: { app, stream }
  })
}

/**
 * 批量将拉流代理添加到分组
 * @param {number} groupId - 分组ID
 * @param {Array} proxies - 拉流代理数组 [{app, stream}, ...]
 */
export function batchAddProxyToGroup(groupId, proxies) {
  return request({
    method: 'post',
    url: `/api/factory/groups/${groupId}/proxies/batch`,
    data: { proxies }
  })
}
