/**
 * 车辆和相机日志记录工具
 * 提供统一的日志记录能力，支持车辆心跳、相机推流、播放、指令下发等业务场景
 */

// 日志级别
export const LogLevel = {
  DEBUG: 'DEBUG',
  INFO: 'INFO',
  WARN: 'WARN',
  ERROR: 'ERROR'
}

// 日志类型
export const LogType = {
  // 车辆相关
  VEHICLE_HEARTBEAT: 'VEHICLE_HEARTBEAT', // 车辆心跳
  VEHICLE_ONLINE: 'VEHICLE_ONLINE', // 车辆上线
  VEHICLE_OFFLINE: 'VEHICLE_OFFLINE', // 车辆离线
  VEHICLE_REGISTER: 'VEHICLE_REGISTER', // 车辆注册
  VEHICLE_STATUS_CHANGE: 'VEHICLE_STATUS_CHANGE', // 车辆状态变更

  // 相机相关
  CAMERA_SUBSCRIBE: 'CAMERA_SUBSCRIBE', // 相机订阅
  CAMERA_UNSUBSCRIBE: 'CAMERA_UNSUBSCRIBE', // 相机取消订阅
  CAMERA_STREAM_START: 'CAMERA_STREAM_START', // 相机开始推流
  CAMERA_STREAM_STOP: 'CAMERA_STREAM_STOP', // 相机停止推流
  CAMERA_STREAM_ERROR: 'CAMERA_STREAM_ERROR', // 相机推流错误
  CAMERA_STATUS_CHANGE: 'CAMERA_STATUS_CHANGE', // 相机状态变更

  // 播放相关
  PLAY_START: 'PLAY_START', // 开始播放
  PLAY_STOP: 'PLAY_STOP', // 停止播放
  PLAY_ERROR: 'PLAY_ERROR', // 播放错误
  PLAY_URL_GENERATED: 'PLAY_URL_GENERATED', // 播放链接生成

  // 指令下发
  COMMAND_SEND: 'COMMAND_SEND', // 指令发送
  COMMAND_SUCCESS: 'COMMAND_SUCCESS', // 指令执行成功
  COMMAND_FAILED: 'COMMAND_FAILED', // 指令执行失败
  COMMAND_TIMEOUT: 'COMMAND_TIMEOUT', // 指令超时

  // API调用
  API_REQUEST: 'API_REQUEST', // API请求
  API_RESPONSE: 'API_RESPONSE', // API响应
  API_ERROR: 'API_ERROR' // API错误
}

class VehicleLogger {
  constructor() {
    this.logs = [] // 内存中的日志缓存
    this.maxLogs = 1000 // 最大缓存日志数量
    this.enableConsole = true // 是否输出到控制台
    this.enableStorage = true // 是否存储到localStorage
    this.storageKey = 'vehicle_logs'
  }

  /**
   * 记录日志
   * @param {Object} logData - 日志数据
   */
  log(logData) {
    const log = {
      id: this.generateLogId(),
      timestamp: new Date().toISOString(),
      timestampMs: Date.now(),
      level: logData.level || LogLevel.INFO,
      type: logData.type,
      vehicleId: logData.vehicleId || null,
      cameraId: logData.cameraId || null,
      app: logData.app || null, // 应用名（通常是车辆ID）
      stream: logData.stream || null, // 流ID（通常是相机ID）
      message: logData.message || '',
      data: logData.data || {},
      error: logData.error || null,
      requestId: logData.requestId || null, // 请求ID，用于关联请求和响应
      duration: logData.duration || null, // 操作耗时（毫秒）
      userAgent: navigator.userAgent,
      url: window.location.href
    }

    // 添加到缓存
    this.logs.push(log)
    
    // 限制缓存大小
    if (this.logs.length > this.maxLogs) {
      this.logs.shift()
    }

    // 输出到控制台
    if (this.enableConsole) {
      this.logToConsole(log)
    }

    // 存储到localStorage
    if (this.enableStorage) {
      this.saveToStorage()
    }

    // 可以在这里添加发送到服务器的逻辑
    // this.sendToServer(log)

    return log
  }

  /**
   * 生成日志ID
   */
  generateLogId() {
    return `${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
  }

  /**
   * 输出到控制台
   */
  logToConsole(log) {
    const prefix = `[${log.timestamp}] [${log.level}] [${log.type}]`
    const info = []
    
    if (log.vehicleId) info.push(`VehicleID:${log.vehicleId}`)
    if (log.cameraId) info.push(`CameraID:${log.cameraId}`)
    if (log.app) info.push(`App:${log.app}`)
    if (log.stream) info.push(`Stream:${log.stream}`)
    if (log.duration) info.push(`Duration:${log.duration}ms`)

    const infoStr = info.length > 0 ? `[${info.join(' | ')}]` : ''
    const message = `${prefix} ${infoStr} ${log.message}`

    switch (log.level) {
      case LogLevel.DEBUG:
        console.debug(message, log.data, log.error)
        break
      case LogLevel.INFO:
        console.info(message, log.data)
        break
      case LogLevel.WARN:
        console.warn(message, log.data, log.error)
        break
      case LogLevel.ERROR:
        console.error(message, log.data, log.error)
        break
      default:
        console.log(message, log.data)
    }
  }

  /**
   * 保存到localStorage
   */
  saveToStorage() {
    try {
      const recentLogs = this.logs.slice(-100) // 只保存最近100条
      localStorage.setItem(this.storageKey, JSON.stringify(recentLogs))
    } catch (error) {
      console.error('Failed to save logs to localStorage:', error)
    }
  }

  /**
   * 从localStorage加载
   */
  loadFromStorage() {
    try {
      const stored = localStorage.getItem(this.storageKey)
      if (stored) {
        this.logs = JSON.parse(stored)
      }
    } catch (error) {
      console.error('Failed to load logs from localStorage:', error)
    }
  }

  /**
   * 清空日志
   */
  clear() {
    this.logs = []
    localStorage.removeItem(this.storageKey)
  }

  /**
   * 获取日志列表
   */
  getLogs(filter = {}) {
    let filtered = [...this.logs]

    // 按类型过滤
    if (filter.type) {
      filtered = filtered.filter(log => log.type === filter.type)
    }

    // 按车辆ID过滤
    if (filter.vehicleId) {
      filtered = filtered.filter(log => log.vehicleId === filter.vehicleId)
    }

    // 按相机ID过滤
    if (filter.cameraId) {
      filtered = filtered.filter(log => log.cameraId === filter.cameraId)
    }

    // 按日志级别过滤
    if (filter.level) {
      filtered = filtered.filter(log => log.level === filter.level)
    }

    // 按时间范围过滤
    if (filter.startTime) {
      filtered = filtered.filter(log => log.timestampMs >= filter.startTime)
    }
    if (filter.endTime) {
      filtered = filtered.filter(log => log.timestampMs <= filter.endTime)
    }

    return filtered
  }

  /**
   * 导出日志
   */
  export(format = 'json') {
    if (format === 'json') {
      return JSON.stringify(this.logs, null, 2)
    } else if (format === 'csv') {
      return this.exportToCsv()
    }
  }

  /**
   * 导出为CSV格式
   */
  exportToCsv() {
    const headers = ['时间', '级别', '类型', '车辆ID', '相机ID', '应用', '流ID', '消息', '耗时']
    const rows = this.logs.map(log => [
      log.timestamp,
      log.level,
      log.type,
      log.vehicleId || '',
      log.cameraId || '',
      log.app || '',
      log.stream || '',
      log.message,
      log.duration || ''
    ])

    const csv = [headers, ...rows]
      .map(row => row.map(cell => `"${cell}"`).join(','))
      .join('\n')

    return csv
  }

  /**
   * 下载日志文件
   */
  download(format = 'json') {
    const content = this.export(format)
    const blob = new Blob([content], { type: format === 'json' ? 'application/json' : 'text/csv' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `vehicle_logs_${new Date().toISOString()}.${format}`
    a.click()
    URL.revokeObjectURL(url)
  }

  // ==================== 便捷方法 ====================

  /**
   * 记录车辆心跳
   */
  logVehicleHeartbeat(vehicleId, data = {}) {
    return this.log({
      level: LogLevel.DEBUG,
      type: LogType.VEHICLE_HEARTBEAT,
      vehicleId,
      message: `车辆心跳上报`,
      data
    })
  }

  /**
   * 记录车辆上线
   */
  logVehicleOnline(vehicleId, data = {}) {
    return this.log({
      level: LogLevel.INFO,
      type: LogType.VEHICLE_ONLINE,
      vehicleId,
      message: `车辆上线`,
      data
    })
  }

  /**
   * 记录车辆离线
   */
  logVehicleOffline(vehicleId, data = {}) {
    return this.log({
      level: LogLevel.WARN,
      type: LogType.VEHICLE_OFFLINE,
      vehicleId,
      message: `车辆离线`,
      data
    })
  }

  /**
   * 记录相机订阅
   */
  logCameraSubscribe(vehicleId, cameraId, data = {}) {
    return this.log({
      level: LogLevel.INFO,
      type: LogType.CAMERA_SUBSCRIBE,
      vehicleId,
      cameraId,
      app: vehicleId,
      stream: cameraId,
      message: `订阅相机`,
      data
    })
  }

  /**
   * 记录相机取消订阅
   */
  logCameraUnsubscribe(vehicleId, cameraId, data = {}) {
    return this.log({
      level: LogLevel.INFO,
      type: LogType.CAMERA_UNSUBSCRIBE,
      vehicleId,
      cameraId,
      app: vehicleId,
      stream: cameraId,
      message: `取消订阅相机`,
      data
    })
  }

  /**
   * 记录相机开始推流
   */
  logCameraStreamStart(vehicleId, cameraId, data = {}) {
    return this.log({
      level: LogLevel.INFO,
      type: LogType.CAMERA_STREAM_START,
      vehicleId,
      cameraId,
      app: vehicleId,
      stream: cameraId,
      message: `相机开始推流`,
      data
    })
  }

  /**
   * 记录相机停止推流
   */
  logCameraStreamStop(vehicleId, cameraId, data = {}) {
    return this.log({
      level: LogLevel.INFO,
      type: LogType.CAMERA_STREAM_STOP,
      vehicleId,
      cameraId,
      app: vehicleId,
      stream: cameraId,
      message: `相机停止推流`,
      data
    })
  }

  /**
   * 记录相机推流错误
   */
  logCameraStreamError(vehicleId, cameraId, error, data = {}) {
    return this.log({
      level: LogLevel.ERROR,
      type: LogType.CAMERA_STREAM_ERROR,
      vehicleId,
      cameraId,
      app: vehicleId,
      stream: cameraId,
      message: `相机推流错误`,
      error,
      data
    })
  }

  /**
   * 记录播放开始
   */
  logPlayStart(vehicleId, cameraId, playUrl, data = {}) {
    return this.log({
      level: LogLevel.INFO,
      type: LogType.PLAY_START,
      vehicleId,
      cameraId,
      app: vehicleId,
      stream: cameraId,
      message: `开始播放相机`,
      data: { playUrl, ...data }
    })
  }

  /**
   * 记录播放停止
   */
  logPlayStop(vehicleId, cameraId, duration, data = {}) {
    return this.log({
      level: LogLevel.INFO,
      type: LogType.PLAY_STOP,
      vehicleId,
      cameraId,
      app: vehicleId,
      stream: cameraId,
      duration,
      message: `停止播放相机`,
      data
    })
  }

  /**
   * 记录播放错误
   */
  logPlayError(vehicleId, cameraId, error, data = {}) {
    return this.log({
      level: LogLevel.ERROR,
      type: LogType.PLAY_ERROR,
      vehicleId,
      cameraId,
      app: vehicleId,
      stream: cameraId,
      message: `播放错误`,
      error,
      data
    })
  }

  /**
   * 记录播放链接生成
   */
  logPlayUrlGenerated(vehicleId, cameraId, playUrl, duration, data = {}) {
    return this.log({
      level: LogLevel.INFO,
      type: LogType.PLAY_URL_GENERATED,
      vehicleId,
      cameraId,
      app: vehicleId,
      stream: cameraId,
      duration,
      message: `播放链接生成成功`,
      data: { playUrl, ...data }
    })
  }

  /**
   * 记录指令发送
   */
  logCommandSend(vehicleId, commandType, commandData = {}) {
    return this.log({
      level: LogLevel.INFO,
      type: LogType.COMMAND_SEND,
      vehicleId,
      message: `发送指令: ${commandType}`,
      data: { commandType, ...commandData }
    })
  }

  /**
   * 记录指令成功
   */
  logCommandSuccess(vehicleId, commandType, duration, responseData = {}) {
    return this.log({
      level: LogLevel.INFO,
      type: LogType.COMMAND_SUCCESS,
      vehicleId,
      duration,
      message: `指令执行成功: ${commandType}`,
      data: { commandType, ...responseData }
    })
  }

  /**
   * 记录指令失败
   */
  logCommandFailed(vehicleId, commandType, error, data = {}) {
    return this.log({
      level: LogLevel.ERROR,
      type: LogType.COMMAND_FAILED,
      vehicleId,
      message: `指令执行失败: ${commandType}`,
      error,
      data: { commandType, ...data }
    })
  }

  /**
   * 记录API请求
   */
  logApiRequest(method, url, params = {}, requestId = null) {
    return this.log({
      level: LogLevel.DEBUG,
      type: LogType.API_REQUEST,
      requestId: requestId || this.generateLogId(),
      message: `API请求: ${method} ${url}`,
      data: { method, url, params }
    })
  }

  /**
   * 记录API响应
   */
  logApiResponse(method, url, response, duration, requestId = null) {
    return this.log({
      level: LogLevel.DEBUG,
      type: LogType.API_RESPONSE,
      requestId,
      duration,
      message: `API响应: ${method} ${url}`,
      data: { method, url, response }
    })
  }

  /**
   * 记录API错误
   */
  logApiError(method, url, error, duration, requestId = null) {
    return this.log({
      level: LogLevel.ERROR,
      type: LogType.API_ERROR,
      requestId,
      duration,
      message: `API错误: ${method} ${url}`,
      error,
      data: { method, url }
    })
  }
}

// 创建全局单例
const vehicleLogger = new VehicleLogger()

// 初始化时从localStorage加载历史日志
vehicleLogger.loadFromStorage()

export default vehicleLogger
