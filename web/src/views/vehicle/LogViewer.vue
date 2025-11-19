<template>
  <div class="log-viewer">
    <el-card class="box-card">
      <div slot="header" class="header">
        <span style="font-weight: bold; font-size: 14px;">日志查看器</span>
        <div class="header-actions">
          <el-button size="mini" icon="el-icon-refresh" @click="refreshLogs">刷新</el-button>
          <el-button size="mini" icon="el-icon-download" @click="exportLogs">导出</el-button>
          <el-button size="mini" icon="el-icon-delete" type="danger" @click="clearLogs">清空</el-button>
        </div>
      </div>

      <!-- 过滤器 -->
      <el-form :inline="true" size="mini" class="filter-form">
        <el-form-item label="日志类型">
          <el-select v-model="filter.type" placeholder="全部" clearable @change="applyFilter">
            <el-option label="全部" value="" />
            <el-option label="车辆心跳" value="VEHICLE_HEARTBEAT" />
            <el-option label="车辆上线" value="VEHICLE_ONLINE" />
            <el-option label="车辆离线" value="VEHICLE_OFFLINE" />
            <el-option label="相机订阅" value="CAMERA_SUBSCRIBE" />
            <el-option label="相机取消订阅" value="CAMERA_UNSUBSCRIBE" />
            <el-option label="推流开始" value="CAMERA_STREAM_START" />
            <el-option label="推流停止" value="CAMERA_STREAM_STOP" />
            <el-option label="推流错误" value="CAMERA_STREAM_ERROR" />
            <el-option label="播放开始" value="PLAY_START" />
            <el-option label="播放停止" value="PLAY_STOP" />
            <el-option label="播放错误" value="PLAY_ERROR" />
            <el-option label="指令发送" value="COMMAND_SEND" />
            <el-option label="指令成功" value="COMMAND_SUCCESS" />
            <el-option label="指令失败" value="COMMAND_FAILED" />
            <el-option label="API请求" value="API_REQUEST" />
            <el-option label="API响应" value="API_RESPONSE" />
            <el-option label="API错误" value="API_ERROR" />
          </el-select>
        </el-form-item>

        <el-form-item label="日志级别">
          <el-select v-model="filter.level" placeholder="全部" clearable @change="applyFilter">
            <el-option label="全部" value="" />
            <el-option label="DEBUG" value="DEBUG" />
            <el-option label="INFO" value="INFO" />
            <el-option label="WARN" value="WARN" />
            <el-option label="ERROR" value="ERROR" />
          </el-select>
        </el-form-item>

        <el-form-item label="车辆ID">
          <el-input
            v-model="filter.vehicleId"
            placeholder="车辆ID"
            clearable
            @change="applyFilter"
            style="width: 150px;"
          />
        </el-form-item>

        <el-form-item label="相机ID">
          <el-input
            v-model="filter.cameraId"
            placeholder="相机ID"
            clearable
            @change="applyFilter"
            style="width: 150px;"
          />
        </el-form-item>

        <el-form-item>
          <el-checkbox v-model="autoRefresh" @change="toggleAutoRefresh">
            自动刷新 ({{ refreshInterval / 1000 }}秒)
          </el-checkbox>
        </el-form-item>
      </el-form>

      <!-- 日志统计 -->
      <div class="log-stats">
        <el-tag size="mini" type="info">总计: {{ filteredLogs.length }}</el-tag>
        <el-tag size="mini" type="success">INFO: {{ getCountByLevel('INFO') }}</el-tag>
        <el-tag size="mini" type="warning">WARN: {{ getCountByLevel('WARN') }}</el-tag>
        <el-tag size="mini" type="danger">ERROR: {{ getCountByLevel('ERROR') }}</el-tag>
        <el-tag size="mini">DEBUG: {{ getCountByLevel('DEBUG') }}</el-tag>
      </div>

      <!-- 日志表格 -->
      <el-table
        :data="paginatedLogs"
        size="mini"
        :height="tableHeight"
        stripe
        :row-class-name="getRowClassName"
        style="width: 100%; margin-top: 10px;">
        
        <el-table-column type="expand">
          <template slot-scope="props">
            <div class="log-detail">
              <el-descriptions :column="2" size="mini" border>
                <el-descriptions-item label="日志ID">{{ props.row.id }}</el-descriptions-item>
                <el-descriptions-item label="请求ID">{{ props.row.requestId || '-' }}</el-descriptions-item>
                <el-descriptions-item label="时间戳">{{ props.row.timestamp }}</el-descriptions-item>
                <el-descriptions-item label="耗时">{{ props.row.duration ? props.row.duration + 'ms' : '-' }}</el-descriptions-item>
                <el-descriptions-item label="车辆ID">{{ props.row.vehicleId || '-' }}</el-descriptions-item>
                <el-descriptions-item label="相机ID">{{ props.row.cameraId || '-' }}</el-descriptions-item>
                <el-descriptions-item label="应用">{{ props.row.app || '-' }}</el-descriptions-item>
                <el-descriptions-item label="流ID">{{ props.row.stream || '-' }}</el-descriptions-item>
                <el-descriptions-item label="消息" :span="2">{{ props.row.message }}</el-descriptions-item>
                <el-descriptions-item label="详细数据" :span="2">
                  <pre class="json-data">{{ formatJson(props.row.data) }}</pre>
                </el-descriptions-item>
                <el-descriptions-item v-if="props.row.error" label="错误信息" :span="2">
                  <pre class="error-data">{{ formatError(props.row.error) }}</pre>
                </el-descriptions-item>
              </el-descriptions>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="时间" width="160">
          <template slot-scope="scope">
            {{ formatTime(scope.row.timestamp) }}
          </template>
        </el-table-column>

        <el-table-column label="级别" width="70">
          <template slot-scope="scope">
            <el-tag :type="getLevelType(scope.row.level)" size="mini">
              {{ scope.row.level }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="类型" width="140">
          <template slot-scope="scope">
            <el-tag size="mini" effect="plain">{{ getTypeLabel(scope.row.type) }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="车辆ID" width="120" show-overflow-tooltip>
          <template slot-scope="scope">
            {{ scope.row.vehicleId || '-' }}
          </template>
        </el-table-column>

        <el-table-column label="相机ID" width="120" show-overflow-tooltip>
          <template slot-scope="scope">
            {{ scope.row.cameraId || '-' }}
          </template>
        </el-table-column>

        <el-table-column label="消息" min-width="200" show-overflow-tooltip>
          <template slot-scope="scope">
            {{ scope.row.message }}
          </template>
        </el-table-column>

        <el-table-column label="耗时" width="80">
          <template slot-scope="scope">
            {{ scope.row.duration ? scope.row.duration + 'ms' : '-' }}
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        style="text-align: right; margin-top: 10px;"
        :current-page="currentPage"
        :page-size="pageSize"
        :page-sizes="[20, 50, 100, 200]"
        layout="total, sizes, prev, pager, next"
        :total="filteredLogs.length"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </el-card>
  </div>
</template>

<script>
import vehicleLogger, { LogType } from '@/utils/vehicleLogger'

export default {
  name: 'LogViewer',
  data() {
    return {
      logs: [],
      filteredLogs: [],
      filter: {
        type: '',
        level: '',
        vehicleId: '',
        cameraId: ''
      },
      currentPage: 1,
      pageSize: 50,
      autoRefresh: false,
      refreshInterval: 2000,
      refreshTimer: null,
      tableHeight: 'calc(100vh - 400px)'
    }
  },
  computed: {
    paginatedLogs() {
      const start = (this.currentPage - 1) * this.pageSize
      const end = start + this.pageSize
      return this.filteredLogs.slice(start, end)
    }
  },
  mounted() {
    this.refreshLogs()
  },
  beforeDestroy() {
    this.stopAutoRefresh()
  },
  methods: {
    refreshLogs() {
      this.logs = vehicleLogger.getLogs()
      this.applyFilter()
    },

    applyFilter() {
      this.filteredLogs = vehicleLogger.getLogs({
        type: this.filter.type || undefined,
        level: this.filter.level || undefined,
        vehicleId: this.filter.vehicleId || undefined,
        cameraId: this.filter.cameraId || undefined
      })
      // 倒序显示，最新的在前面
      this.filteredLogs.reverse()
    },

    toggleAutoRefresh() {
      if (this.autoRefresh) {
        this.startAutoRefresh()
      } else {
        this.stopAutoRefresh()
      }
    },

    startAutoRefresh() {
      this.refreshTimer = setInterval(() => {
        this.refreshLogs()
      }, this.refreshInterval)
    },

    stopAutoRefresh() {
      if (this.refreshTimer) {
        clearInterval(this.refreshTimer)
        this.refreshTimer = null
      }
    },

    clearLogs() {
      this.$confirm('确定清空所有日志吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        vehicleLogger.clear()
        this.refreshLogs()
        this.$message.success('日志已清空')
      }).catch(() => {})
    },

    exportLogs() {
      this.$confirm('选择导出格式', '导出日志', {
        confirmButtonText: 'JSON',
        cancelButtonText: 'CSV',
        distinguishCancelAndClose: true,
        type: 'info'
      }).then(() => {
        vehicleLogger.download('json')
      }).catch(action => {
        if (action === 'cancel') {
          vehicleLogger.download('csv')
        }
      })
    },

    getCountByLevel(level) {
      return this.filteredLogs.filter(log => log.level === level).length
    },

    getLevelType(level) {
      const typeMap = {
        DEBUG: '',
        INFO: 'success',
        WARN: 'warning',
        ERROR: 'danger'
      }
      return typeMap[level] || 'info'
    },

    getTypeLabel(type) {
      const labelMap = {
        VEHICLE_HEARTBEAT: '车辆心跳',
        VEHICLE_ONLINE: '车辆上线',
        VEHICLE_OFFLINE: '车辆离线',
        VEHICLE_REGISTER: '车辆注册',
        VEHICLE_STATUS_CHANGE: '车辆状态变更',
        CAMERA_SUBSCRIBE: '相机订阅',
        CAMERA_UNSUBSCRIBE: '取消订阅',
        CAMERA_STREAM_START: '推流开始',
        CAMERA_STREAM_STOP: '推流停止',
        CAMERA_STREAM_ERROR: '推流错误',
        CAMERA_STATUS_CHANGE: '相机状态变更',
        PLAY_START: '播放开始',
        PLAY_STOP: '播放停止',
        PLAY_ERROR: '播放错误',
        PLAY_URL_GENERATED: '播放链接生成',
        COMMAND_SEND: '指令发送',
        COMMAND_SUCCESS: '指令成功',
        COMMAND_FAILED: '指令失败',
        COMMAND_TIMEOUT: '指令超时',
        API_REQUEST: 'API请求',
        API_RESPONSE: 'API响应',
        API_ERROR: 'API错误'
      }
      return labelMap[type] || type
    },

    getRowClassName({ row }) {
      if (row.level === 'ERROR') {
        return 'error-row'
      } else if (row.level === 'WARN') {
        return 'warning-row'
      }
      return ''
    },

    formatTime(timestamp) {
      return new Date(timestamp).toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: false
      })
    },

    formatJson(data) {
      if (!data || Object.keys(data).length === 0) {
        return '无'
      }
      return JSON.stringify(data, null, 2)
    },

    formatError(error) {
      if (typeof error === 'string') {
        return error
      }
      if (error && error.message) {
        return `${error.message}\n${error.stack || ''}`
      }
      return JSON.stringify(error, null, 2)
    },

    handleSizeChange(val) {
      this.pageSize = val
      this.currentPage = 1
    },

    handleCurrentChange(val) {
      this.currentPage = val
    }
  }
}
</script>

<style scoped>
.log-viewer {
  padding: 12px;
  height: 100vh;
  background-color: #f5f7fa;
}

.box-card {
  height: calc(100vh - 24px);
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.filter-form {
  margin-bottom: 10px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e4e7ed;
}

.log-stats {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}

.log-detail {
  padding: 10px 20px;
  background-color: #f9fafc;
}

.json-data {
  background-color: #f5f7fa;
  padding: 8px;
  border-radius: 4px;
  font-size: 12px;
  max-height: 300px;
  overflow: auto;
  margin: 0;
}

.error-data {
  background-color: #fef0f0;
  color: #f56c6c;
  padding: 8px;
  border-radius: 4px;
  font-size: 12px;
  max-height: 200px;
  overflow: auto;
  margin: 0;
}

.el-table >>> .error-row {
  background-color: #fef0f0;
}

.el-table >>> .warning-row {
  background-color: #fdf6ec;
}
</style>
