<template>
  <div class="vehicle-detail">
    <div v-if="loading" style="text-align: center; padding: 40px;">
      <i class="el-icon-loading" style="font-size: 24px;"></i>
      <p>加载中...</p>
    </div>
    
    <div v-else-if="vehicle">
      <!-- 车辆基本信息 -->
      <el-card class="box-card compact-card" style="margin-bottom: 12px;">
        <div slot="header" class="clearfix">
          <span style="font-weight: bold; font-size: 13px;">车辆信息</span>
        </div>
        <el-descriptions :column="2" border size="mini">
          <el-descriptions-item label="车辆ID">{{ vehicle.vehicleId }}</el-descriptions-item>
          <el-descriptions-item label="IP地址">{{ vehicle.ipAddress || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag v-if="vehicle.status === 'online'" type="success" size="mini">在线</el-tag>
            <el-tag v-else type="info" size="mini">离线</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="名称">{{ vehicle.vehicleName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ vehicle.remark || '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 相机列表 -->
      <el-card class="box-card compact-card">
        <div slot="header" class="clearfix">
          <div class="header-left">
            <span style="font-weight: bold; font-size: 13px;">相机列表 ({{ cameras.length }})</span>
            <el-tag v-if="selectedCameras.length > 0" type="primary" size="mini">
              已选择 {{ selectedCameras.length }} 个
            </el-tag>
          </div>
          <div class="header-right">
            <el-button 
              type="success" 
              size="mini" 
              icon="el-icon-video-play"
              :disabled="selectedCameras.length === 0"
              :loading="batchStartLoading"
              @click="batchStartStream">
              批量启动
            </el-button>
            <el-button 
              type="warning" 
              size="mini" 
              icon="el-icon-video-pause"
              :disabled="selectedCameras.length === 0"
              :loading="batchStopLoading"
              @click="batchStopStream">
              批量停止
            </el-button>
            <el-divider direction="vertical"></el-divider>
            <el-button 
              size="mini" 
              icon="el-icon-refresh" 
              @click="refreshCameras">
              刷新
            </el-button>
          </div>
        </div>
        
        <el-table
          ref="cameraTable"
          :data="cameras"
          size="mini"
          style="width: 100%"
          :loading="camerasLoading"
          @selection-change="handleSelectionChange"
        >
          <el-table-column
            type="selection"
            width="55">
          </el-table-column>
          <el-table-column label="车辆ID" min-width="120">
            <template v-slot:default="scope">
              <span>{{ currentVehicleId }}</span>
            </template>
          </el-table-column>
          <el-table-column label="名称（推流ID）" min-width="150">
            <template v-slot:default="scope">
              <span>{{ scope.row.cameraId }}</span>
            </template>
          </el-table-column>
          <el-table-column label="推流状态" min-width="100">
            <template v-slot:default="scope">
              <el-tag v-if="scope.row.pushing" size="mini" type="success">推流中</el-tag>
              <el-tag v-else size="mini" type="info">已停止</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="topic" label="流Topic" min-width="300">
            <template v-slot:default="scope">
              <span>{{ scope.row.topic || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="120" fixed="right">
            <template v-slot:default="scope">
              <el-button
                v-if="!scope.row.pushing"
                type="text"
                size="mini"
                icon="el-icon-video-play"
                :loading="scope.row.startLoading"
                @click="startStream(scope.row)"
              >启动
              </el-button>
              <el-button
                v-else
                type="text"
                size="mini"
                icon="el-icon-video-pause"
                :loading="scope.row.stopLoading"
                style="color: #f56c6c"
                @click="stopStream(scope.row)"
              >停止
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>
    
    <div v-else style="text-align: center; padding: 40px;">
      <el-empty description="车辆不存在或加载失败" />
    </div>
  </div>
</template>

<script>
import { getVehicle, getVehicleCameras, startCameraStream, stopCameraStream } from '@/api/vehicle'
import vehicleLogger from '@/utils/vehicleLogger'

export default {
  name: 'VehicleDetail',
  props: {
    vehicleId: {
      type: String,
      required: false
    }
  },
  data() {
    return {
      vehicle: null,
      cameras: [],
      selectedCameras: [],
      loading: false,
      camerasLoading: false,
      batchStartLoading: false,
      batchStopLoading: false,
      currentVehicleId: ''
    }
  },
  watch: {
    vehicleId: {
      immediate: true,
      handler(newVal) {
        if (newVal) {
          this.currentVehicleId = newVal
          this.loadVehicle()
          this.loadCameras()
        }
      }
    }
  },
  mounted() {
    // 优先从路由参数获取 vehicleId
    const vehicleIdFromRoute = this.$route.query.vehicleId
    if (vehicleIdFromRoute) {
      this.currentVehicleId = vehicleIdFromRoute
    } else if (this.vehicleId) {
      this.currentVehicleId = this.vehicleId
    }
    
    if (this.currentVehicleId) {
      this.loadVehicle()
      this.loadCameras()
    }
  },
  methods: {
    handleSelectionChange(selection) {
      this.selectedCameras = selection
    },
    loadVehicle() {
      this.loading = true
      const startTime = Date.now()
      
      getVehicle(this.currentVehicleId).then(response => {
        const duration = Date.now() - startTime
        
        if (response.code === 200) {
          this.vehicle = response.data
          vehicleLogger.logApiResponse('GET', `/vehicle/${this.currentVehicleId}`, response, duration)
        } else {
          vehicleLogger.logApiError('GET', `/vehicle/${this.currentVehicleId}`, new Error(response.msg), duration)
          this.$message.error('获取车辆信息失败: ' + response.msg)
        }
      }).catch(error => {
        const duration = Date.now() - startTime
        vehicleLogger.logApiError('GET', `/vehicle/${this.currentVehicleId}`, error, duration)
        this.$message.error('获取车辆信息失败: ' + (error.message || '未知错误'))
      }).finally(() => {
        this.loading = false
      })
    },
    loadCameras() {
      this.camerasLoading = true
      getVehicleCameras(this.currentVehicleId).then(response => {
        if (response.code === 200) {
          this.cameras = (response.data || []).map(camera => ({
            ...camera,
            startLoading: false,
            stopLoading: false
          }))
        } else {
          this.$message.error('获取相机列表失败: ' + response.msg)
        }
      }).catch(error => {
        this.$message.error('获取相机列表失败: ' + (error.message || '未知错误'))
      }).finally(() => {
        this.camerasLoading = false
      })
    },
    refreshCameras() {
      this.loadCameras()
    },
    batchStartStream() {
      if (this.selectedCameras.length === 0) {
        this.$message.warning('请先选择要启动推流的相机')
        return
      }

      this.$confirm(`确定要启动 ${this.selectedCameras.length} 个相机的推流吗？`, '批量启动推流', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.batchStartLoading = true
        const promises = this.selectedCameras.map(camera => 
          startCameraStream(this.currentVehicleId, camera.cameraId)
        )

        Promise.allSettled(promises).then(results => {
          const successCount = results.filter(r => r.status === 'fulfilled' && r.value.code === 200).length
          const failCount = this.selectedCameras.length - successCount

          if (successCount > 0) {
            this.$message.success(`成功启动 ${successCount} 个相机推流${failCount > 0 ? `，失败 ${failCount} 个` : ''}`)
          } else {
            this.$message.error('全部启动失败')
          }

          // 刷新列表
          setTimeout(() => {
            this.loadCameras()
            this.$refs.cameraTable.clearSelection()
          }, 1000)
        }).finally(() => {
          this.batchStartLoading = false
        })
      }).catch(() => {
        // 用户取消
      })
    },
    batchStopStream() {
      if (this.selectedCameras.length === 0) {
        this.$message.warning('请先选择要停止推流的相机')
        return
      }

      this.$confirm(`确定要停止 ${this.selectedCameras.length} 个相机的推流吗？`, '批量停止推流', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.batchStopLoading = true
        const promises = this.selectedCameras.map(camera => 
          stopCameraStream(this.currentVehicleId, camera.cameraId)
        )

        Promise.allSettled(promises).then(results => {
          const successCount = results.filter(r => r.status === 'fulfilled' && r.value.code === 200).length
          const failCount = this.selectedCameras.length - successCount

          if (successCount > 0) {
            this.$message.success(`成功停止 ${successCount} 个相机推流${failCount > 0 ? `，失败 ${failCount} 个` : ''}`)
          } else {
            this.$message.error('全部停止失败')
          }

          // 刷新列表
          setTimeout(() => {
            this.loadCameras()
            this.$refs.cameraTable.clearSelection()
          }, 1000)
        }).finally(() => {
          this.batchStopLoading = false
        })
      }).catch(() => {
        // 用户取消
      })
    },
    startStream(camera) {
      this.$set(camera, 'startLoading', true)
      const startTime = Date.now()
      
      vehicleLogger.logCommandSend(this.currentVehicleId, 'START_STREAM', {
        cameraId: camera.cameraId
      })
      
      startCameraStream(this.currentVehicleId, camera.cameraId).then(response => {
        const duration = Date.now() - startTime
        
        if (response.code === 200) {
          vehicleLogger.logCameraStreamStart(this.currentVehicleId, camera.cameraId, {
            response: response.data,
            duration
          })
          vehicleLogger.logCommandSuccess(this.currentVehicleId, 'START_STREAM', duration, {
            cameraId: camera.cameraId
          })
          this.$message.success('启动推流成功')
          // 延迟刷新，等待状态同步
          setTimeout(() => {
            this.loadCameras()
          }, 1000)
        } else {
          vehicleLogger.logCameraStreamError(this.currentVehicleId, camera.cameraId, new Error(response.msg), {
            duration
          })
          vehicleLogger.logCommandFailed(this.currentVehicleId, 'START_STREAM', new Error(response.msg), {
            cameraId: camera.cameraId
          })
          this.$message.error('启动推流失败: ' + response.msg)
        }
      }).catch(error => {
        const duration = Date.now() - startTime
        vehicleLogger.logCameraStreamError(this.currentVehicleId, camera.cameraId, error, { duration })
        vehicleLogger.logCommandFailed(this.currentVehicleId, 'START_STREAM', error, {
          cameraId: camera.cameraId
        })
        this.$message.error('启动推流失败: ' + (error.message || '未知错误'))
      }).finally(() => {
        this.$set(camera, 'startLoading', false)
      })
    },
    stopStream(camera) {
      this.$set(camera, 'stopLoading', true)
      const startTime = Date.now()
      
      vehicleLogger.logCommandSend(this.currentVehicleId, 'STOP_STREAM', {
        cameraId: camera.cameraId
      })
      
      stopCameraStream(this.currentVehicleId, camera.cameraId).then(response => {
        const duration = Date.now() - startTime
        
        if (response.code === 200) {
          vehicleLogger.logCameraStreamStop(this.currentVehicleId, camera.cameraId, {
            response: response.data,
            duration
          })
          vehicleLogger.logCommandSuccess(this.currentVehicleId, 'STOP_STREAM', duration, {
            cameraId: camera.cameraId
          })
          this.$message.success('停止推流成功')
          // 延迟刷新，等待状态同步
          setTimeout(() => {
            this.loadCameras()
          }, 1000)
        } else {
          vehicleLogger.logCameraStreamError(this.currentVehicleId, camera.cameraId, new Error(response.msg), {
            duration
          })
          vehicleLogger.logCommandFailed(this.currentVehicleId, 'STOP_STREAM', new Error(response.msg), {
            cameraId: camera.cameraId
          })
          this.$message.error('停止推流失败: ' + response.msg)
        }
      }).catch(error => {
        const duration = Date.now() - startTime
        vehicleLogger.logCameraStreamError(this.currentVehicleId, camera.cameraId, error, { duration })
        vehicleLogger.logCommandFailed(this.currentVehicleId, 'STOP_STREAM', error, {
          cameraId: camera.cameraId
        })
        this.$message.error('停止推流失败: ' + (error.message || '未知错误'))
      }).finally(() => {
        this.$set(camera, 'stopLoading', false)
      })
    }
  }
}
</script>

<style scoped>
.vehicle-detail {
  padding: 12px;
  min-height: calc(100vh - 100px);
  background-color: #f5f7fa;
}

.box-card {
  margin-bottom: 12px;
}

.compact-card >>> .el-card__header {
  padding: 10px 14px;
}

.compact-card >>> .el-card__body {
  padding: 10px 14px;
}

.compact-card >>> .el-descriptions-item__label {
  width: 75px;
  font-size: 12px;
  padding: 6px 10px;
}

.compact-card >>> .el-descriptions-item__content {
  font-size: 12px;
  padding: 6px 10px;
}

.clearfix {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 28px;
  flex-wrap: wrap;
  gap: 6px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.header-right {
  display: flex;
  gap: 6px;
  align-items: center;
  flex-shrink: 0;
  margin-left: auto; /* 确保靠右对齐 */
}

.header-right .el-button {
  margin-left: 0;
  white-space: nowrap; /* 防止按钮文字换行 */
}

.header-right .el-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.header-right .el-divider--vertical {
  margin: 0 4px;
}

/* 批量操作按钮样式优化 */
.header-right .el-button--success {
  background-color: #67c23a;
  border-color: #67c23a;
  color: white;
}

.header-right .el-button--success:hover:not(:disabled) {
  background-color: #85ce61;
  border-color: #85ce61;
}

.header-right .el-button--warning {
  background-color: #e6a23c;
  border-color: #e6a23c;
  color: white;
}

.header-right .el-button--warning:hover:not(:disabled) {
  background-color: #ebb563;
  border-color: #ebb563;
}

/* 选中标签动画 */
.el-tag {
  animation: fadeIn 0.3s ease-in;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-5px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 响应式设计 - 小屏幕优化 */
@media (max-width: 1200px) {
  .header-right .el-button span {
    display: inline; /* 保持文字显示 */
  }
}

@media (max-width: 768px) {
  .clearfix {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .header-left {
    width: 100%;
    justify-content: space-between;
  }
  
  .header-right {
    width: 100%;
    margin-left: 0;
    justify-content: flex-end;
    flex-wrap: wrap;
  }
  
  .header-right .el-button {
    flex: 0 0 auto;
  }
}
</style>

