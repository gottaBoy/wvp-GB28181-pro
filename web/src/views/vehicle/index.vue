<template>
  <div id="vehicleList" class="app-container">
    <el-form :inline="true" size="mini">
      <el-form-item label="搜索">
        <el-input
          v-model="searchStr"
          style="margin-right: 1rem; width: auto;"
          placeholder="车辆ID或名称"
          prefix-icon="el-icon-search"
          clearable
          @input="handleSearch"
        />
      </el-form-item>
      <el-form-item label="状态">
        <el-select
          v-model="statusFilter"
          style="width: 8rem; margin-right: 1rem;"
          placeholder="请选择"
          default-first-option
          @change="handleSearch"
        >
          <el-option label="全部" value="" />
          <el-option label="在线" value="online" />
          <el-option label="离线" value="offline" />
        </el-select>
      </el-form-item>
      <el-form-item style="float: right;">
        <el-button 
          type="success"
          icon="el-icon-video-camera"
          @click="goToMonitor">
          车辆监控
        </el-button>
        <el-button
          icon="el-icon-refresh-right"
          circle
          :loading="loading"
          @click="refresh()"
        />
      </el-form-item>
    </el-form>
    <!--车辆列表-->
    <el-table
      size="small"
      :data="filteredVehicleList"
      height="calc(100% - 64px)"
      header-row-class-name="table-header"
      :loading="loading"
    >
      <el-table-column prop="vehicleId" label="车辆ID" min-width="150" />
      <el-table-column prop="ipAddress" label="IP" min-width="140" />
      <el-table-column label="状态" min-width="100">
        <template v-slot:default="scope">
          <el-tag v-if="scope.row.status === 'online'" size="medium" type="success">在线</el-tag>
          <el-tag v-else size="medium" type="info">离线</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="vehicleName" label="名称" min-width="150">
        <template v-slot:default="scope">
          <span>{{ scope.row.vehicleName || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="200">
        <template v-slot:default="scope">
          <span>{{ scope.row.remark || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="280" fixed="right">
        <template v-slot:default="scope">
          <el-button
            type="text"
            size="small"
            icon="el-icon-view"
            @click="showDetail(scope.row)"
          >详情</el-button>
          
          <el-button
            type="text"
            size="small"
            icon="el-icon-connection"
            :loading="connectionChecking[scope.row.vehicleId]"
            @click="checkConnection(scope.row)"
          >连接测试</el-button>
          
          <el-dropdown @command="handleBatchCommand">
            <el-button type="text" size="small">
              批量操作<i class="el-icon-arrow-down el-icon--right"></i>
            </el-button>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item :command="{action: 'batchSubscribe', vehicle: scope.row}">
                全部订阅
              </el-dropdown-item>
              <el-dropdown-item :command="{action: 'batchUnsubscribe', vehicle: scope.row}">
                全部取消订阅
              </el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </template>
      </el-table-column>
    </el-table>
    
    <!-- 车辆详情对话框 -->
    <el-dialog
      title="车辆详情"
      :visible.sync="detailDialogVisible"
      width="90%"
      :before-close="handleCloseDetail"
      :close-on-click-modal="false"
    >
      <vehicle-detail
        v-if="detailDialogVisible"
        :vehicle-id="selectedVehicleId"
        @close="detailDialogVisible = false"
      />
    </el-dialog>
  </div>
</template>

<script>
import { 
  getAllVehicles, 
  checkVehicleConnection,
  subscribeVehicleCameras,
  unsubscribeVehicleCameras,
  getVehicleCameras
} from '@/api/vehicle'
import VehicleDetail from './detail.vue'

export default {
  name: 'VehicleList',
  components: {
    VehicleDetail
  },
  data() {
    return {
      vehicleList: [],
      loading: false,
      searchStr: '',
      statusFilter: '',
      detailDialogVisible: false,
      selectedVehicleId: '',
      connectionChecking: {}
    }
  },
  computed: {
    filteredVehicleList() {
      let filtered = this.vehicleList
      
      if (this.searchStr) {
        const search = this.searchStr.toLowerCase()
        filtered = filtered.filter(item => {
          return (item.vehicleId && item.vehicleId.toLowerCase().includes(search)) ||
                 (item.vehicleName && item.vehicleName.toLowerCase().includes(search))
        })
      }
      
      if (this.statusFilter) {
        filtered = filtered.filter(item => item.status === this.statusFilter)
      }
      
      return filtered
    }
  },
  mounted() {
    this.refresh()
  },
  methods: {
    goToMonitor() {
      this.$router.push('/vehicle/monitor')
    },
    refresh() {
      this.loading = true
      getAllVehicles().then(response => {
        if (response.code === 200) {
          this.vehicleList = response.data || []
        } else {
          this.$message.error('获取车辆列表失败: ' + response.msg)
        }
      }).catch(error => {
        this.$message.error('获取车辆列表失败: ' + (error.message || '未知错误'))
      }).finally(() => {
        this.loading = false
      })
    },
    handleSearch() {
      // 使用computed属性自动过滤，无需手动处理
    },
    getPushingCameraCount(cameras) {
      if (!cameras || cameras.length === 0) return 0
      return cameras.filter(c => c.pushing === true).length
    },
    showDetail(row) {
      this.selectedVehicleId = row.vehicleId
      this.detailDialogVisible = true
    },
    handleCloseDetail() {
      this.detailDialogVisible = false
      this.selectedVehicleId = ''
    },
    
    async checkConnection(vehicle) {
      this.$set(this.connectionChecking, vehicle.vehicleId, true)
      try {
        const response = await checkVehicleConnection(vehicle.vehicleId)
        if (response.code === 200) {
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

    async handleBatchCommand(command) {
      const { action, vehicle } = command
      
      try {
        // 先获取车辆的相机列表
        const cameraResponse = await getVehicleCameras(vehicle.vehicleId)
        if (cameraResponse.code !== 200) {
          this.$message.error('获取相机列表失败: ' + cameraResponse.msg)
          return
        }
        
        const cameras = cameraResponse.data || []
        if (cameras.length === 0) {
          this.$message.info('该车辆没有相机')
          return
        }
        
        const cameraIds = cameras.map(camera => camera.cameraId)
        
        if (action === 'batchSubscribe') {
          const response = await subscribeVehicleCameras(vehicle.vehicleId, cameraIds)
          if (response.code === 200) {
            this.$message.success(`批量订阅成功 (${cameraIds.length} 个相机)`)
            this.refresh() // 刷新列表
          } else {
            this.$message.error('批量订阅失败: ' + response.msg)
          }
        } else if (action === 'batchUnsubscribe') {
          const response = await unsubscribeVehicleCameras(vehicle.vehicleId, cameraIds)
          if (response.code === 200) {
            this.$message.success(`批量取消订阅成功 (${cameraIds.length} 个相机)`)
            this.refresh() // 刷新列表
          } else {
            this.$message.error('批量取消订阅失败: ' + response.msg)
          }
        }
      } catch (error) {
        this.$message.error(`批量操作异常: ${error.message}`)
      }
    }
  }
}
</script>

<style scoped>
#vehicleList {
  height: calc(100vh - 84px);
  padding: 20px;
}

.app-container {
  height: 100%;
}
</style>

