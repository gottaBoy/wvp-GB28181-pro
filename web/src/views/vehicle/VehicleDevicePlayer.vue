<template>
  <div id="vehicleDevicePlayer" v-loading="isLoging">
    <el-dialog
      v-if="showVideoDialog"
      v-el-drag-dialog
      title="车辆相机播放"
      top="0"
      append-to-body
      width="40vw"
      :close-on-click-modal="false"
      :visible.sync="showVideoDialog"
      @close="close()"
    >
      <div style="width: 100%; ">
        <!-- 只显示WebRTC播放器 -->
        <rtc-player
          ref="webRTC"
          :visible.sync="showVideoDialog"
          :video-url="videoUrl"
          :error="videoError"
          :message="videoError"
          style="width: 100%; height: 60vh;"
          :has-audio="hasAudio"
          fluent
          autoplay
          live
        />
      </div>
      
      <div id="shared" style="text-align: right; margin-top: 1rem;">
        <el-tabs v-model="tabActiveName">
          <el-tab-pane label="实时视频" name="media">
            <div style="display: flex; margin-bottom: 0.5rem; height: 2.5rem;">
              <span style="width: 5rem; line-height: 2.5rem; text-align: right;">播放地址：</span>
              <el-input v-model="videoUrl" :disabled="true">
                <template slot="append">
                  <i
                    class="cpoy-btn el-icon-document-copy"
                    title="点击拷贝"
                    style="cursor: pointer"
                    @click="copyUrl(videoUrl)"
                  />
                </template>
              </el-input>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import rtcPlayer from '../common/rtcPlayer.vue'

export default {
  name: 'VehicleDevicePlayer',
  components: {
    rtcPlayer
  },
  data() {
    return {
      videoUrl: '',
      showVideoDialog: false,
      streamId: '',
      app: '',
      mediaServerId: '',
      tabActiveName: 'media',
      hasAudio: true,
      isLoging: false,
      streamInfo: null
    }
  },
  methods: {
    openDialog: function(tab, deviceId, channelId, param) {
      if (this.showVideoDialog) {
        return
      }
      this.tabActiveName = tab
      this.streamId = ''
      this.mediaServerId = ''
      this.app = ''
      this.videoUrl = ''
      
      if (this.$refs.webRTC) {
        this.$refs.webRTC.pause()
      }
      
      switch (tab) {
        case 'media':
          this.play(param.streamInfo, param.hasAudio)
          break
        case 'streamPlay':
          this.tabActiveName = 'media'
          this.play(param.streamInfo, param.hasAudio)
          break
      }
    },
    
    play: function(streamInfo, hasAudio) {
      this.streamInfo = streamInfo
      this.hasAudio = hasAudio !== undefined ? hasAudio : true
      this.isLoging = false
      
      // 获取WebRTC URL
      this.videoUrl = this.getUrlByStreamInfo()
      this.streamId = streamInfo.stream
      this.app = streamInfo.app
      this.mediaServerId = streamInfo.mediaServerId
      
      console.log('VehicleDevicePlayer - 播放URL:', this.videoUrl)
      
      this.playFromStreamInfo(false, streamInfo)
    },
    
    getUrlByStreamInfo() {
      let streamInfo = this.streamInfo
      if (this.streamInfo.transcodeStream) {
        streamInfo = this.streamInfo.transcodeStream
      }
      
      let videoUrl
      // 优先使用rtc链接
      if (streamInfo.rtc) {
        videoUrl = streamInfo.rtc
      } else if (streamInfo.rtcs) {
        videoUrl = streamInfo.rtcs
      } else if (streamInfo.ws_flv) {
        videoUrl = streamInfo.ws_flv
      } else if (streamInfo.wss_flv) {
        videoUrl = streamInfo.wss_flv
      } else if (streamInfo.flv) {
        videoUrl = streamInfo.flv
      }
      
      return videoUrl
    },

    playFromStreamInfo: function(realHasAudio, streamInfo) {
      this.showVideoDialog = true
      this.hasAudio = realHasAudio && this.hasAudio
      
      if (this.$refs.webRTC) {
        this.$refs.webRTC.play(this.getUrlByStreamInfo(streamInfo))
      } else {
        this.$nextTick(() => {
          this.$refs.webRTC.play(this.getUrlByStreamInfo(streamInfo))
        })
      }
    },
    
    close: function() {
      console.log('关闭视频')
      if (this.$refs.webRTC) {
        this.$refs.webRTC.pause()
      }
      this.videoUrl = ''
      this.showVideoDialog = false
    },
    
    videoError: function(e) {
      console.log('播放器错误：' + JSON.stringify(e))
    },
    
    copyUrl: function(url) {
      const input = document.createElement('input')
      input.value = url
      document.body.appendChild(input)
      input.select()
      document.execCommand('copy')
      document.body.removeChild(input)
      this.$message.success('复制成功')
    }
  }
}
</script>

<style scoped>
.cpoy-btn {
  cursor: pointer;
}

.cpoy-btn:hover {
  color: #409eff;
}
</style>
