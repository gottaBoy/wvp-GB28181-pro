<template>
  <div class="vehicle-camera-monitor">
    <!-- 分栏布局 -->
    <el-container>
      <!-- 左侧车辆列表 -->
      <el-aside width="300px" class="left-panel">
        <el-card class="vehicle-panel" shadow="never">
          <template #header>
            <div class="panel-header">
              <span style="font-size: 13px;">车辆列表</span>
              <el-button 
                type="primary" 
                size="mini"
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
                size="mini"
                icon="el-icon-video-play"
                :disabled="selectedCameras.length === 0"
                @click="batchPlayCameras"
                :loading="batchPlaying">
                播放选中 ({{ selectedCameras.length }})
              </el-button>
              <el-button 
                type="warning"
                size="mini"
                icon="el-icon-video-pause"
                :disabled="Object.keys(playingCameras).length === 0"
                @click="batchStopCameras">
                停止全部
              </el-button>
              <el-button 
                type="primary"
                size="mini"
                icon="el-icon-refresh"
                @click="refreshCameras"
                :loading="cameraLoading">
                刷新
              </el-button>
            </el-button-group>

            <el-dropdown @command="handleBatchCommand" size="mini" style="margin-left: 8px;">
              <el-button type="info" size="mini">
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
              <span style="font-size: 13px;">相机列表 ({{ cameras.length }})</span>
              <div class="selection-info">
                已选择: {{ selectedCameras.length }} / {{ cameras.length }}
              </div>
            </div>
          </template>

          <!-- 相机表格 -->
          <el-table
            ref="cameraTable"
            :data="cameras"
            size="mini"
            v-loading="cameraLoading"
            @selection-change="handleSelectionChange"
            style="width: 100%">
            
            <el-table-column type="selection" width="55" align="center"></el-table-column>
            
            <el-table-column prop="cameraId" label="相机ID" width="120">
              <template #default="scope">
                <el-tag size="mini" effect="plain">{{ scope.row.cameraId }}</el-tag>
              </template>
            </el-table-column>
            
            <el-table-column prop="name" label="相机名称" min-width="130">
              <template #default="scope">
                <div class="camera-name">
                  <i class="el-icon-video-camera" style="color: #409EFF; margin-right: 4px;"></i>
                  {{ scope.row.name || '未命名相机' }}
                </div>
              </template>
            </el-table-column>
            
            <el-table-column prop="description" label="描述" min-width="150" show-overflow-tooltip>
              <template #default="scope">
                {{ scope.row.description || '-' }}
              </template>
            </el-table-column>
            
            <el-table-column label="状态" width="80" align="center">
              <template #default="scope">
                <el-tag 
                  :type="scope.row.status === 'active' ? 'success' : 'info'"
                  size="mini">
                  {{ scope.row.status === 'active' ? '活跃' : '非活跃' }}
                </el-tag>
              </template>
            </el-table-column>
            
            <el-table-column label="推流状态" width="90" align="center">
              <template #default="scope">
                <el-tag 
                  :type="scope.row.pushing ? 'success' : 'info'"
                  size="mini">
                  {{ scope.row.pushing ? '推流中' : '未推流' }}
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column label="播放状态" width="90" align="center">
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
            
            <el-table-column label="操作" width="80" align="center">
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
            size="mini"
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
          
          <div class="video-item-wrapper">
            <div class="video-item-header">
              <span class="video-item-title">
                <i class="el-icon-video-camera"></i>
                {{ videoInfo.cameraName }}
              </span>
              <div class="header-actions">
                <el-button 
                  type="text" 
                  icon="el-icon-refresh"
                  size="mini"
                  class="reload-btn"
                  @click="reloadSingleCamera(cameraId)"
                  title="重新加载">
                </el-button>
                <el-button 
                  type="text" 
                  icon="el-icon-close"
                  size="mini"
                  class="close-btn"
                  @click="stopSingleCamera(cameraId)"
                  title="关闭">
                </el-button>
              </div>
            </div>
            
            <div class="video-item-content">
              <!-- 加载状态 -->
              <div v-if="videoInfo.loading" class="video-loading">
                <i class="el-icon-loading"></i>
                <p>正在获取播放链接...</p>
              </div>
              
              <!-- 错误状态 -->
              <div v-else-if="videoInfo.error" class="video-error">
                <i class="el-icon-warning-outline"></i>
                <p>{{ videoInfo.error }}</p>
                <el-button 
                  type="primary" 
                  size="mini"
                  @click="retrySingleCamera(cameraId)">
                  重试
                </el-button>
              </div>
              
              <!-- 播放器 -->
              <vehicle-rtc-player
                v-else-if="videoInfo.url"
                :ref="'rtcPlayer_' + cameraId"
                :player-id="'video_' + cameraId"
                :video-url="videoInfo.url"
                :has-audio="true"
                style="width: 100%; height: 100%;"
              />
              
              <!-- 等待推流 -->
              <div v-else class="video-waiting">
                <i class="el-icon-video-camera-solid"></i>
                <p>等待推流上线...</p>
                <el-button 
                  type="success" 
                  size="mini"
                  @click="retrySingleCamera(cameraId)">
                  检查推流
                </el-button>
              </div>
            </div>
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
  batchGetVehicleCameraWebRTCUrls,
  directStartCameraStream,
  directStopCameraStream
} from '@/api/vehicle'
import VehicleRtcPlayer from './VehicleRtcPlayer.vue'
import vehicleLogger from '@/utils/vehicleLogger'

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
        if (count <= 12) return 'grid-12'
        if (count <= 16) return 'grid-16'
        if (count <= 20) return 'grid-20'
        return 'grid-25' // 最多5x5
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
        let successCount = 0
        let pendingCount = 0
        let errorCount = 0

        // 先创建所有播放窗口（即使还没有URL）
        for (const camera of this.selectedCameras) {
          const cameraId = camera.cameraId
          
          // 先创建窗口占位，URL设置为null
          this.$set(this.playingCameras, cameraId, {
            url: null,
            cameraName: camera.name || camera.cameraId,
            loading: true,
            error: null
          })
          pendingCount++
        }

        // 等待DOM更新，创建所有播放器组件
        await this.$nextTick()

        // 逐个获取播放链接并尝试播放
        for (const camera of this.selectedCameras) {
          const cameraId = camera.cameraId
          
          try {
            console.log(`获取相机 ${cameraId} 播放链接...`)
            const response = await getVehicleCameraWebRTCUrl(this.selectedVehicle.vehicleId, cameraId)
            console.log(`相机 ${cameraId} API响应:`, response)
            
            if (!response) {
              console.warn(`相机 ${cameraId} 响应为空，保留窗口等待推流`)
              this.$set(this.playingCameras[cameraId], 'loading', false)
              this.$set(this.playingCameras[cameraId], 'error', '等待推流上线...')
              errorCount++
              pendingCount--
              continue
            }
            
            if (response.code === 200 && response.data) {
              const url = response.data
              console.log(`相机 ${cameraId} 播放链接获取成功:`, url)
              
              // 更新URL
              this.$set(this.playingCameras[cameraId], 'url', url)
              this.$set(this.playingCameras[cameraId], 'loading', false)
              this.$set(this.playingCameras[cameraId], 'error', null)
              
              // 尝试播放
              await this.$nextTick()
              const playerRef = this.$refs[`rtcPlayer_${cameraId}`]
              if (playerRef && playerRef[0]) {
                playerRef[0].play(url)
                successCount++
              }
            } else {
              console.warn(`相机 ${cameraId} 播放链接获取失败: ${response.msg}，保留窗口`)
              this.$set(this.playingCameras[cameraId], 'loading', false)
              this.$set(this.playingCameras[cameraId], 'error', response.msg || '获取播放链接失败')
              errorCount++
            }
            
            pendingCount--
          } catch (error) {
            console.error(`相机 ${cameraId} 播放链接获取异常:`, error)
            this.$set(this.playingCameras[cameraId], 'loading', false)
            this.$set(this.playingCameras[cameraId], 'error', error.message || '获取播放链接异常')
            errorCount++
            pendingCount--
          }
        }

        console.log('批量播放完成:', { successCount, errorCount, totalWindows: Object.keys(this.playingCameras).length })

        // 统一显示结果提示
        const totalCount = this.selectedCameras.length
        if (successCount === totalCount) {
          this.$message.success(`成功播放 ${successCount} 个相机`)
        } else if (successCount > 0) {
          this.$message({
            message: `成功播放 ${successCount}/${totalCount} 个相机`,
            type: 'success',
            duration: 3000
          })
        } else {
          this.$message({
            message: `已创建 ${totalCount} 个播放窗口，等待推流上线`,
            type: 'warning',
            duration: 3000
          })
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
      const startTime = Date.now()
      
      try {
        vehicleLogger.logCommandSend(this.selectedVehicle.vehicleId, 'GET_PLAY_URL', {
          cameraId: camera.cameraId
        })
        
        const response = await getVehicleCameraWebRTCUrl(this.selectedVehicle.vehicleId, camera.cameraId)
        const duration = Date.now() - startTime
        
        if (!response) {
          vehicleLogger.logPlayError(this.selectedVehicle.vehicleId, camera.cameraId, new Error('未收到响应'))
          // 不显示错误提示，由 request.js 拦截器统一处理
          return
        }
        
        if (response.code === 200 && response.data) {
          const url = response.data
          
          vehicleLogger.logPlayUrlGenerated(this.selectedVehicle.vehicleId, camera.cameraId, url, duration)
          
          if (url) {
            console.log(`单个播放相机 ${camera.cameraId}:`, url)
            
            // 添加到播放列表
            this.$set(this.playingCameras, camera.cameraId, {
              url: url,
              cameraName: camera.name || camera.cameraId,
              loading: false,
              error: null
            })
            
            // 等待DOM更新
            await this.$nextTick()
            
            // 播放
            const playerRef = this.$refs[`rtcPlayer_${camera.cameraId}`]
            if (playerRef && playerRef[0]) {
              playerRef[0].play(url)
              vehicleLogger.logPlayStart(this.selectedVehicle.vehicleId, camera.cameraId, url)
              this.$message.success(`开始播放相机 ${camera.cameraId}`)
            } else {
              console.error('找不到播放器组件引用:', `rtcPlayer_${camera.cameraId}`)
              vehicleLogger.logPlayError(this.selectedVehicle.vehicleId, camera.cameraId, new Error('播放器组件未就绪'))
              this.$message.error('播放器组件未就绪，请重试')
              this.$delete(this.playingCameras, camera.cameraId)
            }
          } else {
            vehicleLogger.logPlayError(this.selectedVehicle.vehicleId, camera.cameraId, new Error('播放链接为空'))
            // 不显示错误提示，由 request.js 拦截器统一处理
          }
        } else {
          vehicleLogger.logPlayError(this.selectedVehicle.vehicleId, camera.cameraId, new Error(response.msg || '未知错误'))
          // 不显示错误提示，由 request.js 拦截器统一处理
        }
      } catch (error) {
        const duration = Date.now() - startTime
        console.error('播放相机失败:', error)
        vehicleLogger.logPlayError(this.selectedVehicle.vehicleId, camera.cameraId, error, { duration })
        // 不显示错误提示，由 request.js 拦截器统一处理
      } finally {
        this.$set(this.singlePlayLoading, camera.cameraId, false)
      }
    },

    stopSingleCamera(cameraId) {
      console.log(`停止相机播放: ${cameraId}`)
      
      // 停止播放器
      const playerRef = this.$refs[`rtcPlayer_${cameraId}`]
      if (playerRef && playerRef[0]) {
        playerRef[0].pause()
      }
      
      // 记录日志
      if (this.selectedVehicle) {
        vehicleLogger.logPlayStop(this.selectedVehicle.vehicleId, cameraId, null, {
          playUrl: this.playingCameras[cameraId]?.url
        })
      }
      
      // 从播放列表中移除
      this.$delete(this.playingCameras, cameraId)
      
      this.$message.success(`停止播放相机: ${cameraId}`)
    },

    async retrySingleCamera(cameraId) {
      if (!this.selectedVehicle) {
        this.$message.error('未选择车辆')
        return
      }

      console.log(`重试获取相机 ${cameraId} 播放链接`)
      
      // 设置加载状态
      this.$set(this.playingCameras[cameraId], 'loading', true)
      this.$set(this.playingCameras[cameraId], 'error', null)
      
      try {
        const response = await getVehicleCameraWebRTCUrl(this.selectedVehicle.vehicleId, cameraId)
        
        if (!response) {
          this.$set(this.playingCameras[cameraId], 'loading', false)
          this.$set(this.playingCameras[cameraId], 'error', '未收到响应，请稍后重试')
          return
        }
        
        if (response.code === 200 && response.data) {
          const url = response.data
          console.log(`相机 ${cameraId} 重试成功，播放链接:`, url)
          
          // 更新URL
          this.$set(this.playingCameras[cameraId], 'url', url)
          this.$set(this.playingCameras[cameraId], 'loading', false)
          this.$set(this.playingCameras[cameraId], 'error', null)
          
          // 尝试播放
          await this.$nextTick()
          const playerRef = this.$refs[`rtcPlayer_${cameraId}`]
          if (playerRef && playerRef[0]) {
            playerRef[0].play(url)
            // 只有重试成功时才显示成功提示
            this.$message.success(`相机 ${cameraId} 播放成功`)
          }
        } else {
          this.$set(this.playingCameras[cameraId], 'loading', false)
          this.$set(this.playingCameras[cameraId], 'error', response.msg || '获取播放链接失败')
          // 重试失败不显示错误提示，只在窗口内显示
        }
      } catch (error) {
        console.error(`重试相机 ${cameraId} 失败:`, error)
        this.$set(this.playingCameras[cameraId], 'loading', false)
        this.$set(this.playingCameras[cameraId], 'error', error.message || '重试失败')
        // 异常也不显示提示，只在窗口内显示
      }
    },

    async reloadSingleCamera(cameraId) {
      if (!this.selectedVehicle) {
        this.$message.error('未选择车辆')
        return
      }

      console.log(`重新加载相机 ${cameraId}`)
      
      // 先停止当前播放
      const playerRef = this.$refs[`rtcPlayer_${cameraId}`]
      if (playerRef && playerRef[0]) {
        playerRef[0].pause()
      }
      
      // 设置加载状态
      this.$set(this.playingCameras[cameraId], 'loading', true)
      this.$set(this.playingCameras[cameraId], 'error', null)
      this.$set(this.playingCameras[cameraId], 'url', null)
      
      try {
        const response = await getVehicleCameraWebRTCUrl(this.selectedVehicle.vehicleId, cameraId)
        
        if (!response) {
          this.$set(this.playingCameras[cameraId], 'loading', false)
          this.$set(this.playingCameras[cameraId], 'error', '未收到响应，请稍后重试')
          return
        }
        
        if (response.code === 200 && response.data) {
          const url = response.data
          console.log(`相机 ${cameraId} 重新加载成功，播放链接:`, url)
          
          // 更新URL
          this.$set(this.playingCameras[cameraId], 'url', url)
          this.$set(this.playingCameras[cameraId], 'loading', false)
          this.$set(this.playingCameras[cameraId], 'error', null)
          
          // 尝试播放
          await this.$nextTick()
          const newPlayerRef = this.$refs[`rtcPlayer_${cameraId}`]
          if (newPlayerRef && newPlayerRef[0]) {
            newPlayerRef[0].play(url)
            this.$message.success(`相机 ${cameraId} 重新加载成功`)
          }
        } else {
          this.$set(this.playingCameras[cameraId], 'loading', false)
          this.$set(this.playingCameras[cameraId], 'error', response.msg || '获取播放链接失败')
        }
      } catch (error) {
        console.error(`重新加载相机 ${cameraId} 失败:`, error)
        this.$set(this.playingCameras[cameraId], 'loading', false)
        this.$set(this.playingCameras[cameraId], 'error', error.message || '重新加载失败')
      }
    },

    batchStopCameras() {
      const cameraIds = Object.keys(this.playingCameras)
      const count = cameraIds.length
      
      cameraIds.forEach(cameraId => {
        // 停止播放器（不显示单个提示）
        const playerRef = this.$refs[`rtcPlayer_${cameraId}`]
        if (playerRef && playerRef[0]) {
          playerRef[0].pause()
        }
        
        // 记录日志
        if (this.selectedVehicle) {
          vehicleLogger.logPlayStop(this.selectedVehicle.vehicleId, cameraId, null, {
            playUrl: this.playingCameras[cameraId]?.url
          })
        }
        
        // 从播放列表中移除
        this.$delete(this.playingCameras, cameraId)
      })
      
      // 统一提示
      this.$message.success(`已停止 ${count} 个相机播放`)
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
        // if (this.selectedVehicle) {
        //   this.loadVehicleCameras(this.selectedVehicle.vehicleId)
        // }
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
  padding: 8px 10px;
  margin-bottom: 6px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
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
  font-size: 13px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 2px;
}

.vehicle-id {
  font-size: 11px;
  color: #909399;
  margin-bottom: 2px;
}

.vehicle-ip {
  font-size: 10px;
  color: #c0c4cc;
}

.vehicle-status {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.camera-count {
  font-size: 10px;
  color: #909399;
  margin-top: 3px;
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
  padding: 8px 16px;
  background-color: #fff;
  border-bottom: 1px solid #e4e7ed;
  margin-bottom: 0;
}

.toolbar-left .current-vehicle {
  font-size: 14px;
  font-weight: 600;
  color: #409eff;
}

.toolbar-left .no-vehicle {
  font-size: 13px;
  color: #909399;
}

.camera-panel {
  flex: 1;
  margin: 12px;
  margin-top: 0;
  border: none;
}

.selection-info {
  font-size: 11px;
  color: #909399;
}

.camera-name {
  display: flex;
  align-items: center;
}

.text-muted {
  color: #c0c4cc;
  font-size: 11px;
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
  padding: 10px 16px;
  background-color: #1f1f1f;
  color: #fff;
  border-bottom: 1px solid #333;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 500;
}

.header-left i {
  font-size: 18px;
  color: #409eff;
}

.video-grid-container {
  flex: 1;
  display: grid;
  gap: 6px;
  padding: 6px;
  overflow: auto;
  align-content: start; /* 从顶部开始对齐 */
  align-items: stretch; /* 拉伸以填充网格单元格 */
}

/* 确保所有网格布局都有明确的高度计算 */
.grid-1,
.grid-2,
.grid-4,
.grid-6,
.grid-9,
.grid-12,
.grid-16,
.grid-20,
.grid-25 {
  grid-auto-rows: 1fr; /* 确保所有行高度相等 */
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

.grid-16 {
  grid-template-columns: repeat(4, 1fr);
  grid-template-rows: repeat(4, 1fr);
}

.grid-20 {
  grid-template-columns: repeat(5, 1fr);
  grid-template-rows: repeat(4, 1fr);
}

.grid-25 {
  grid-template-columns: repeat(5, 1fr);
  grid-template-rows: repeat(5, 1fr);
}

.video-grid-item {
  background-color: #1a1a1a;
  border-radius: 4px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  border: 1px solid #333;
  transition: border-color 0.3s;
  min-height: 0;
  min-width: 0;
  position: relative;
}

.video-grid-item:hover {
  border-color: #409eff;
}

/* 新增wrapper包裹层，确保内容在边界内 */
.video-item-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.video-item-header {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 6px 10px;
  background-color: rgba(44, 62, 80, 0);
  color: #fff;
  z-index: 10;
  transition: background-color 0.3s, opacity 0.3s;
  opacity: 0;
  border-radius: 0 0 8px 8px;
}

.video-grid-item:hover .video-item-header {
  background-color: rgba(44, 62, 80, 0.9);
  opacity: 1;
}

.video-item-title {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.video-item-title i {
  color: #409eff;
  flex-shrink: 0;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
}

.reload-btn,
.close-btn {
  color: #fff !important;
  padding: 4px !important;
  font-size: 14px;
}

.reload-btn:hover {
  color: #409eff !important;
  background-color: rgba(255, 255, 255, 0.1) !important;
  border-radius: 4px;
}

.close-btn:hover {
  color: #f56c6c !important;
  background-color: rgba(255, 255, 255, 0.1) !important;
  border-radius: 4px;
}

.video-item-content {
  flex: 1;
  position: relative;
  background-color: #000;
  min-height: 0; /* 允许flex收缩 */
  overflow: hidden; /* 确保内容不溢出 */
  width: 100%;
  height: 100%;
}

/* 视频加载状态 */
.video-loading,
.video-error,
.video-waiting {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #fff;
  text-align: center;
  padding: 20px;
}

.video-loading i {
  font-size: 36px;
  color: #409eff;
  margin-bottom: 12px;
}

.video-error i {
  font-size: 36px;
  color: #f56c6c;
  margin-bottom: 12px;
}

.video-waiting i {
  font-size: 36px;
  color: #e6a23c;
  margin-bottom: 12px;
}

.video-loading p,
.video-error p,
.video-waiting p {
  font-size: 13px;
  margin: 0 0 12px 0;
  color: #ddd;
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
