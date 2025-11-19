<template>
  <div class="vehicle-rtc-player" style="width: 100%; height: 100%; position: relative;">
    <video 
      :ref="playerId"
      controls 
      autoplay 
      playsinline
      style="width: 100%; height: 100%; object-fit: contain; background-color: #000;">
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
      timer: null
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
          this.$emit('play-success', { playerId: this.playerId })
        })

        this.webrtcPlayer.on(ZLMRTCClient.Events.WEBRTC_OFFER_ANWSER_EXCHANGE_FAILED, (e) => {
          console.error(`[${this.playerId}] offer/answer 交换失败:`, e)
          this.$emit('error', { type: 'OFFER_ANSWER_ERROR', message: 'offer/answer交换失败', detail: e })
          
          // 如果流不存在，可以尝试重连
          if (e.code === -400 && e.msg === '流不存在') {
            console.log(`[${this.playerId}] 流不存在，100ms后重试`)
            this.timer = setTimeout(() => {
              if (this.webrtcPlayer) {
                this.webrtcPlayer.close()
              }
              this.play(url)
            }, 100)
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
}

video {
  width: 100% !important;
  height: 100% !important;
  object-fit: contain !important;
  background-color: #000;
}
</style>
