<template>
  <div id="factoryMonitor" class="app-container">
    <el-container style="height: calc(100vh - 124px);">
      <!-- 左侧厂区列表 - 20% 宽度 -->
      <el-aside width="20%" style="border-right: 1px solid #dcdfe6; padding: 10px; overflow: hidden;">
        <div class="aside-header">
          <h3 style="margin: 0 0 10px 0;">厂区列表</h3>
          <el-input
            v-model="searchFactory"
            placeholder="搜索厂区"
            prefix-icon="el-icon-search"
            clearable
            size="small"
            style="margin-bottom: 10px;"
          />
        </div>
        
        <el-scrollbar style="height: calc(100% - 80px);">
          <div
            v-for="factory in filteredFactories"
            :key="factory.app"
            :class="['factory-item', { 'active': selectedFactory === factory.app }]"
            @click="selectFactory(factory)"
          >
            <div class="factory-info">
              <i class="el-icon-office-building" style="margin-right: 8px;" />
              <span class="factory-name">{{ factory.name || factory.app }}</span>
              <el-tag size="mini" style="margin-left: auto;">{{ factory.count }}</el-tag>
            </div>
          </div>
          
          <el-empty v-if="filteredFactories.length === 0" description="暂无厂区" />
        </el-scrollbar>
      </el-aside>

      <!-- 右侧摄像头列表和播放器 - 80% 宽度，带滚动条 -->
      <el-main style="padding: 0; display: flex; flex-direction: column; height: 100%; overflow-y: auto;">
        <div v-if="selectedFactory" style="flex: 1; display: flex; flex-direction: column; min-height: min-content;">
          <!-- 工具栏 -->
          <div class="toolbar" style="flex-shrink: 0;">
            <div class="toolbar-left">
              <h3 style="margin: 0;">{{ selectedFactoryName }}</h3>
              <el-tag type="info" size="small" style="margin-left: 10px;">
                共 {{ cameras.length }} 个摄像头
              </el-tag>
            </div>
            <div class="toolbar-right">
              <el-checkbox
                v-model="selectAll"
                :indeterminate="isIndeterminate"
                @change="handleSelectAll"
              >
                全选
              </el-checkbox>
              <el-button
                type="primary"
                size="small"
                icon="el-icon-video-play"
                :disabled="selectedCameras.length === 0"
                @click="batchPlay"
              >
                批量播放 ({{ selectedCameras.length }})
              </el-button>
              <el-button
                type="danger"
                size="small"
                icon="el-icon-switch-button"
                :disabled="playingCameras.length === 0"
                @click="stopAll"
              >
                全部停止
              </el-button>
              <el-divider direction="vertical" />
              <el-button-group>
                <el-button
                  size="small"
                  :type="gridLayout === 1 ? 'primary' : ''"
                  @click="changeGridLayout(1)"
                >
                  1画
                </el-button>
                <el-button
                  size="small"
                  :type="gridLayout === 4 ? 'primary' : ''"
                  @click="changeGridLayout(4)"
                >
                  4画
                </el-button>
                <el-button
                  size="small"
                  :type="gridLayout === 9 ? 'primary' : ''"
                  @click="changeGridLayout(9)"
                >
                  9画
                </el-button>
                <el-button
                  size="small"
                  :type="gridLayout === 16 ? 'primary' : ''"
                  @click="changeGridLayout(16)"
                >
                  16画
                </el-button>
                <el-button
                  size="small"
                  :type="gridLayout === 0 ? 'primary' : ''"
                  @click="changeGridLayout(0)"
                >
                  全部
                </el-button>
              </el-button-group>
              <el-button
                size="small"
                :icon="isFullscreen ? 'el-icon-copy-document' : 'el-icon-full-screen'"
                @click="toggleFullscreen"
              >
                {{ isFullscreen ? '退出全屏' : '全屏' }}
              </el-button>
              <el-button
                size="small"
                icon="el-icon-refresh"
                @click="refreshCameras"
              >
                刷新
              </el-button>
            </div>
          </div>

          <!-- 摄像头列表 -->
          <div class="camera-list">
            <!-- 状态统计提示 -->
            <el-alert
              v-if="cameras.length > 0"
              :title="getCameraStatusSummary()"
              type="info"
              :closable="false"
              style="margin-bottom: 10px;"
            />
            
            <el-checkbox-group v-model="selectedCameras">
              <div
                v-for="camera in cameras"
                :key="camera.id"
                :class="['camera-item', { 'playing': playingCameras.includes(camera.id) }]"
              >
                <el-checkbox :label="camera.id" class="camera-checkbox">
                  <div class="camera-info">
                    <i class="el-icon-video-camera" />
                    <span class="camera-name">{{ camera.name }}</span>
                    <el-tag size="mini" type="info">通道 {{ camera.stream }}</el-tag>
                    <el-tag
                      v-if="camera.pulling"
                      size="mini"
                      type="success"
                      style="margin-left: 5px;"
                    >
                      在线
                    </el-tag>
                    <el-tag
                      v-if="playingCameras.includes(camera.id)"
                      size="mini"
                      type="warning"
                      style="margin-left: 5px;"
                    >
                      播放中
                    </el-tag>
                  </div>
                </el-checkbox>
                <div class="camera-actions">
                  <el-button
                    v-if="!playingCameras.includes(camera.id)"
                    type="text"
                    size="small"
                    icon="el-icon-video-play"
                    @click.stop="playSingleCamera(camera)"
                  >
                    播放
                  </el-button>
                  <el-button
                    v-else
                    type="text"
                    size="small"
                    icon="el-icon-video-pause"
                    style="color: #f56c6c;"
                    @click.stop="stopSingleCamera(camera)"
                  >
                    停止
                  </el-button>
                </div>
              </div>
            </el-checkbox-group>

            <el-empty v-if="cameras.length === 0" description="该厂区暂无摄像头" />
          </div>

          <!-- 视频播放区域 -->
          <div v-if="Object.keys(playingVideos).length > 0" class="video-area" :class="{ 'fullscreen': isFullscreen }">
            <div class="video-area-header">
              <h4 style="margin: 0;">正在播放 ({{ Object.keys(playingVideos).length }})</h4>
              <div class="video-area-actions">
                <el-tag size="small" type="info">{{ gridLayoutText }}</el-tag>
                <el-button
                  v-if="isFullscreen"
                  type="text"
                  size="small"
                  icon="el-icon-close"
                  @click="toggleFullscreen"
                >
                  退出全屏
                </el-button>
              </div>
            </div>
            <div :class="['video-grid', `grid-${gridLayout}`]">
              <div
                v-for="(videoInfo, cameraId) in playingVideos"
                :key="cameraId"
                class="video-item"
              >
                <div class="video-item-header">
                  <span class="video-title">{{ videoInfo.name }}</span>
                  <div class="video-item-actions">
                    <el-tag v-if="videoInfo.retrying" size="mini" type="warning">重试中...</el-tag>
                    <el-button
                      type="text"
                      size="mini"
                      icon="el-icon-refresh"
                      title="刷新"
                      @click="reloadSingleCamera(cameraId)"
                    />
                    <el-button
                      type="text"
                      size="mini"
                      icon="el-icon-close"
                      title="关闭"
                      @click="closeSingleCamera(cameraId)"
                    />
                  </div>
                </div>
                <div class="video-container">
                  <VehicleRtcPlayer
                    v-if="videoInfo.url"
                    :ref="`rtcPlayer_${cameraId}`"
                    :player-id="`player_${cameraId}`"
                    :video-url="videoInfo.url"
                    :has-audio="true"
                    @error="handlePlayerError(cameraId, $event)"
                  />
                  <div v-else-if="videoInfo.loading" class="video-loading">
                    <i class="el-icon-loading" />
                    <p>加载中...</p>
                  </div>
                  <div v-else-if="videoInfo.error" class="video-error">
                    <i class="el-icon-warning-outline" />
                    <p class="error-message">{{ videoInfo.errorMsg || '加载失败' }}</p>
                    <div class="error-actions">
                      <el-button
                        v-if="!videoInfo.retrying && !videoInfo.suggestRemove"
                        size="mini"
                        type="primary"
                        @click="reloadSingleCamera(cameraId)"
                      >
                        重试
                      </el-button>
                      <el-button
                        v-if="videoInfo.suggestRemove"
                        size="mini"
                        type="warning"
                        icon="el-icon-warning"
                        disabled
                      >
                        代理已失效
                      </el-button>
                      <el-button
                        size="mini"
                        icon="el-icon-close"
                        @click="closeSingleCamera(cameraId)"
                      >
                        关闭
                      </el-button>
                    </div>
                  </div>
                  <div v-else class="video-error">
                    <i class="el-icon-warning-outline" />
                    <p>未知错误</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div v-else style="flex: 1; display: flex; align-items: center; justify-content: center;">
          <el-empty description="请选择一个厂区" icon="el-icon-office-building" />
        </div>
      </el-main>
    </el-container>
  </div>
</template>

<script>
import { getFactoryListSummary, getFactoryCameras, getCameraPlayUrl, stopCameraPlay } from '@/api/factory'
import VehicleRtcPlayer from '../vehicle/VehicleRtcPlayer.vue'

export default {
  name: 'FactoryMonitor',
  components: {
    VehicleRtcPlayer
  },
  data() {
    return {
      searchFactory: '',
      factories: [],
      selectedFactory: null,
      selectedFactoryName: '',
      cameras: [],
      selectedCameras: [],
      selectAll: false,
      isIndeterminate: false,
      playingCameras: [],
      playingVideos: {},
      updateTimer: null,
      gridLayout: 0, // 画面布局：0=全部, 1/4/9/16（默认全部）
      isFullscreen: false,
      retryCount: {}, // 重试次数记录
      maxRetry: 3, // 最大重试次数
      apiCallLock: {} // API调用锁，防止重复调用
    }
  },
  computed: {
    filteredFactories() {
      if (!this.searchFactory) {
        return this.factories
      }
      const search = this.searchFactory.toLowerCase()
      return this.factories.filter(f =>
        (f.name || f.app).toLowerCase().includes(search)
      )
    },
    gridLayoutText() {
      return `${this.gridLayout}画`
    },
    // 在线摄像头数量
    onlineCamerasCount() {
      return this.cameras.filter(c => c.pulling).length
    }
  },
  watch: {
    selectedCameras(val) {
      const checkedCount = val.length
      this.selectAll = checkedCount === this.cameras.length
      this.isIndeterminate = checkedCount > 0 && checkedCount < this.cameras.length
    }
  },
  mounted() {
    this.loadFactories()
    this.startAutoUpdate()
  },
  beforeDestroy() {
    this.stopAutoUpdate()
    this.stopAllPlaying()
  },
  methods: {
    // 加载厂区列表
    async loadFactories() {
      try {
        console.log('[厂区监控] 开始加载厂区列表...')
        const response = await getFactoryListSummary()
        console.log('[厂区监控] API原始响应:', response)
        console.log('[厂区监控] 响应状态码:', response.status)
        console.log('[厂区监控] 响应数据:', response.data)
        
        if (!response || !response.data) {
          throw new Error('API响应数据为空')
        }
        
        this.factories = response.data || []
        console.log('[厂区监控] 厂区列表:', this.factories)
        
        if (this.factories.length > 0) {
          this.$message.success(`加载成功，共 ${this.factories.length} 个厂区`)
        } else {
          this.$message.warning('暂无厂区数据')
        }
      } catch (error) {
        console.error('[厂区监控] 加载厂区列表失败 - 错误对象:', error)
        console.error('[厂区监控] 错误消息:', error.message)
        console.error('[厂区监控] 错误堆栈:', error.stack)
        console.error('[厂区监控] 错误响应:', error.response)
        
        const errorMsg = error.response?.data?.msg || error.message || '未知错误'
        this.$message.error('加载厂区列表失败: ' + errorMsg)
      }
    },

    // 获取厂区名称
    getFactoryName(app) {
      const nameMap = {
        'guangqing': '广青厂区',
        'rtp': 'RTP厂区',
        'proxy': '代理厂区'
      }
      return nameMap[app] || app
    },

    // 获取摄像头状态摘要
    getCameraStatusSummary() {
      const total = this.cameras.length
      const online = this.onlineCamerasCount
      const offline = total - online
      
      if (offline === 0) {
        return `全部摄像头在线 (${total}/${total})`
      } else if (online === 0) {
        return `⚠️ 全部摄像头离线 (0/${total})，请检查ZLMediaKit服务和网络连接`
      } else {
        return `⚠️ ${online}/${total} 个摄像头在线，${offline} 个离线`
      }
    },

    // 选择厂区
    async selectFactory(factory) {
      this.selectedFactory = factory.app
      this.selectedFactoryName = factory.name
      this.selectedCameras = []
      this.cameras = []
      await this.loadCameras()
    },

    // 加载摄像头列表
    async loadCameras(silent = false) {
      // 如果没有选中厂区，直接返回
      if (!this.selectedFactory) {
        console.warn('[厂区监控] 未选中厂区，跳过加载摄像头')
        return
      }
      
      try {
        console.log('[厂区监控] 开始加载摄像头，厂区:', this.selectedFactory, '静默模式:', silent)
        const response = await getFactoryCameras(this.selectedFactory)
        console.log('[厂区监控] API原始响应:', response)
        console.log('[厂区监控] 响应状态码:', response.status)
        console.log('[厂区监控] 响应数据:', response.data)
        
        if (!response || !response.data) {
          throw new Error('API响应数据为空')
        }
        
        this.cameras = response.data || []
        console.log('[厂区监控] 摄像头列表:', this.cameras)
        console.log(`[厂区监控] 厂区 ${this.selectedFactory} 摄像头加载完成，共 ${this.cameras.length} 个`)
        
        // 非静默模式才显示提示
        if (!silent && this.cameras.length === 0) {
          this.$message.warning('该厂区暂无摄像头')
        }
      } catch (error) {
        console.error('[厂区监控] 加载摄像头列表失败 - 错误对象:', error)
        console.error('[厂区监控] 错误消息:', error.message)
        console.error('[厂区监控] 错误堆栈:', error.stack)
        console.error('[厂区监控] 错误响应:', error.response)
        
        // 非静默模式才显示错误提示
        if (!silent) {
          const errorMsg = error.response?.data?.msg || error.message || '未知错误'
          this.$message.error('加载摄像头列表失败: ' + errorMsg)
        }
      }
    },

    // 刷新摄像头列表
    refreshCameras() {
      this.loadCameras()
    },

    // 全选/取消全选
    handleSelectAll(val) {
      this.selectedCameras = val ? this.cameras.map(c => c.id) : []
    },

    // 批量播放
    async batchPlay() {
      if (this.selectedCameras.length === 0) {
        this.$message.warning('请先选择要播放的摄像头')
        return
      }

      console.log('[厂区监控] 开始批量播放，共', this.selectedCameras.length, '个摄像头')
      
      // 🔑 优化: 限制批量播放数量（0表示全部，不限制）
      const maxBatch = this.gridLayout === 0 ? 999 : this.gridLayout
      if (this.selectedCameras.length > maxBatch) {
        const layoutText = this.gridLayout === 0 ? '全部' : `${this.gridLayout}画`
        this.$message.warning(`当前${layoutText}布局，最多同时播放${maxBatch}个摄像头`)
        return
      }
      
      let successCount = 0
      let failCount = 0
      const loading = this.$loading({
        lock: true,
        text: `正在启动播放 (0/${this.selectedCameras.length})`,
        spinner: 'el-icon-loading',
        background: 'rgba(0, 0, 0, 0.7)'
      })

      try {
        for (let i = 0; i < this.selectedCameras.length; i++) {
          const cameraId = this.selectedCameras[i]
          const camera = this.cameras.find(c => c.id === cameraId)
          
          if (camera && !this.playingCameras.includes(cameraId)) {
            // 区分直接播放和启动拉流
            const action = (camera.pulling && camera.playUrls) ? '播放' : '启动拉流'
            loading.text = `正在${action} (${i + 1}/${this.selectedCameras.length}): ${camera.name || camera.stream}`
            
            const success = await this.playSingleCamera(camera, true)
            if (success) {
              successCount++
            } else {
              failCount++
            }
            
            // 每个请求之间延迟500ms，避免并发过多
            if (i < this.selectedCameras.length - 1) {
              await new Promise(resolve => setTimeout(resolve, 500))
            }
          }
        }

        loading.close()
        
        if (successCount > 0) {
          this.$message.success(`成功启动 ${successCount} 个摄像头${failCount > 0 ? `，失败 ${failCount} 个` : ''}`)
        } else {
          this.$message.error(`全部播放失败，请检查摄像头状态和网络连接`)
        }
      } catch (error) {
        loading.close()
        console.error('[厂区监控] 批量播放异常:', error)
        this.$message.error('批量播放出现异常: ' + error.message)
      }
    },

    // 播放单个摄像头
    async playSingleCamera(camera, silent = false) {
      try {
        // 防止重复调用
        if (this.apiCallLock[camera.id]) {
          console.warn('[厂区监控] 摄像头正在处理中，跳过重复调用:', camera.id)
          return false
        }
        
        if (this.playingCameras.includes(camera.id)) {
          if (!silent) this.$message.warning('该摄像头正在播放中')
          return false
        }

        // 设置API调用锁
        this.$set(this.apiCallLock, camera.id, true)

        // 重置重试计数
        this.retryCount[camera.id] = 0

        // 🔑 关键修复: 两阶段数据设置
        // 第一阶段: 添加到播放列表，显示加载状态，URL为null（不触发组件创建）
        this.$set(this.playingVideos, camera.id, {
          id: camera.id,
          name: camera.name || camera.gbName,
          stream: camera.stream,
          loading: true,
          url: null,  // ← URL为null，v-if="videoInfo.url"为false，组件不创建
          error: false,
          errorMsg: null,
          retrying: false
        })
        this.playingCameras.push(camera.id)

        // 判断摄像头是否已经在拉流
        console.log('[厂区监控] 摄像头状态:', {
          id: camera.id,
          name: camera.name,
          pulling: camera.pulling,
          hasPlayUrls: !!camera.playUrls
        })
        
        // 情况1: 摄像头已在拉流且有播放地址 - 直接播放
        if (camera.pulling && camera.playUrls) {
          console.log('[厂区监控] 摄像头已在拉流，直接使用播放地址')
          
          let videoUrl = null
          let playType = null
          
          // 优先使用WebRTC，其次FLV
          if (camera.playUrls.rtc) {
            videoUrl = camera.playUrls.rtc
            playType = 'WebRTC'
          } else if (camera.playUrls.flv) {
            videoUrl = camera.playUrls.flv
            playType = 'FLV'
          } else if (camera.playUrls.ws_flv) {
            videoUrl = camera.playUrls.ws_flv
            playType = 'WS-FLV'
          } else if (camera.playUrls.hls) {
            videoUrl = camera.playUrls.hls
            playType = 'HLS'
          }
          
          if (videoUrl) {
            console.log(`[厂区监控] 直接播放(${playType}):`, videoUrl)
            
            // 🔑 第二阶段: 设置URL，触发v-if和watch
            this.$set(this.playingVideos[camera.id], 'url', videoUrl)
            this.$set(this.playingVideos[camera.id], 'playType', playType)
            this.$set(this.playingVideos[camera.id], 'loading', false)
            
            if (!silent) {
              this.$message.success(`${camera.name || camera.stream} 开始播放(${playType})`)
            }
            
            // 释放API调用锁
            this.$set(this.apiCallLock, camera.id, false)
            return true
          }
        }
        
        // 情况2: 摄像头未拉流或无播放地址 - 调用/api/proxy/start启动拉流
        console.log('[厂区监控] 摄像头未拉流，调用/api/proxy/start启动拉流')
        if (!silent) {
          this.$message.info(`正在启动拉流: ${camera.name || camera.stream}`)
        }
        
        let response
        try {
          response = await getCameraPlayUrl(camera.id)
        } catch (apiError) {
          console.error('[厂区监控] 启动拉流失败:', apiError)
          throw new Error(apiError.message || '启动拉流失败')
        }
        
        console.log('[厂区监控] /api/proxy/start 响应:', response)
        
        // 验证响应数据
        if (!response) {
          throw new Error('API响应为空，可能是网络问题或服务器未响应')
        }
        
        if (!response.data) {
          console.error('[厂区监控] 响应缺少data字段，完整响应:', response)
          throw new Error('API响应格式错误(缺少data字段)，请检查拉流代理状态和ZLMediaKit服务')
        }
        
        const streamInfo = response.data
        console.log('[厂区监控] 流信息:', streamInfo)
        
        // 检查streamInfo是否为对象
        if (!streamInfo || typeof streamInfo !== 'object') {
          console.error('[厂区监控] 流信息格式错误:', streamInfo)
          throw new Error('流信息格式错误，请检查ZLMediaKit服务状态')
        }
        
        console.log('[厂区监控] 可用协议:', {
          hasRtc: !!streamInfo.rtc,
          hasFlv: !!streamInfo.flv,
          hasHls: !!streamInfo.hls,
          hasRtmp: !!streamInfo.rtmp,
          hasRtsp: !!streamInfo.rtsp
        })

        // 优先使用WebRTC，其次FLV
        let videoUrl = null
        let playType = null
        
        // 后端返回的是字符串URL，不是对象
        if (streamInfo.rtc) {
          videoUrl = streamInfo.rtc
          playType = 'WebRTC'
        } else if (streamInfo.flv) {
          videoUrl = streamInfo.flv
          playType = 'FLV'
        } else if (streamInfo.ws_flv) {
          videoUrl = streamInfo.ws_flv
          playType = 'WS-FLV'
        } else if (streamInfo.hls) {
          videoUrl = streamInfo.hls
          playType = 'HLS'
        } else if (streamInfo.rtmp) {
          videoUrl = streamInfo.rtmp
          playType = 'RTMP'
        }
        
        if (!videoUrl) {
          console.error('[厂区监控] 未找到可用的播放URL，流信息:', streamInfo)
          console.error('[厂区监控] streamInfo所有字段:', Object.keys(streamInfo))
          throw new Error('未获取到可用的播放地址。可能原因：\n1. ZLMediaKit流媒体服务未启动\n2. 拉流代理未成功拉取RTSP流\n3. 摄像头RTSP地址不可达')
        }
        
        console.log(`[厂区监控] 使用${playType}播放，URL:`, videoUrl)
        
        // 🔑 第二阶段: 设置URL，触发v-if和组件mounted时的自动播放
        this.$set(this.playingVideos[camera.id], 'url', videoUrl)
        this.$set(this.playingVideos[camera.id], 'playType', playType)
        this.$set(this.playingVideos[camera.id], 'loading', false)
        
        if (!silent) {
          console.log(`[厂区监控] 摄像头播放成功(${playType}):`, camera.name || camera.stream)
          this.$message.success(`${camera.name || camera.stream} 开始播放(${playType})`)
        }
        
        // 释放API调用锁
        this.$set(this.apiCallLock, camera.id, false)
        return true
      } catch (error) {
        // 释放API调用锁
        this.$set(this.apiCallLock, camera.id, false)
        
        console.error('[厂区监控] 播放失败详情:', {
          cameraId: camera.id,
          cameraName: camera.name,
          cameraPulling: camera.pulling,
          errorMessage: error.message,
          errorStack: error.stack,
          errorResponse: error.response
        })
        
        // 🔑 优化: 分析错误原因，给出更友好的提示
        const errorMsg = error.response?.data?.msg || error.message || '播放失败'
        let friendlyMsg = errorMsg
        let suggestRemove = false  // 是否建议删除该代理
        
        // 识别常见错误类型
        if (errorMsg.includes('代理节点不存在') || errorMsg.includes('not found')) {
          friendlyMsg = '拉流代理不存在，可能已被删除'
          suggestRemove = true
        } else if (errorMsg.includes('RTSP') && (errorMsg.includes('超时') || errorMsg.includes('timeout') || errorMsg.includes('连接失败'))) {
          friendlyMsg = '摄像头RTSP连接失败，请检查摄像头是否在线'
        } else if (errorMsg.includes('401') || errorMsg.includes('Unauthorized') || errorMsg.includes('认证失败')) {
          friendlyMsg = '摄像头认证失败，请检查用户名密码'
        } else if (errorMsg.includes('404') || errorMsg.includes('Not Found')) {
          friendlyMsg = '摄像头地址不存在或已下线'
        } else if (errorMsg.includes('流不存在') || errorMsg.includes('stream not found')) {
          friendlyMsg = '拉流失败，摄像头可能已离线或RTSP地址错误'
        } else if (errorMsg.includes('ZLMediaKit') || errorMsg.includes('流媒体')) {
          friendlyMsg = 'ZLMediaKit流媒体服务异常，请检查服务状态'
        }
        
        console.warn('[厂区监控] 播放失败原因:', friendlyMsg)
        
        // 设置错误状态
        this.$set(this.playingVideos, camera.id, {
          id: camera.id,
          name: camera.name || camera.gbName,
          stream: camera.stream,
          loading: false,
          url: null,
          error: true,
          errorMsg: friendlyMsg,
          retrying: false,
          suggestRemove: suggestRemove  // 标记是否建议删除
        })
        
        if (!silent) {
          // 🔑 根据错误类型显示不同级别的提示
          if (suggestRemove) {
            this.$message.warning({
              message: `${camera.name || camera.stream}: ${friendlyMsg}`,
              duration: 5000,
              showClose: true
            })
          } else {
            this.$message.error({
              message: `${camera.name || camera.stream}: ${friendlyMsg}`,
              duration: 5000,
              showClose: true
            })
          }
        }
        
        // 不自动重试，避免大量错误请求
        return false
      }
    },

    // 停止单个摄像头
    async stopSingleCamera(camera) {
      try {
        console.log('[厂区监控] 停止摄像头播放:', camera.name, camera.id)
        
        // 先检查摄像头是否在播放列表中
        const isPlaying = this.playingCameras.includes(camera.id)
        console.log('[厂区监控] 摄像头播放状态:', { id: camera.id, isPlaying })
        
        // 🔑 优化: 无论后端停止是否成功，都清理本地播放器
        try {
          // 调用后端停止拉流
          await stopCameraPlay(camera.id)
          console.log('[厂区监控] 后端停止拉流成功:', camera.id)
        } catch (error) {
          console.warn('[厂区监控] 后端停止拉流失败（可能代理已不存在）:', error.message)
          
          // 🔑 特殊错误处理
          const errorMsg = error.response?.data?.msg || error.message || ''
          
          if (errorMsg.includes('代理节点不存在') || errorMsg.includes('not found') || errorMsg.includes('不存在')) {
            console.log('[厂区监控] 代理节点不存在，仅清理本地播放器')
            // 代理不存在时，静默处理，不显示错误提示
          } else if (errorMsg.includes('未在拉流') || errorMsg.includes('not pulling')) {
            console.log('[厂区监控] 代理未在拉流，仅清理本地播放器')
            // 未在拉流时，也静默处理
          } else {
            // 其他错误，记录但不影响本地清理
            console.error('[厂区监控] 停止拉流遇到未知错误:', errorMsg)
          }
        }
        
        // 🔑 关键: 无论后端是否成功，都清理本地播放器和数据
        this.closeSingleCamera(camera.id)
        
        // 只有在真正播放的情况下才显示成功提示
        if (isPlaying) {
          this.$message.success(`已停止播放: ${camera.name || camera.stream}`)
        }
      } catch (error) {
        // 最外层错误捕获（理论上不应该到这里）
        console.error('[厂区监控] 停止播放异常:', error)
        
        // 即使出错也要清理本地播放器
        this.closeSingleCamera(camera.id)
        
        // 不显示错误提示，避免用户困扰
        // this.$message.error(`停止播放失败: ${error.message}`)
      }
    },

    // 关闭单个视频
    closeSingleCamera(cameraId) {
      console.log('[厂区监控] 关闭摄像头:', cameraId)
      
      // 🔑 先暂停播放器
      const playerRef = this.$refs[`rtcPlayer_${cameraId}`]
      if (playerRef && playerRef.length > 0) {
        console.log('[厂区监控] 调用播放器pause()方法')
        try {
          playerRef[0].pause()
        } catch (error) {
          console.error('[厂区监控] 暂停播放器失败:', error)
        }
      } else {
        console.warn('[厂区监控] 未找到播放器ref:', `rtcPlayer_${cameraId}`)
      }
      
      // 🔑 等待一个tick再删除数据（确保pause执行完毕）
      this.$nextTick(() => {
        // 从播放列表移除
        this.$delete(this.playingVideos, cameraId)
        const index = this.playingCameras.indexOf(cameraId)
        if (index > -1) {
          this.playingCameras.splice(index, 1)
        }
        
        // 清理API调用锁
        this.$delete(this.apiCallLock, cameraId)
        
        console.log('[厂区监控] 摄像头已关闭:', cameraId)
      })
    },

    // 重新加载单个摄像头
    async reloadSingleCamera(cameraId) {
      const camera = this.cameras.find(c => c.id === cameraId)
      if (!camera) return

      // 先关闭
      this.closeSingleCamera(cameraId)
      
      // 延迟重新播放
      await new Promise(resolve => setTimeout(resolve, 500))
      await this.playSingleCamera(camera)
    },

    // 停止所有播放
    async stopAll() {
      console.log('[厂区监控] 停止所有播放，当前播放数量:', this.playingCameras.length)
      
      if (this.playingCameras.length === 0) {
        this.$message.warning('当前没有正在播放的视频')
        return
      }
      
      const playingCopy = [...this.playingCameras] // 复制数组，避免遍历时修改
      for (const cameraId of playingCopy) {
        const camera = this.cameras.find(c => c.id === cameraId)
        if (camera) {
          await this.stopSingleCamera(camera)
        } else {
          // 如果找不到camera对象，直接清理本地数据
          console.warn('[厂区监控] 找不到摄像头对象，直接清理:', cameraId)
          this.closeSingleCamera(cameraId)
        }
      }
      
      this.$message.success(`已停止所有播放 (${playingCopy.length}个)`)
    },

    // 停止所有正在播放的视频（销毁前清理）
    stopAllPlaying() {
      Object.keys(this.playingVideos).forEach(cameraId => {
        this.closeSingleCamera(parseInt(cameraId))
      })
    },

    // 播放器错误处理
    handlePlayerError(cameraId, error) {
      console.error('[厂区监控] 播放器错误:', cameraId, error)
      
      const videoInfo = this.playingVideos[cameraId]
      if (!videoInfo) return

      // 设置错误状态
      this.$set(this.playingVideos[cameraId], 'loading', false)
      this.$set(this.playingVideos[cameraId], 'error', true)
      this.$set(this.playingVideos[cameraId], 'errorMsg', error.message || '播放失败')

      // 不自动重试，避免循环调用
      // this.autoRetry(cameraId)
    },

    // 自动重试
    async autoRetry(cameraId) {
      if (!this.retryCount[cameraId]) {
        this.retryCount[cameraId] = 0
      }

      if (this.retryCount[cameraId] >= this.maxRetry) {
        console.warn('[厂区监控] 摄像头重试次数已达上限:', cameraId)
        this.$set(this.playingVideos[cameraId], 'errorMsg', `重试${this.maxRetry}次后仍失败`)
        return
      }

      this.retryCount[cameraId]++
      this.$set(this.playingVideos[cameraId], 'retrying', true)
      
      console.log('[厂区监控] 摄像头开始第', this.retryCount[cameraId], '次重试:', cameraId)

      // 等待2秒后重试
      await new Promise(resolve => setTimeout(resolve, 2000))
      
      const camera = this.cameras.find(c => c.id === cameraId)
      if (camera) {
        // 先关闭
        this.closeSingleCamera(cameraId)
        // 重新播放
        await this.playSingleCamera(camera, true)
      }
    },

    // 切换全屏
    toggleFullscreen() {
      this.isFullscreen = !this.isFullscreen
      console.log('[厂区监控] 切换全屏模式:', this.isFullscreen)
      
      if (this.isFullscreen) {
        const elem = document.querySelector('.video-area')
        if (elem) {
          if (elem.requestFullscreen) {
            elem.requestFullscreen()
          } else if (elem.webkitRequestFullscreen) {
            elem.webkitRequestFullscreen()
          } else if (elem.mozRequestFullScreen) {
            elem.mozRequestFullScreen()
          } else if (elem.msRequestFullscreen) {
            elem.msRequestFullscreen()
          }
        }
      } else {
        if (document.exitFullscreen) {
          document.exitFullscreen()
        } else if (document.webkitExitFullscreen) {
          document.webkitExitFullscreen()
        } else if (document.mozCancelFullScreen) {
          document.mozCancelFullScreen()
        } else if (document.msExitFullscreen) {
          document.msExitFullscreen()
        }
      }
    },

    // 切换画面布局
    changeGridLayout(layout) {
      console.log('[厂区监控] 切换画面布局:', layout)
      this.gridLayout = layout
      
      // 优化提示文本
      const layoutText = layout === 0 ? '全部' : `${layout}画`
      this.$message.success(`已切换到${layoutText}布局`)
    },

    // 开始自动更新
    startAutoUpdate() {
      this.updateTimer = setInterval(() => {
        if (this.selectedFactory) {
          console.log('[厂区监控] 定时任务：静默刷新摄像头列表')
          this.loadCameras(true) // 静默刷新，不显示错误提示
        }
      }, 10000) // 每10秒刷新一次
    },

    // 停止自动更新
    stopAutoUpdate() {
      if (this.updateTimer) {
        clearInterval(this.updateTimer)
        this.updateTimer = null
      }
    }
  }
}
</script>

<style scoped lang="scss">
.factory-item {
  padding: 12px;
  margin-bottom: 8px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
  border: 1px solid #e4e7ed;

  &:hover {
    background-color: #f5f7fa;
    border-color: #409eff;
  }

  &.active {
    background-color: #ecf5ff;
    border-color: #409eff;
  }

  .factory-info {
    display: flex;
    align-items: center;

    .factory-name {
      font-weight: 500;
      flex: 1;
    }
  }
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
  margin: 10px 10px 10px 10px;
  flex-shrink: 0;

  .toolbar-left {
    display: flex;
    align-items: center;
  }

  .toolbar-right {
    display: flex;
    gap: 10px;
    align-items: center;
  }
}

.camera-list {
  max-height: none;
  height: auto;
  overflow-y: visible;
  padding: 10px;
  margin: 0 10px 10px 10px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  background-color: #fff;
  flex-shrink: 0;
}

.camera-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px;
  margin-bottom: 8px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  transition: all 0.3s;

  &:hover {
    background-color: #f5f7fa;
  }

  &.playing {
    background-color: #f0f9ff;
    border-color: #409eff;
  }

  .camera-checkbox {
    flex: 1;
  }

  .camera-info {
    display: flex;
    align-items: center;
    gap: 8px;

    .camera-name {
      font-weight: 500;
    }
  }

  .camera-actions {
    margin-left: 10px;
  }
}

.video-area {
  margin: 0 10px 10px 10px;
  padding: 10px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  background-color: #fff;
  transition: all 0.3s ease;
  position: relative;
  display: flex;
  flex-direction: column;

  /* 全屏样式 */
  &.fullscreen {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 9999;
    background: #000;
    padding: 0;
    margin: 0;
    border: none;
    border-radius: 0;
    flex: 1;

    .video-area-header {
      background: rgba(0, 0, 0, 0.8);
      color: white;
      border-bottom-color: rgba(255, 255, 255, 0.1);
    }

    .video-grid {
      gap: 5px;
      padding: 5px;
      flex: 1;
      height: calc(100vh - 50px);
    }

    .video-item {
      border-radius: 0;
      
      .video-item-header {
        font-size: 12px;
        padding: 5px 10px;
        background-color: rgba(0, 0, 0, 0.8);
        border-bottom-color: rgba(255, 255, 255, 0.1);
      }
    }
  }
}

.video-area-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 15px;
  background: #fafafa;
  border-bottom: 1px solid #e8e8e8;
  margin: -10px -10px 10px -10px;

  .layout-info {
    font-weight: 500;
    color: #333;
  }
}

.video-grid {
  display: grid;
  gap: 15px;
  flex: 1;
  transition: all 0.3s ease;

  /* 单画面布局 (1x1) */
  &.grid-1 {
    grid-template-columns: 1fr;
    grid-template-rows: 1fr;
    
    .video-item {
      min-height: 400px;
    }
  }

  /* 4画面布局 (2x2) */
  &.grid-4 {
    grid-template-columns: repeat(2, 1fr);
    grid-auto-rows: minmax(300px, 1fr);
  }

  /* 9画面布局 (3x3) */
  &.grid-9 {
    grid-template-columns: repeat(3, 1fr);
    grid-auto-rows: minmax(250px, 1fr);
  }

  /* 16画面布局 (4x4) */
  &.grid-16 {
    grid-template-columns: repeat(4, 1fr);
    grid-auto-rows: minmax(200px, 1fr);
  }

  /* 全部画面布局 (自适应) */
  &.grid-0 {
    grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
    grid-auto-rows: minmax(200px, 1fr);
  }
}

.video-item {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
  background-color: #000;
  display: flex;
  flex-direction: column;
  position: relative;
  min-height: 200px;

  .video-item-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 8px 12px;
    background-color: rgba(0, 0, 0, 0.7);
    color: white;
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    z-index: 10;

    .video-title {
      font-weight: 500;
      flex: 1;
    }

    .video-item-actions {
      display: flex;
      gap: 5px;

      .el-button {
        padding: 5px;
        min-width: auto;
      }
    }
  }

  .video-container {
    width: 100%;
    height: 100%;
    background-color: #000;
    position: relative;
  }

  .video-player {
    width: 100%;
    height: 100%;
  }

  .video-loading,
  .video-error {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 100%;
    color: #909399;
    gap: 10px;
    padding: 20px;

    i {
      font-size: 48px;
      margin-bottom: 10px;
    }
    
    p {
      margin: 0;
      text-align: center;
    }
  }
  
  .video-loading {
    i {
      animation: spin 1s linear infinite;
    }
  }

  .video-error {
    color: #f56c6c;
    
    .error-message {
      font-size: 14px;
      line-height: 1.5;
      margin-bottom: 10px;
      max-width: 90%;
      word-break: break-word;
    }
    
    .error-actions {
      display: flex;
      gap: 10px;
      margin-top: 10px;
    }

    i {
      animation: none;
    }
  }

  .video-retrying {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    background: rgba(24, 144, 255, 0.9);
    color: white;
    padding: 10px 20px;
    border-radius: 4px;
    font-size: 14px;
    z-index: 20;
  }
}

/* 加载动画 */
@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 响应式优化 */
@media (max-width: 1366px) {
  .video-grid.grid-16 {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 1024px) {
  .video-grid.grid-9 {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .video-grid.grid-16 {
    grid-template-columns: repeat(2, 1fr);
  }
}

/* 滚动条样式 */
.camera-list::-webkit-scrollbar {
  width: 6px;
}

.camera-list::-webkit-scrollbar-thumb {
  background: #ddd;
  border-radius: 3px;
}

.camera-list::-webkit-scrollbar-thumb:hover {
  background: #ccc;
}
</style>
