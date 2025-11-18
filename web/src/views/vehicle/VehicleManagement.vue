<template>
  <div class="vehicle-management">
    <!-- 车辆列表 -->
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>车辆管理</span>
          <el-button type="primary" @click="refreshVehicles">刷新列表</el-button>
        </div>
      </template>
      
      <el-table 
        :data="vehicles" 
        v-loading="loading"
        @row-click="handleRowClick"
        style="width: 100%">
        <el-table-column prop="vehicleId" label="车辆ID" width="120"></el-table-column>
        <el-table-column prop="vehicleName" label="车辆名称" width="150"></el-table-column>
        <el-table-column prop="ipAddress" label="IP地址" width="140"></el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="scope">
            <el-tag 
              :type="scope.row.status === 'online' ? 'success' : 'danger'"
              size="small">
              {{ scope.row.status === 'online' ? '在线' : '离线' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastHeartbeat" label="最后心跳" width="160"></el-table-column>
        <el-table-column label="相机数量" width="80">
          <template #default="scope">
            {{ scope.row.cameras ? scope.row.cameras.length : 0 }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="scope">
            <el-button 
              type="primary" 
              size="small"
              @click.stop="showVehicleDetail(scope.row)"
              >详情</el-button>
            <el-button 
              type="success" 
              size="small"
              @click.stop="checkConnection(scope.row)"
              :loading="connectionChecking[scope.row.vehicleId]"
              >连接测试</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 车辆详情对话框 -->
    <el-drawer
      v-model="detailDrawerVisible"
      :title="currentVehicle ? `车辆详情 - ${currentVehicle.vehicleName}` : '车辆详情'"
      size="60%"
      direction="rtl">
      
      <div v-if="currentVehicle" class="vehicle-detail">
        <!-- 基本信息 -->
        <el-card class="detail-card" shadow="never">
          <template #header>
            <span>基本信息</span>
            <el-tag 
              :type="currentVehicle.status === 'online' ? 'success' : 'danger'"
              style="margin-left: 10px;">
              {{ currentVehicle.status === 'online' ? '在线' : '离线' }}
            </el-tag>
          </template>
          
          <el-descriptions :column="2" border>
            <el-descriptions-item label="车辆ID">{{ currentVehicle.vehicleId }}</el-descriptions-item>
            <el-descriptions-item label="车辆名称">{{ currentVehicle.vehicleName }}</el-descriptions-item>
            <el-descriptions-item label="IP地址">{{ currentVehicle.ipAddress }}</el-descriptions-item>
            <el-descriptions-item label="最后心跳">{{ currentVehicle.lastHeartbeat }}</el-descriptions-item>
            <el-descriptions-item label="注册时间">{{ currentVehicle.registerTime }}</el-descriptions-item>
            <el-descriptions-item label="更新时间">{{ currentVehicle.updateTime }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 相机列表 -->
        <el-card class="detail-card" shadow="never">
          <template #header>
            <div class="camera-header">
              <span>相机列表 ({{ vehicleCameras.length }})</span>
              <div>
                <el-button 
                  type="success" 
                  size="small"
                  @click="batchSubscribe"
                  :disabled="!hasInactiveCameras"
                  >全部订阅</el-button>
                <el-button 
                  type="warning" 
                  size="small"
                  @click="batchUnsubscribe"
                  :disabled="!hasActiveCameras"
                  >全部取消</el-button>
                <el-button 
                  type="info" 
                  size="small"
                  @click="refreshCameras"
                  >刷新状态</el-button>
              </div>
            </div>
          </template>
          
          <el-table 
            :data="vehicleCameras" 
            v-loading="camerasLoading"
            style="width: 100%">
            <el-table-column type="selection" width="55"></el-table-column>
            <el-table-column prop="cameraId" label="相机ID" width="120"></el-table-column>
            <el-table-column prop="name" label="相机名称" width="150"></el-table-column>
            <el-table-column prop="description" label="描述" show-overflow-tooltip></el-table-column>
            <el-table-column prop="status" label="状态" width="80">
              <template #default="scope">
                <el-tag 
                  :type="scope.row.status === 'active' ? 'success' : 'info'"
                  size="small">
                  {{ scope.row.status === 'active' ? '活跃' : '非活跃' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="推流状态" width="80">
              <template #default="scope">
                <el-tag 
                  :type="scope.row.pushing ? 'success' : 'info'"
                  size="small">
                  {{ scope.row.pushing ? '推流中' : '未推流' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="280">
              <template #default="scope">
                <div class="button-group">
                  <!-- 订阅操作 -->
                  <el-button 
                    v-if="scope.row.status === 'inactive'"
                    type="success" 
                    size="small"
                    @click="subscribeCamera(scope.row.cameraId)"
                    :loading="operationLoading[scope.row.cameraId]"
                    >订阅</el-button>
                  <el-button 
                    v-else
                    type="warning" 
                    size="small"
                    @click="unsubscribeCamera(scope.row.cameraId)"
                    :loading="operationLoading[scope.row.cameraId]"
                    >取消订阅</el-button>
                  
                  <!-- WVP推流操作 -->
                  <el-button 
                    v-if="scope.row.pushing"
                    type="danger" 
                    size="small"
                    @click="stopStream(scope.row.cameraId)"
                    :loading="streamLoading[scope.row.cameraId]"
                    >停止推流</el-button>
                  <el-button 
                    v-else
                    type="primary" 
                    size="small"
                    @click="startStream(scope.row.cameraId)"
                    :loading="streamLoading[scope.row.cameraId]"
                    >开始推流</el-button>

                  <!-- 播放按钮 - 只在推流中时显示 -->
                  <el-button 
                    v-if="scope.row.pushing"
                    type="success" 
                    size="small"
                    @click="playCamera(scope.row.cameraId)"
                    :loading="playLoading[scope.row.cameraId]"
                    >播放</el-button>

                  <!-- 直接调用车辆端API -->
                  <el-dropdown 
                    @command="(command) => handleDirectCommand(command, scope.row.cameraId)"
                    size="small">
                    <el-button type="info" size="small">
                      直接控制<i class="el-icon-arrow-down el-icon--right"></i>
                    </el-button>
                    <el-dropdown-menu slot="dropdown">
                      <el-dropdown-item command="direct-start">直接启动</el-dropdown-item>
                      <el-dropdown-item command="direct-stop">直接停止</el-dropdown-item>
                    </el-dropdown-menu>
                  </el-dropdown>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </div>
    </el-drawer>

    <!-- WebRTC播放器对话框 -->
    <el-dialog
      title="WebRTC播放器"
      :visible.sync="playDialogVisible"
      :close-on-click-modal="false"
      width="80%"
      top="5vh"
      @close="closePlayer">
      <div v-if="playDialogVisible && playUrl" class="player-container">
        <rtc-player
          ref="rtcPlayer"
          :visible.sync="playDialogVisible"
          :video-url="playUrl"
          :error="playError"
          :message="playError"
          style="width: 100%; height: 60vh;"
          :has-audio="true"
          fluent
          autoplay
          live
        />
      </div>
      <div v-if="!playUrl && !playLoading" class="no-stream-tip">
        <el-alert
          title="暂无视频流"
          description="请确保相机已启动推流且推流状态正常"
          type="warning"
          :closable="false">
        </el-alert>
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button @click="closePlayer">关闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { 
  getAllVehicles, 
  getVehicle, 
  getVehicleCameras,
  subscribeVehicleCameras,
  unsubscribeVehicleCameras,
  getVehicleSubscribedCameras,
  checkVehicleConnection,
  startCameraStream,
  stopCameraStream,
  directStartCameraStream,
  directStopCameraStream,
  getVehicleCameraWebRTCUrl
} from '@/api/vehicle'
import rtcPlayer from '@/views/common/rtcPlayer.vue'

export default {
  name: 'VehicleManagement',
  components: {
    rtcPlayer
  },
  data() {
    return {
      vehicles: [],
      loading: false,
      detailDrawerVisible: false,
      currentVehicle: null,
      vehicleCameras: [],
      camerasLoading: false,
      connectionChecking: {},
      operationLoading: {},
      streamLoading: {},
      directLoading: {},
      playLoading: {},
      playDialogVisible: false,
      playUrl: '',
      playError: '',
      currentPlayingCamera: null,
      autoRefreshTimer: null
    }
  },
  computed: {
    hasActiveCameras() {
      return this.vehicleCameras.some(camera => camera.status === 'active')
    },
    hasInactiveCameras() {
      return this.vehicleCameras.some(camera => camera.status === 'inactive')
    }
  },
  mounted() {
    this.loadVehicles()
    // 启动定时刷新车辆列表，每30秒刷新一次以同步在线状态
    this.autoRefreshTimer = setInterval(() => {
      this.loadVehicles()
    }, 30000) // 30秒刷新一次
  },
  beforeDestroy() {
    // 组件销毁前清理定时器
    if (this.autoRefreshTimer) {
      clearInterval(this.autoRefreshTimer)
    }
  },
  methods: {
    async loadVehicles() {
      this.loading = true
      try {
        const response = await getAllVehicles()
        if (response.code === 0) {
          this.vehicles = response.data || []
        } else {
          this.$message.error('获取车辆列表失败: ' + response.msg)
        }
      } catch (error) {
        this.$message.error('获取车辆列表异常: ' + error.message)
      } finally {
        this.loading = false
      }
    },

    async showVehicleDetail(vehicle) {
      this.currentVehicle = vehicle
      this.detailDrawerVisible = true
      await this.loadVehicleCameras(vehicle.vehicleId)
    },

    async loadVehicleCameras(vehicleId) {
      this.camerasLoading = true
      try {
        const response = await getVehicleCameras(vehicleId)
        if (response.code === 0) {
          this.vehicleCameras = response.data || []
        } else {
          this.$message.error('获取相机列表失败: ' + response.msg)
        }
      } catch (error) {
        this.$message.error('获取相机列表异常: ' + error.message)
      } finally {
        this.camerasLoading = false
      }
    },

    async checkConnection(vehicle) {
      this.$set(this.connectionChecking, vehicle.vehicleId, true)
      try {
        const response = await checkVehicleConnection(vehicle.vehicleId)
        if (response.code === 0) {
          const connected = response.data
          this.$message({
            type: connected ? 'success' : 'warning',
            message: `车辆 ${vehicle.vehicleId} ${connected ? '连接正常' : '连接异常'}`
          })
        } else {
          this.$message.error('连接检查失败: ' + response.msg)
        }
      } catch (error) {
        this.$message.error('连接检查异常: ' + error.message)
      } finally {
        this.$set(this.connectionChecking, vehicle.vehicleId, false)
      }
    },

    async subscribeCamera(cameraId) {
      if (!this.currentVehicle) return
      
      this.$set(this.operationLoading, cameraId, true)
      try {
        const response = await subscribeVehicleCameras(this.currentVehicle.vehicleId, [cameraId])
        if (response.code === 0) {
          this.$message.success('订阅成功')
          await this.loadVehicleCameras(this.currentVehicle.vehicleId)
        } else {
          this.$message.error('订阅失败: ' + response.msg)
        }
      } catch (error) {
        this.$message.error('订阅异常: ' + error.message)
      } finally {
        this.$set(this.operationLoading, cameraId, false)
      }
    },

    async unsubscribeCamera(cameraId) {
      if (!this.currentVehicle) return
      
      this.$set(this.operationLoading, cameraId, true)
      try {
        const response = await unsubscribeVehicleCameras(this.currentVehicle.vehicleId, [cameraId])
        if (response.code === 0) {
          this.$message.success('取消订阅成功')
          await this.loadVehicleCameras(this.currentVehicle.vehicleId)
        } else {
          this.$message.error('取消订阅失败: ' + response.msg)
        }
      } catch (error) {
        this.$message.error('取消订阅异常: ' + error.message)
      } finally {
        this.$set(this.operationLoading, cameraId, false)
      }
    },

    async batchSubscribe() {
      if (!this.currentVehicle) return
      
      const inactiveCameras = this.vehicleCameras
        .filter(camera => camera.status === 'inactive')
        .map(camera => camera.cameraId)
      
      if (inactiveCameras.length === 0) {
        this.$message.info('没有需要订阅的相机')
        return
      }

      try {
        const response = await subscribeVehicleCameras(this.currentVehicle.vehicleId, inactiveCameras)
        if (response.code === 0) {
          this.$message.success(`批量订阅成功 (${inactiveCameras.length} 个相机)`)
          await this.loadVehicleCameras(this.currentVehicle.vehicleId)
        } else {
          this.$message.error('批量订阅失败: ' + response.msg)
        }
      } catch (error) {
        this.$message.error('批量订阅异常: ' + error.message)
      }
    },

    async batchUnsubscribe() {
      if (!this.currentVehicle) return
      
      const activeCameras = this.vehicleCameras
        .filter(camera => camera.status === 'active')
        .map(camera => camera.cameraId)
      
      if (activeCameras.length === 0) {
        this.$message.info('没有需要取消订阅的相机')
        return
      }

      try {
        const response = await unsubscribeVehicleCameras(this.currentVehicle.vehicleId, activeCameras)
        if (response.code === 0) {
          this.$message.success(`批量取消订阅成功 (${activeCameras.length} 个相机)`)
          await this.loadVehicleCameras(this.currentVehicle.vehicleId)
        } else {
          this.$message.error('批量取消订阅失败: ' + response.msg)
        }
      } catch (error) {
        this.$message.error('批量取消订阅异常: ' + error.message)
      }
    },

    async startStream(cameraId) {
      if (!this.currentVehicle) return
      
      this.$set(this.streamLoading, cameraId, true)
      try {
        console.log('启动推流请求:', {
          vehicleId: this.currentVehicle.vehicleId,
          cameraId: cameraId
        })
        
        const response = await startCameraStream(this.currentVehicle.vehicleId, cameraId)
        console.log('启动推流响应:', response) // 调试日志
        console.log('响应类型:', typeof response)
        console.log('响应内容:', JSON.stringify(response, null, 2))
        
        if (response && typeof response === 'object' && 'code' in response) {
          if (response.code === 0) {
            this.$message.success('开始推流成功')
            await this.loadVehicleCameras(this.currentVehicle.vehicleId)
          } else {
            this.$message.error('开始推流失败: ' + (response.msg || '错误代码: ' + response.code))
          }
        } else {
          console.error('响应格式异常:', response)
          this.$message.error('开始推流失败: 服务器响应格式异常')
        }
      } catch (error) {
        console.error('启动推流异常详细信息:', {
          error: error,
          message: error.message,
          stack: error.stack,
          response: error.response
        })
        
        let errorMessage = '开始推流异常'
        if (error.response && error.response.data) {
          errorMessage += ': ' + (error.response.data.msg || error.response.data.message || '服务器错误')
        } else if (error.message) {
          errorMessage += ': ' + error.message
        }
        
        this.$message.error(errorMessage)
      } finally {
        this.$set(this.streamLoading, cameraId, false)
      }
    },

    async stopStream(cameraId) {
      if (!this.currentVehicle) return
      
      this.$set(this.streamLoading, cameraId, true)
      try {
        const response = await stopCameraStream(this.currentVehicle.vehicleId, cameraId)
        console.log('停止推流响应:', response) // 调试日志
        
        if (response && response.code === 0) {
          this.$message.success('停止推流成功')
          await this.loadVehicleCameras(this.currentVehicle.vehicleId)
        } else if (response && response.code !== 0) {
          this.$message.error('停止推流失败: ' + (response.msg || '未知错误'))
        } else {
          this.$message.error('停止推流失败: 服务器响应异常')
        }
      } catch (error) {
        console.error('停止推流异常:', error) // 调试日志
        this.$message.error('停止推流异常: ' + (error.message || error.toString()))
      } finally {
        this.$set(this.streamLoading, cameraId, false)
      }
    },

    handleDirectCommand(command, cameraId) {
      if (command === 'direct-start') {
        this.directStartStream(cameraId)
      } else if (command === 'direct-stop') {
        this.directStopStream(cameraId)
      }
    },

    async directStartStream(cameraId) {
      if (!this.currentVehicle) return
      
      this.$set(this.directLoading, cameraId, true)
      try {
        const response = await directStartCameraStream(this.currentVehicle.vehicleId, cameraId)
        if (response.code === 0) {
          this.$message.success(`直接启动推流成功 - 相机: ${cameraId}`)
          await this.loadVehicleCameras(this.currentVehicle.vehicleId)
        } else {
          this.$message.error('直接启动推流失败: ' + response.msg)
        }
      } catch (error) {
        this.$message.error('直接启动推流异常: ' + error.message)
      } finally {
        this.$set(this.directLoading, cameraId, false)
      }
    },

    async directStopStream(cameraId) {
      if (!this.currentVehicle) return
      
      this.$set(this.directLoading, cameraId, true)
      try {
        const response = await directStopCameraStream(this.currentVehicle.vehicleId, cameraId)
        if (response.code === 0) {
          this.$message.success(`直接停止推流成功 - 相机: ${cameraId}`)
          await this.loadVehicleCameras(this.currentVehicle.vehicleId)
        } else {
          this.$message.error('直接停止推流失败: ' + response.msg)
        }
      } catch (error) {
        this.$message.error('直接停止推流异常: ' + error.message)
      } finally {
        this.$set(this.directLoading, cameraId, false)
      }
    },

    refreshVehicles() {
      this.loadVehicles()
    },

    async refreshCameras() {
      if (this.currentVehicle) {
        await this.loadVehicleCameras(this.currentVehicle.vehicleId)
      }
    },

    handleRowClick(row) {
      this.showVehicleDetail(row)
    },

    async playCamera(cameraId) {
      if (!this.currentVehicle) return
      
      this.$set(this.playLoading, cameraId, true)
      this.playError = ''
      this.currentPlayingCamera = cameraId
      
      try {
        const response = await getVehicleCameraWebRTCUrl(this.currentVehicle.vehicleId, cameraId)
        if (response.code === 0 && response.data) {
          this.playUrl = response.data
          this.playDialogVisible = true
          this.$message.success('正在启动WebRTC播放器...')
        } else {
          this.$message.error('获取播放链接失败: ' + (response.msg || '未知错误'))
        }
      } catch (error) {
        this.$message.error('获取播放链接异常: ' + error.message)
        this.playError = error.message
      } finally {
        this.$set(this.playLoading, cameraId, false)
      }
    },

    closePlayer() {
      this.playDialogVisible = false
      this.playUrl = ''
      this.playError = ''
      this.currentPlayingCamera = null
      
      // 确保播放器正确清理
      if (this.$refs.rtcPlayer) {
        this.$refs.rtcPlayer.pause()
      }
    }
  }
}
</script>

<style scoped>
.vehicle-management {
  padding: 20px;
}

.box-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.detail-card {
  margin-bottom: 20px;
}

.camera-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.vehicle-detail {
  padding: 10px;
}

.el-table .cell {
  word-break: keep-all;
}

.button-group {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.button-group .el-button {
  margin: 0;
}

.button-group .el-dropdown {
  margin-left: 4px;
}

/* 播放器相关样式 */
.player-container {
  width: 100%;
  height: 60vh;
  background-color: #000;
  border-radius: 6px;
  overflow: hidden;
}

.no-stream-tip {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
}

.dialog-footer {
  text-align: center;
}
</style>