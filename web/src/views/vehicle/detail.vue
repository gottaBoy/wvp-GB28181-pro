<template>
  <div class="vehicle-detail">
    <div v-if="loading" style="text-align: center; padding: 40px;">
      <i class="el-icon-loading" style="font-size: 24px;"></i>
      <p>加载中...</p>
    </div>
    
    <div v-else-if="vehicle">
      <!-- 车辆基本信息 -->
      <el-card class="box-card" style="margin-bottom: 20px;">
        <div slot="header" class="clearfix">
          <span style="font-weight: bold;">车辆信息</span>
        </div>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="编码">{{ vehicle.vehicleId }}</el-descriptions-item>
          <el-descriptions-item label="IP地址">{{ vehicle.ipAddress || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag v-if="vehicle.status === 'online'" type="success">在线</el-tag>
            <el-tag v-else type="info">离线</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="名称">{{ vehicle.vehicleName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ vehicle.remark || '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 相机列表 -->
      <el-card class="box-card">
        <div slot="header" class="clearfix">
          <span style="font-weight: bold;">相机列表 ({{ cameras.length }})</span>
          <el-button style="float: right; padding: 3px 0" type="text" icon="el-icon-refresh" @click="refreshCameras">刷新</el-button>
        </div>
        
        <el-table
          :data="cameras"
          size="small"
          style="width: 100%"
          :loading="camerasLoading"
        >
          <el-table-column label="应用" min-width="120">
            <template v-slot:default="scope">
              <span>{{ vehicleId }}</span>
            </template>
          </el-table-column>
          <el-table-column label="名称（推流ID）" min-width="150">
            <template v-slot:default="scope">
              <span>{{ scope.row.cameraId }}</span>
            </template>
          </el-table-column>
          <el-table-column label="推流状态" min-width="120">
            <template v-slot:default="scope">
              <el-tag v-if="scope.row.pushing" size="small" type="success">推流中</el-tag>
              <el-tag v-else size="small" type="info">已停止</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="topic" label="原始流" min-width="300">
            <template v-slot:default="scope">
              <span>{{ scope.row.topic || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="150" fixed="right">
            <template v-slot:default="scope">
              <el-button
                v-if="!scope.row.pushing"
                type="text"
                size="small"
                icon="el-icon-video-play"
                :loading="scope.row.startLoading"
                @click="startStream(scope.row)"
              >启动推流
              </el-button>
              <el-button
                v-else
                type="text"
                size="small"
                icon="el-icon-video-pause"
                :loading="scope.row.stopLoading"
                style="color: #f56c6c"
                @click="stopStream(scope.row)"
              >停止推流
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

export default {
  name: 'VehicleDetail',
  props: {
    vehicleId: {
      type: String,
      required: true
    }
  },
  data() {
    return {
      vehicle: null,
      cameras: [],
      loading: false,
      camerasLoading: false
    }
  },
  watch: {
    vehicleId: {
      immediate: true,
      handler(newVal) {
        if (newVal) {
          this.loadVehicle()
          this.loadCameras()
        }
      }
    }
  },
  mounted() {
    if (this.vehicleId) {
      this.loadVehicle()
      this.loadCameras()
    }
  },
  methods: {
    loadVehicle() {
      this.loading = true
      getVehicle(this.vehicleId).then(response => {
        if (response.code === 0) {
          this.vehicle = response.data
        } else {
          this.$message.error('获取车辆信息失败: ' + response.msg)
        }
      }).catch(error => {
        this.$message.error('获取车辆信息失败: ' + (error.message || '未知错误'))
      }).finally(() => {
        this.loading = false
      })
    },
    loadCameras() {
      this.camerasLoading = true
      getVehicleCameras(this.vehicleId).then(response => {
        if (response.code === 0) {
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
    startStream(camera) {
      this.$set(camera, 'startLoading', true)
      startCameraStream(this.vehicleId, camera.cameraId).then(response => {
        if (response.code === 0) {
          this.$message.success('启动推流成功')
          // 延迟刷新，等待状态同步
          setTimeout(() => {
            this.loadCameras()
          }, 1000)
        } else {
          this.$message.error('启动推流失败: ' + response.msg)
        }
      }).catch(error => {
        this.$message.error('启动推流失败: ' + (error.message || '未知错误'))
      }).finally(() => {
        this.$set(camera, 'startLoading', false)
      })
    },
    stopStream(camera) {
      this.$set(camera, 'stopLoading', true)
      stopCameraStream(this.vehicleId, camera.cameraId).then(response => {
        if (response.code === 0) {
          this.$message.success('停止推流成功')
          // 延迟刷新，等待状态同步
          setTimeout(() => {
            this.loadCameras()
          }, 1000)
        } else {
          this.$message.error('停止推流失败: ' + response.msg)
        }
      }).catch(error => {
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
  padding: 20px;
}

.box-card {
  margin-bottom: 20px;
}
</style>

