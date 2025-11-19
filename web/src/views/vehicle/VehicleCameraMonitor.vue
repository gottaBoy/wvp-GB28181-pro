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

          <!-- 空状态-->
          <el-empty 
            v-if="!vehicleLoading && vehicles.length === 0"
            description="暂无车辆数据"
            :image-size="80">
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
                :disabled="Object.keys(playingCameras).length === 0"
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
                <el-dropdown-item command="selectStreaming">选择推流相机</el-dropdown-item>
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
                  v-if="playingCameras[scope.row.cameraId]"
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
                  v-if="!playingCameras[scope.row.cameraId]"
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

          <!-- 空状态-->
          <el-empty 
            v-if="!cameraLoading && cameras.length === 0"
            description="该车辆暂无相机数据"
            :image-size="80">
          </el-empty>
        </el-card>

        <!-- 未选择车辆提示 -->
        <div v-else class="no-vehicle-selected">
          <el-empty 
            description="请从左侧选择一个车辆查看相机信息"
            :image-size="120">
          </el-empty>
        </div>
      </el-main>
    </el-container>
    
    <!-- 多窗口视频播放区-- 平铺显示 -->
    <div v-if="Object.keys(playingCameras).length > 0" class="video-grid-overlay">
      <div class="video-grid-header">
        <div class="header-left">
          <i class="el-icon-video-camera"></i>
          <span>正在播放 {{ Object.keys(playingCameras).length }} 个相机</span>
        </div>
        <div class="header-right">
          <el-button 
            type="warning" 
            size="small"
            icon="el-icon-close"
            @click="batchStopCameras">
            关闭所有
          </el-button>
        </div>
      </div>
      
      <div class="video-grid-container" :class="getGridClass()">
        <div 
          v-for="(videoInfo, cameraId) in playingCameras"
          :key="cameraId"
          class="video-grid-item">
          <div class="video-item-header">
            <span class="video-item-title">
              <i class="el-icon-video-camera"></i>
              {{ getCameraName(cameraId) }}
            </span>
            <el-button 
              type="text" 
              icon="el-icon-close"
              size="mini"
              @click="stopSingleCamera(cameraId)">
            </el-button>
          </div>
          <div class="video-item-content">
            <vehicle-rtc-player
              :ref="'rtcPlayer_' + cameraId"
              :player-id="'video_' + cameraId"
              :video-url="videoInfo.url"
              :has-audio="true"
              style="width: 100%; height: 100%;"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { 
  getAllVehicles, 
  getVehicleCameras,
  getVehicleCameraWebRTCUrl,
  batchGetVehicleCameraWebRTCUrls
} from '@/api/vehicle'
import VehicleRtcPlayer from './VehicleRtcPlayer.vue'

export default {
  name: 'VehicleCameraMonitor',
  components: {
    VehicleRtcPlayer
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
      playingCameras: {}, // cameraId -> { url, cameraName }
      singlePlayLoading: {},

      // 定时刷新
      autoRefreshTimer: null
    }
  },
  computed: {
    // 根据播放数量动态计算网格布局
    getGridClass() {
      return () => {
        const count = Object.keys(this.playingCameras).length
        if (count === 1) return 'grid-1'
        if (count === 2) return 'grid-2'
        if (count <= 4) return 'grid-4'
        if (count <= 6) return 'grid-6'
        if (count <= 9) return 'grid-9'
        return 'grid-12'
      }
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
    // 获取相机名称
    getCameraName(cameraId) {
      const camera = this.cameras.find(c => c.cameraId === cameraId)
      return camera ? (camera.name || cameraId) : cameraId
    },
    
    // ==================== 车辆管理 ====================
    async loadVehicles() {
      this.vehicleLoading = true
      try {
        const response = await getAllVehicles()
        if (response.code === 200) {
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
        if (response.code === 200) {
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
        console.log('开始批量获取播放链接', { vehicleId: this.selectedVehicle.vehicleId, cameraIds })
        
        const webrtcUrls = {}
        let successCount = 0
        let errorCount = 0

        // 使用循环方式逐个获取播放链接，避免批量API问题
        for (const camera of this.selectedCameras) {
          try {
            console.log(`获取相机 ${camera.cameraId} 播放链接...`)
            const response = await getVehicleCameraWebRTCUrl(this.selectedVehicle.vehicleId, camera.cameraId)
            console.log(`相机 ${camera.cameraId} API响应:`, response)
            
            if (!response) {
              console.warn(`相机 ${camera.cameraId} 响应为空`)
              errorCount++
              continue
            }
            
            if (response.code === 200 && response.data) {
              webrtcUrls[camera.cameraId] = response.data
              successCount++
              console.log(`相机 ${camera.cameraId} 播放链接获取成功:`, response.data)
            } else {
              console.warn(`相机 ${camera.cameraId} 播放链接获取失败:`, response.msg)
              errorCount++
            }
          } catch (error) {
            console.error(`相机 ${camera.cameraId} 播放链接获取异常:`, error)
            errorCount++
          }
        }

        console.log('播放链接获取完成:', { successCount, errorCount, webrtcUrls })

        // 创建播放窗口 - 直接设置URL到playingCameras
        for (const camera of this.selectedCameras) {
          const cameraId = camera.cameraId
          const url = webrtcUrls[cameraId]
          
          if (url) {
            console.log(`设置相机 ${cameraId} 播放信息:`, url)
            
            this.$set(this.playingCameras, cameraId, {
              url: url,
              cameraName: camera.name || camera.cameraId
            })
          }
        }

        // 等待DOM更新后播放
        await this.$nextTick()
        
        // 触发所有播放器播放
        for (const camera of this.selectedCameras) {
          const cameraId = camera.cameraId
          const url = webrtcUrls[cameraId]
          
          if (url) {
            const playerRef = this.$refs[`rtcPlayer_${cameraId}`]
            if (playerRef && playerRef[0]) {
              console.log(`开始播放相机 ${cameraId}`)
              playerRef[0].play(url)
            }
          }
        }
        
        if (successCount > 0) {
          this.$message.success(`成功打开 ${successCount} 个播放器${errorCount > 0 ? `，${errorCount} 个失败` : ''}`)
        } else {
          this.$message.error('所有相机播放失败')
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
        const response = await getVehicleCameraWebRTCUrl(this.selectedVehicle.vehicleId, camera.cameraId)
        
        if (!response) {
          this.$message.error('未找到相机推流或推流未启动')
          return
        }
        
        if (response.code === 200 && response.data) {
          const url = response.data
          if (url) {
            console.log(`单个播放相机 ${camera.cameraId}:`, url)
            
            // 添加到播放列表
            this.$set(this.playingCameras, camera.cameraId, {
              url: url,
              cameraName: camera.name || camera.cameraId
            })
            
            // 等待DOM更新
            await this.$nextTick()
            
            // 播放
            const playerRef = this.$refs[`rtcPlayer_${camera.cameraId}`]
            if (playerRef && playerRef[0]) {
              playerRef[0].play(url)
              this.$message.success(`开始播放相机 ${camera.cameraId}`)
            } else {
              console.error('找不到播放器组件引用:', `rtcPlayer_${camera.cameraId}`)
              this.$message.error('播放器组件未就绪，请重试')
              this.$delete(this.playingCameras, camera.cameraId)
            }
          } else {
            this.$message.error('播放链接为空')
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
      console.log(`停止相机播放: ${cameraId}`)
      
      // 停止播放�?
      const playerRef = this.$refs[`rtcPlayer_${cameraId}`]
      if (playerRef && playerRef[0]) {
        playerRef[0].pause()
      }
      
      // 从播放列表中移除
      this.$delete(this.playingCameras, cameraId)
      
      this.$message.success(`停止播放相机: ${cameraId}`)
    },

    batchStopCameras() {
      const cameraIds = Object.keys(this.playingCameras)
      cameraIds.forEach(cameraId => {
        this.stopSingleCamera(cameraId)
      })
      this.$message.success('已停止所有播放')
    },

    stopAllCameras() {
      // 停止所有播放器
      Object.keys(this.playingCameras).forEach((cameraId) => {
        const playerRef = this.$refs[`rtcPlayer_${cameraId}`]
        if (playerRef && playerRef[0]) {
          playerRef[0].pause()
        }
      })
      
      this.playingCameras = {}
      console.log('停止所有相机播放')
    },

    // ==================== 定时刷新 ====================
    startAutoRefresh() {
      // 10秒刷新一次车辆状态
      this.autoRefreshTimer = setInterval(() => {
        this.loadVehicles()
      }, 5000)
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

/* ==================== 右侧主面==================== */
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

/* ==================== 视频网格播放区域 ==================== */
.video-grid-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #000;
  z-index: 2000;
  display: flex;
  flex-direction: column;
}

.video-grid-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  background-color: #1f1f1f;
  color: #fff;
  border-bottom: 1px solid #333;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 500;
}

.header-left i {
  font-size: 20px;
  color: #409eff;
}

.video-grid-container {
  flex: 1;
  display: grid;
  gap: 8px;
  padding: 8px;
  overflow: auto;
}

/* 网格布局 - 根据数量自动调整 */
.grid-1 {
  grid-template-columns: 1fr;
  grid-template-rows: 1fr;
}

.grid-2 {
  grid-template-columns: repeat(2, 1fr);
  grid-template-rows: 1fr;
}

.grid-4 {
  grid-template-columns: repeat(2, 1fr);
  grid-template-rows: repeat(2, 1fr);
}

.grid-6 {
  grid-template-columns: repeat(3, 1fr);
  grid-template-rows: repeat(2, 1fr);
}

.grid-9 {
  grid-template-columns: repeat(3, 1fr);
  grid-template-rows: repeat(3, 1fr);
}

.grid-12 {
  grid-template-columns: repeat(4, 1fr);
  grid-template-rows: repeat(3, 1fr);
}

.video-grid-item {
  background-color: #1a1a1a;
  border-radius: 4px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  border: 1px solid #333;
  transition: border-color 0.3s;
}

.video-grid-item:hover {
  border-color: #409eff;
}

.video-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background-color: #2c3e50;
  color: #fff;
  min-height: 40px;
}

.video-item-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.video-item-title i {
  color: #409eff;
  flex-shrink: 0;
}

.video-item-content {
  flex: 1;
  position: relative;
  background-color: #000;
  min-height: 0;
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
  
  .grid-2, .grid-4, .grid-6, .grid-9, .grid-12 {
    grid-template-columns: 1fr !important;
  }
}

/* ==================== 滚动条样式==================== */
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
