<template>
  <div class="vehicle-camera-monitor">
    <!-- 分栏布局 -->
    <el-container>
      <!-- 左侧车辆列表 -->
      <el-aside width="300px" class="left-panel">
        <el-card class="vehicle-panel" shadow="never">
          <template #header>
            <div class="panel-header">
              <span>车辆列表</span>
              <el-button 
                type="primary" 
                size="small"
                icon="el-icon-refresh"
                @click="refreshVehicles"
                :loading="vehicleLoading">
                刷新
              </el-button>
            </div>
          </template>

          <div class="vehicle-list">
            <div
              v-for="vehicle in vehicles"
              :key="vehicle.vehicleId"
              :class="['vehicle-item', { active: selectedVehicle && selectedVehicle.vehicleId === vehicle.vehicleId }]"
              @click="selectVehicle(vehicle)">
              
              <div class="vehicle-info">
                <div class="vehicle-name">{{ vehicle.vehicleName || vehicle.vehicleId }}</div>
                <div class="vehicle-id">{{ vehicle.vehicleId }}</div>
                <div class="vehicle-ip">{{ vehicle.ipAddress || '未知IP' }}</div>
              </div>
              
              <div class="vehicle-status">
                <el-tag 
                  :type="vehicle.status === 'online' ? 'success' : 'danger'"
                  size="mini">
                  {{ vehicle.status === 'online' ? '在线' : '离线' }}
                </el-tag>
                <div class="camera-count">
                  {{ vehicle.cameras ? vehicle.cameras.length : 0 }} 相机
                </div>
              </div>
            </div>
          </div>

          <!-- 空状态 -->
          <el-empty 
            v-if="!vehicleLoading && vehicles.length === 0"
            description="暂无车辆数据"
            image-size="80">
          </el-empty>
        </el-card>
      </el-aside>

      <!-- 右侧相机列表和播放区域 -->
      <el-main class="right-panel">
        <!-- 工具栏 -->
        <div class="toolbar">
          <div class="toolbar-left">
            <span v-if="selectedVehicle" class="current-vehicle">
              当前车辆: {{ selectedVehicle.vehicleName || selectedVehicle.vehicleId }}
            </span>
            <span v-else class="no-vehicle">请选择车辆</span>
          </div>
          
          <div class="toolbar-right" v-if="selectedVehicle">
            <el-button-group>
              <el-button 
                type="success"
                icon="el-icon-video-play"
                :disabled="selectedCameras.length === 0"
                @click="batchPlayCameras"
                :loading="batchPlaying">
                播放选中 ({{ selectedCameras.length }})
              </el-button>
              <el-button 
                type="warning"
                icon="el-icon-video-pause"
                :disabled="playingCameras.size === 0"
                @click="batchStopCameras">
                停止全部
              </el-button>
              <el-button 
                type="primary"
                icon="el-icon-refresh"
                @click="refreshCameras"
                :loading="cameraLoading">
                刷新相机
              </el-button>
            </el-button-group>

            <el-dropdown @command="handleBatchCommand" style="margin-left: 12px;">
              <el-button type="info">
                批量操作<i class="el-icon-arrow-down el-icon--right"></i>
              </el-button>
              <el-dropdown-menu slot="dropdown">
                <el-dropdown-item command="selectAll">全选相机</el-dropdown-item>
                <el-dropdown-item command="selectNone">取消全选</el-dropdown-item>
                <el-dropdown-item command="selectActive" divided>选择活跃相机</el-dropdown-item>
                <el-dropdown-item command="selectStreaming">选择推流中</el-dropdown-item>
              </el-dropdown-menu>
            </el-dropdown>
          </div>
        </div>

        <!-- 相机列表 -->
        <el-card v-if="selectedVehicle" class="camera-panel" shadow="never">
          <template #header>
            <div class="panel-header">
              <span>相机列表 ({{ cameras.length }})</span>
              <div class="selection-info">
                已选择: {{ selectedCameras.length }} / {{ cameras.length }}
              </div>
            </div>
          </template>

          <!-- 相机表格 -->
          <el-table
            ref="cameraTable"
            :data="cameras"
            v-loading="cameraLoading"
            @selection-change="handleSelectionChange"
            style="width: 100%">
            
            <el-table-column type="selection" width="55" align="center"></el-table-column>
            
            <el-table-column prop="cameraId" label="相机ID" width="140">
              <template #default="scope">
                <el-tag size="mini" effect="plain">{{ scope.row.cameraId }}</el-tag>
              </template>
            </el-table-column>
            
            <el-table-column prop="name" label="相机名称" min-width="150">
              <template #default="scope">
                <div class="camera-name">
                  <i class="el-icon-video-camera" style="color: #409EFF; margin-right: 4px;"></i>
                  {{ scope.row.name || '未命名相机' }}
                </div>
              </template>
            </el-table-column>
            
            <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip>
              <template #default="scope">
                {{ scope.row.description || '-' }}
              </template>
            </el-table-column>
            
            <el-table-column label="状态" width="90" align="center">
              <template #default="scope">
                <el-tag 
                  :type="scope.row.status === 'active' ? 'success' : 'info'"
                  size="mini">
                  {{ scope.row.status === 'active' ? '活跃' : '非活跃' }}
                </el-tag>
              </template>
            </el-table-column>
            
            <el-table-column label="推流状态" width="100" align="center">
              <template #default="scope">
                <el-tag 
                  :type="scope.row.pushing ? 'success' : 'info'"
                  size="mini">
                  {{ scope.row.pushing ? '推流中' : '未推流' }}
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column label="播放状态" width="100" align="center">
              <template #default="scope">
                <el-tag 
                  v-if="playingCameras.has(scope.row.cameraId)"
                  type="warning" 
                  size="mini">
                  播放中
                </el-tag>
                <span v-else class="text-muted">未播放</span>
              </template>
            </el-table-column>
            
            <el-table-column label="操作" width="120" align="center">
              <template #default="scope">
                <el-button
                  v-if="!playingCameras.has(scope.row.cameraId)"
                  type="success"
                  size="mini"
                  icon="el-icon-video-play"
                  @click="playSingleCamera(scope.row)"
                  :loading="singlePlayLoading[scope.row.cameraId]">
                </el-button>
                <el-button
                  v-else
                  type="warning"
                  size="mini"
                  icon="el-icon-video-pause"
                  @click="stopSingleCamera(scope.row.cameraId)">
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 空状态 -->
          <el-empty 
            v-if="!cameraLoading && cameras.length === 0"
            description="该车辆暂无相机数据"
            image-size="80">
          </el-empty>
        </el-card>

        <!-- 未选择车辆提示 -->
        <div v-else class="no-vehicle-selected">
          <el-empty 
            description="请从左侧选择一个车辆查看相机信息"
            image-size="120">
          </el-empty>
        </div>
      </el-main>
    </el-container>

    <!-- 多窗口播放区域 -->
    <div v-if="playingCameras.size > 0" class="video-grid">
      <draggable 
        v-model="videoWindows" 
        :options="{ animation: 200, ghostClass: 'ghost' }"
        class="video-container">
        <div
          v-for="(videoInfo, cameraId) in playingCameras"
          :key="cameraId"
          class="video-window"
          :class="{ 'video-window-fullscreen': videoInfo.fullscreen }">
          
          <div class="video-header">
            <div class="video-title">
              <i class="el-icon-video-camera"></i>
              <span>{{ videoInfo.cameraName }}</span>
              <el-tag size="mini" type="success">{{ cameraId }}</el-tag>
            </div>
            <div class="video-controls">
              <el-button-group size="mini">
                <el-button 
                  icon="el-icon-full-screen"
                  @click="toggleFullscreen(cameraId)"
                  :type="videoInfo.fullscreen ? 'warning' : 'info'">
                </el-button>
                <el-button 
                  icon="el-icon-close"
                  type="danger"
                  @click="stopSingleCamera(cameraId)">
                </el-button>
              </el-button-group>
            </div>
          </div>

          <div class="video-content">
            <rtc-player
              :ref="'player_' + cameraId"
              :visible="true"
              :video-url="videoInfo.url"
              :error="videoInfo.error"
              style="width: 100%; height: 100%;"
              :has-audio="true"
              fluent
              autoplay
              live
              @error="handleVideoError(cameraId, $event)"
              @ready="handleVideoReady(cameraId)"
            />
            
            <!-- 加载状态 -->
            <div v-if="videoInfo.loading" class="video-loading">
              <el-loading-spinner></el-loading-spinner>
              <div>正在加载视频流...</div>
            </div>

            <!-- 错误状态 -->
            <div v-if="videoInfo.error" class="video-error">
              <i class="el-icon-warning-outline"></i>
              <div>{{ videoInfo.error }}</div>
            </div>
          </div>
        </div>
      </draggable>
    </div>
  </div>
</template>

<script>
import { 
  getAllVehicles, 
  getVehicleCameras,
  batchGetVehicleCameraWebRTCUrls
} from '@/api/vehicle'
import rtcPlayer from '@/views/common/rtcPlayer.vue'
import draggable from 'vuedraggable'

export default {
  name: 'VehicleCameraMonitor',
  components: {
    rtcPlayer,
    draggable
  },
  data() {
    return {
      // 车辆相关
      vehicles: [],
      selectedVehicle: null,
      vehicleLoading: false,
      
      // 相机相关
      cameras: [],
      selectedCameras: [],
      cameraLoading: false,
      
      // 播放相关
      playingCameras: new Map(), // cameraId -> { url, cameraName, loading, error, fullscreen }
      videoWindows: [],
      batchPlaying: false,
      singlePlayLoading: {},

      // 定时器
      autoRefreshTimer: null
    }
  },
  mounted() {
    this.loadVehicles()
    // 启动定时刷新
    this.startAutoRefresh()
  },
  beforeDestroy() {
    this.stopAutoRefresh()
    this.stopAllCameras()
  },
  methods: {
    // ==================== 车辆管理 ====================
    async loadVehicles() {
      this.vehicleLoading = true
      try {
        const response = await getAllVehicles()
        if (response.code === 0) {
          this.vehicles = response.data || []
          console.log('加载车辆列表:', this.vehicles.length, '个车辆')
        } else {
          this.$message.error('获取车辆列表失败: ' + response.msg)
        }
      } catch (error) {
        console.error('获取车辆列表异常:', error)
        this.$message.error('获取车辆列表异常: ' + error.message)
      } finally {
        this.vehicleLoading = false
      }
    },

    selectVehicle(vehicle) {
      if (this.selectedVehicle && this.selectedVehicle.vehicleId === vehicle.vehicleId) {
        return // 已经选中的车辆，不重复加载
      }
      
      console.log('选择车辆:', vehicle)
      this.selectedVehicle = vehicle
      this.selectedCameras = []
      this.stopAllCameras() // 停止之前的播放
      this.loadVehicleCameras(vehicle.vehicleId)
    },

    refreshVehicles() {
      this.loadVehicles()
    },

    // ==================== 相机管理 ====================
    async loadVehicleCameras(vehicleId) {
      this.cameraLoading = true
      try {
        const response = await getVehicleCameras(vehicleId)
        if (response.code === 0) {
          this.cameras = response.data || []
          console.log('加载相机列表:', this.cameras.length, '个相机')
          // 清空之前的选择
          this.$nextTick(() => {
            if (this.$refs.cameraTable) {
              this.$refs.cameraTable.clearSelection()
            }
          })
        } else {
          this.$message.error('获取相机列表失败: ' + response.msg)
        }
      } catch (error) {
        console.error('获取相机列表异常:', error)
        this.$message.error('获取相机列表异常: ' + error.message)
      } finally {
        this.cameraLoading = false
      }
    },

    handleSelectionChange(selection) {
      this.selectedCameras = selection
      console.log('选中相机变更:', selection.map(c => c.cameraId))
    },

    refreshCameras() {
      if (this.selectedVehicle) {
        this.loadVehicleCameras(this.selectedVehicle.vehicleId)
      }
    },

    // ==================== 批量操作 ====================
    handleBatchCommand(command) {
      if (!this.$refs.cameraTable) return
      
      switch (command) {
        case 'selectAll':
          this.$refs.cameraTable.toggleAllSelection()
          break
        case 'selectNone':
          this.$refs.cameraTable.clearSelection()
          break
        case 'selectActive':
          this.$refs.cameraTable.clearSelection()
          this.cameras.forEach(camera => {
            if (camera.status === 'active') {
              this.$refs.cameraTable.toggleRowSelection(camera, true)
            }
          })
          break
        case 'selectStreaming':
          this.$refs.cameraTable.clearSelection()
          this.cameras.forEach(camera => {
            if (camera.pushing) {
              this.$refs.cameraTable.toggleRowSelection(camera, true)
            }
          })
          break
      }
    },

    // ==================== 播放控制 ====================
    async batchPlayCameras() {
      if (this.selectedCameras.length === 0) {
        this.$message.warning('请先选择要播放的相机')
        return
      }

      if (!this.selectedVehicle) {
        this.$message.error('未选择车辆')
        return
      }

      this.batchPlaying = true
      console.log('批量播放相机:', this.selectedCameras.map(c => c.cameraId))

      try {
        const cameraIds = this.selectedCameras.map(camera => camera.cameraId)
        
        // 获取WebRTC播放链接
        const response = await batchGetVehicleCameraWebRTCUrls(this.selectedVehicle.vehicleId, cameraIds)
        if (response.code === 0 && response.data) {
          const webrtcUrls = response.data
          
          // 创建播放窗口
          for (const camera of this.selectedCameras) {
            const cameraId = camera.cameraId
            const url = webrtcUrls[cameraId]
            
            if (url) {
              this.playingCameras.set(cameraId, {
                url: url,
                cameraName: camera.name || camera.cameraId,
                loading: true,
                error: null,
                fullscreen: false
              })
            } else {
              console.warn('未获取到相机播放链接:', cameraId)
              this.$message.warning(`相机 ${cameraId} 未获取到播放链接`)
            }
          }

          // 触发响应式更新
          this.playingCameras = new Map(this.playingCameras)
          
          this.$message.success(`开始播放 ${Object.keys(webrtcUrls).length} 个相机`)
        } else {
          this.$message.error('获取播放链接失败: ' + (response.msg || '未知错误'))
        }
      } catch (error) {
        console.error('批量播放失败:', error)
        this.$message.error('批量播放失败: ' + error.message)
      } finally {
        this.batchPlaying = false
      }
    },

    async playSingleCamera(camera) {
      if (!this.selectedVehicle) {
        this.$message.error('未选择车辆')
        return
      }

      this.$set(this.singlePlayLoading, camera.cameraId, true)
      
      try {
        const response = await batchGetVehicleCameraWebRTCUrls(this.selectedVehicle.vehicleId, [camera.cameraId])
        if (response.code === 0 && response.data) {
          const url = response.data[camera.cameraId]
          if (url) {
            this.playingCameras.set(camera.cameraId, {
              url: url,
              cameraName: camera.name || camera.cameraId,
              loading: true,
              error: null,
              fullscreen: false
            })
            // 触发响应式更新
            this.playingCameras = new Map(this.playingCameras)
            this.$message.success(`开始播放相机: ${camera.cameraId}`)
          } else {
            this.$message.error('未获取到播放链接')
          }
        } else {
          this.$message.error('获取播放链接失败: ' + (response.msg || '未知错误'))
        }
      } catch (error) {
        console.error('播放相机失败:', error)
        this.$message.error('播放失败: ' + error.message)
      } finally {
        this.$set(this.singlePlayLoading, camera.cameraId, false)
      }
    },

    stopSingleCamera(cameraId) {
      if (this.playingCameras.has(cameraId)) {
        // 停止播放器
        const playerRef = this.$refs[`player_${cameraId}`]
        if (playerRef && playerRef[0]) {
          playerRef[0].pause()
        }
        
        this.playingCameras.delete(cameraId)
        // 触发响应式更新
        this.playingCameras = new Map(this.playingCameras)
        
        console.log('停止播放相机:', cameraId)
        this.$message.success(`停止播放相机: ${cameraId}`)
      }
    },

    batchStopCameras() {
      this.stopAllCameras()
      this.$message.success('已停止所有播放')
    },

    stopAllCameras() {
      // 停止所有播放器
      this.playingCameras.forEach((_, cameraId) => {
        const playerRef = this.$refs[`player_${cameraId}`]
        if (playerRef && playerRef[0]) {
          playerRef[0].pause()
        }
      })
      
      this.playingCameras.clear()
      this.playingCameras = new Map()
      console.log('停止所有相机播放')
    },

    toggleFullscreen(cameraId) {
      if (this.playingCameras.has(cameraId)) {
        const videoInfo = this.playingCameras.get(cameraId)
        videoInfo.fullscreen = !videoInfo.fullscreen
        this.playingCameras.set(cameraId, videoInfo)
        // 触发响应式更新
        this.playingCameras = new Map(this.playingCameras)
      }
    },

    // ==================== 播放器事件 ====================
    handleVideoError(cameraId, error) {
      console.error('视频播放错误:', cameraId, error)
      if (this.playingCameras.has(cameraId)) {
        const videoInfo = this.playingCameras.get(cameraId)
        videoInfo.error = error.message || '播放失败'
        videoInfo.loading = false
        this.playingCameras.set(cameraId, videoInfo)
        // 触发响应式更新
        this.playingCameras = new Map(this.playingCameras)
      }
    },

    handleVideoReady(cameraId) {
      console.log('视频准备就绪:', cameraId)
      if (this.playingCameras.has(cameraId)) {
        const videoInfo = this.playingCameras.get(cameraId)
        videoInfo.loading = false
        videoInfo.error = null
        this.playingCameras.set(cameraId, videoInfo)
        // 触发响应式更新
        this.playingCameras = new Map(this.playingCameras)
      }
    },

    // ==================== 定时刷新 ====================
    startAutoRefresh() {
      // 每30秒刷新一次车辆状态
      this.autoRefreshTimer = setInterval(() => {
        this.loadVehicles()
      }, 30000)
    },

    stopAutoRefresh() {
      if (this.autoRefreshTimer) {
        clearInterval(this.autoRefreshTimer)
        this.autoRefreshTimer = null
      }
    }
  }
}
</script>

<style scoped>
.vehicle-camera-monitor {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f5f7fa;
}

/* ==================== 左侧车辆面板 ==================== */
.left-panel {
  background-color: #fff;
  border-right: 1px solid #e4e7ed;
}

.vehicle-panel {
  height: 100%;
  border: none;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}

.vehicle-list {
  max-height: calc(100vh - 120px);
  overflow-y: auto;
}

.vehicle-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  margin-bottom: 8px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s;
}

.vehicle-item:hover {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.1);
}

.vehicle-item.active {
  border-color: #409eff;
  background-color: #ecf5ff;
}

.vehicle-info {
  flex: 1;
}

.vehicle-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 2px;
}

.vehicle-id {
  font-size: 12px;
  color: #909399;
  margin-bottom: 2px;
}

.vehicle-ip {
  font-size: 11px;
  color: #c0c4cc;
}

.vehicle-status {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.camera-count {
  font-size: 11px;
  color: #909399;
  margin-top: 4px;
}

/* ==================== 右侧主面板 ==================== */
.right-panel {
  flex: 1;
  padding: 0;
  display: flex;
  flex-direction: column;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  background-color: #fff;
  border-bottom: 1px solid #e4e7ed;
  margin-bottom: 0;
}

.toolbar-left .current-vehicle {
  font-size: 16px;
  font-weight: 600;
  color: #409eff;
}

.toolbar-left .no-vehicle {
  font-size: 14px;
  color: #909399;
}

.camera-panel {
  flex: 1;
  margin: 20px;
  margin-top: 0;
  border: none;
}

.selection-info {
  font-size: 12px;
  color: #909399;
}

.camera-name {
  display: flex;
  align-items: center;
}

.text-muted {
  color: #c0c4cc;
  font-size: 12px;
}

.no-vehicle-selected {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
}

/* ==================== 视频播放区域 ==================== */
.video-grid {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.8);
  z-index: 2000;
  padding: 20px;
  overflow: auto;
}

.video-container {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  grid-gap: 20px;
  height: 100%;
}

.video-window {
  background-color: #000;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
  display: flex;
  flex-direction: column;
  min-height: 300px;
}

.video-window-fullscreen {
  position: fixed !important;
  top: 20px !important;
  left: 20px !important;
  right: 20px !important;
  bottom: 20px !important;
  z-index: 3000;
  grid-column: 1 / -1;
  grid-row: 1 / -1;
}

.video-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background-color: #2c3e50;
  color: #fff;
}

.video-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.video-content {
  flex: 1;
  position: relative;
  background-color: #000;
}

.video-loading {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: #fff;
  text-align: center;
}

.video-error {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: #f56c6c;
  text-align: center;
}

.video-error i {
  font-size: 24px;
  margin-bottom: 8px;
}

/* ==================== 拖拽效果 ==================== */
.ghost {
  opacity: 0.5;
}

/* ==================== 响应式布局 ==================== */
@media (max-width: 768px) {
  .left-panel {
    width: 250px !important;
  }
  
  .video-container {
    grid-template-columns: 1fr;
  }
  
  .video-window {
    min-height: 250px;
  }
}

/* ==================== 滚动条样式 ==================== */
.vehicle-list::-webkit-scrollbar {
  width: 6px;
}

.vehicle-list::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.vehicle-list::-webkit-scrollbar-thumb {
  background: #c0c4cc;
  border-radius: 3px;
}

.vehicle-list::-webkit-scrollbar-thumb:hover {
  background: #909399;
}
</style>