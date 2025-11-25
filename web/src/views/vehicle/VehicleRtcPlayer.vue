<template>
  <div class="vehicle-rtc-player">
    <video 
      :ref="playerId"
      controls 
      autoplay 
      playsinline
      class="video-element">
      Your browser is too old which doesn't support HTML5 video.
    </video>
  </div>
</template>

<script>
export default {
  name: 'VehicleRtcPlayer',
  props: {
    videoUrl: {
      type: String,
      default: ''
    },
    hasAudio: {
      type: Boolean,
      default: true
    },
    playerId: {
      type: String,
      required: true
    }
  },
  data() {
    return {
      webrtcPlayer: null,
      timer: null,
      retryCount: 0, // 重试次数
      maxRetries: 5, // 最大重试次数
      currentUrl: null // 当前播放的URL
    }
  },
  watch: {
    videoUrl(newUrl, oldUrl) {
      if (newUrl && newUrl !== oldUrl) {
        console.log(`[${this.playerId}] URL变化，重新播放:`, newUrl)
        this.pause()
        this.play(newUrl)
      }
    }
  },
  mounted() {
    console.log(`[${this.playerId}] 组件已挂载`)
    // 🔑 关键修复: 如果组件挂载时已有URL，立即播放
    if (this.videoUrl) {
      console.log(`[${this.playerId}] 组件挂载时已有URL，立即播放:`, this.videoUrl)
      this.$nextTick(() => {
        this.play(this.videoUrl)
      })
    }
  },
  beforeDestroy() {
    console.log(`[${this.playerId}] 组件即将销毁，清理播放器`)
    this.pause()
    clearTimeout(this.timer)
  },
  methods: {
    play: function(url) {
      if (!url) {
        console.warn(`[${this.playerId}] 播放URL为空`)
        return
      }

      // 如果URL变化，重置重试计数器
      if (this.currentUrl !== url) {
        this.retryCount = 0
      }
      
      // 保存当前URL，用于重试
      this.currentUrl = url

      console.log(`[${this.playerId}] 开始播放:`, url)
      
      // 先清理旧的播放器
      if (this.webrtcPlayer) {
        console.log(`[${this.playerId}] 清理旧播放器`)
        try {
          this.webrtcPlayer.close()
        } catch (e) {
          console.error(`[${this.playerId}] 清理旧播放器失败:`, e)
        }
        this.webrtcPlayer = null
      }
      
      // 清除之前的重试定时器
      if (this.timer) {
        clearTimeout(this.timer)
        this.timer = null
      }

      // 获取video元素
      const videoElement = this.$refs[this.playerId]
      if (!videoElement) {
        console.error(`[${this.playerId}] 找不到video元素`)
        return
      }

      console.log(`[${this.playerId}] 创建WebRTC播放器`)
      
      try {
        this.webrtcPlayer = new ZLMRTCClient.Endpoint({
          element: videoElement,
          debug: false, // 关闭调试日志，避免控制台刷屏
          zlmsdpUrl: url,
          simulecast: false,
          useCamera: false,
          audioEnable: this.hasAudio,
          videoEnable: true,
          recvOnly: true,
          usedatachannel: false
        })

        this.webrtcPlayer.on(ZLMRTCClient.Events.WEBRTC_ICE_CANDIDATE_ERROR, (e) => {
          console.error(`[${this.playerId}] ICE 协商出错:`, e)
          this.$emit('error', { type: 'ICE_ERROR', message: 'ICE协商出错', detail: e })
        })

        this.webrtcPlayer.on(ZLMRTCClient.Events.WEBRTC_ON_REMOTE_STREAMS, (e) => {
          console.log(`[${this.playerId}] 播放成功，获取到远端流`)
          this.retryCount = 0 // 播放成功，重置重试计数器
          this.$emit('play-success', { playerId: this.playerId })
        })

        this.webrtcPlayer.on(ZLMRTCClient.Events.WEBRTC_OFFER_ANWSER_EXCHANGE_FAILED, (e) => {
          console.error(`[${this.playerId}] offer/answer 交换失败:`, e)
          
          // 处理各种错误情况，添加重试逻辑
          if (e.code === -400) {
            const errorMsg = e.msg || ''
            // 流不存在或没有活跃的track，可能是流还未准备好，延迟重试
            if (errorMsg.includes('流不存在') || 
                errorMsg.includes('活跃的track') || 
                errorMsg.includes('active track') ||
                errorMsg.includes('have_active_media') ||
                errorMsg.includes('活跃的')) {
              
              // 检查是否超过最大重试次数
              if (this.retryCount >= this.maxRetries) {
                console.error(`[${this.playerId}] 已达到最大重试次数(${this.maxRetries})，停止重试`)
                this.$emit('error', { 
                  type: 'OFFER_ANSWER_ERROR', 
                  message: `流未准备好，已重试${this.maxRetries}次。可能原因：\n1. 流尚未开始推流\n2. 推流服务异常\n3. 网络连接问题`, 
                  detail: e 
                })
                this.retryCount = 0 // 重置计数器
                return
              }
              
              // 指数退避策略：第1次1秒，第2次2秒，第3次4秒，以此类推
              const retryDelay = Math.min(1000 * Math.pow(2, this.retryCount), 8000) // 最大8秒
              this.retryCount++
              
              console.log(`[${this.playerId}] 流未准备好 (${errorMsg})，${retryDelay}ms后进行第${this.retryCount}次重试（共${this.maxRetries}次）`)
              
              // 清除之前的定时器
              if (this.timer) {
                clearTimeout(this.timer)
              }
              
              this.timer = setTimeout(() => {
                if (this.webrtcPlayer) {
                  try {
                    this.webrtcPlayer.close()
                  } catch (err) {
                    console.error(`[${this.playerId}] 关闭播放器失败:`, err)
                  }
                  this.webrtcPlayer = null
                }
                // 重试播放（使用保存的URL）
                if (this.currentUrl) {
                  this.play(this.currentUrl)
                } else {
                  this.play(url)
                }
              }, retryDelay)
              
              // 发送警告事件，但不阻止重试
              this.$emit('error', { 
                type: 'OFFER_ANSWER_ERROR', 
                message: `流未准备好，正在重试 (${this.retryCount}/${this.maxRetries})...`, 
                detail: e,
                retrying: true
              })
            } else {
              // 其他错误，不重试
              console.error(`[${this.playerId}] 不可恢复的错误:`, errorMsg)
              this.$emit('error', { type: 'OFFER_ANSWER_ERROR', message: 'offer/answer交换失败', detail: e })
              this.retryCount = 0 // 重置计数器
            }
          } else {
            // 非-400错误，不重试
            this.$emit('error', { type: 'OFFER_ANSWER_ERROR', message: 'offer/answer交换失败', detail: e })
            this.retryCount = 0 // 重置计数器
          }
        })

        this.webrtcPlayer.on(ZLMRTCClient.Events.WEBRTC_ON_LOCAL_STREAM, (s) => {
          console.log(`[${this.playerId}] 获取到本地流`)
        })

        console.log(`[${this.playerId}] WebRTC播放器创建成功`)
      } catch (error) {
        console.error(`[${this.playerId}] 创建播放器失败:`, error)
        this.$emit('error', { type: 'CREATE_ERROR', message: '创建播放器失败', detail: error })
      }
    },
    
    pause: function() {
      console.log(`[${this.playerId}] 暂停播放`)
      
      // 清除重试定时器
      if (this.timer) {
        clearTimeout(this.timer)
        this.timer = null
      }
      
      // 重置重试计数器
      this.retryCount = 0
      
      if (this.webrtcPlayer) {
        try {
          this.webrtcPlayer.close()
        } catch (e) {
          console.error(`[${this.playerId}] 关闭播放器失败:`, e)
        }
        this.webrtcPlayer = null
      }
    }
  }
}
</script>

<style scoped>
.vehicle-rtc-player {
  width: 100%;
  height: 100%;
  position: relative;
  overflow: hidden;
  background-color: #000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.video-element {
  width: 100%;
  height: 100%;
  object-fit: cover; /* 改为 cover 以填充满容器 */
  background-color: #000;
  display: block;
}
</style>
