<template>
  <div id="h265Player" ref="container" style="background-color: #000000; " @dblclick="fullscreenSwich">
    <div id="glplayer" ref="playerBox" style="width: 100%; height: 100%; margin: 0 auto;" >
      <div v-if="playerLoading" class="play-loading">
        <i class="el-icon-loading" />
        视频加载中
      </div>
    </div>

    <div v-if="showButton" id="buttonsBox" class="buttons-box">
      <div class="buttons-box-left">
        <i v-if="!playing" class="iconfont icon-play h265web-btn" @click="unPause" />
        <i v-if="playing" class="iconfont icon-pause h265web-btn" @click="pause" />
        <i class="iconfont icon-stop h265web-btn" @click="destroy" />
        <i v-if="isNotMute" class="iconfont icon-audio-high h265web-btn" @click="mute()" />
        <i v-if="!isNotMute" class="iconfont icon-audio-mute h265web-btn" @click="cancelMute()" />
      </div>
      <div class="buttons-box-right">
        <!--          <i class="iconfont icon-file-record1 h265web-btn"></i>-->
        <!--          <i class="iconfont icon-xiangqing2 h265web-btn" ></i>-->
        <i
          class="iconfont icon-camera1196054easyiconnet h265web-btn"
          style="font-size: 1rem !important"
          @click="screenshot"
        />
        <i class="iconfont icon-shuaxin11 h265web-btn" @click="playBtnClick" />
        <i v-if="!fullscreen" class="iconfont icon-weibiaoti10 h265web-btn" @click="fullscreenSwich" />
        <i v-if="fullscreen" class="iconfont icon-weibiaoti11 h265web-btn" @click="fullscreenSwich" />
      </div>
    </div>
  </div>
</template>

<script>
const h265webPlayer = {}
/**
 * 从github上复制的
 * @see https://github.com/numberwolf/h265web.js/blob/master/example_normal/index.js
 */
const token = 'base64:QXV0aG9yOmNoYW5neWFubG9uZ3xudW1iZXJ3b2xmLEdpdGh1YjpodHRwczovL2dpdGh1Yi5jb20vbnVtYmVyd29sZixFbWFpbDpwb3JzY2hlZ3QyM0Bmb3htYWlsLmNvbSxRUTo1MzEzNjU4NzIsSG9tZVBhZ2U6aHR0cDovL3h2aWRlby52aWRlbyxEaXNjb3JkOm51bWJlcndvbGYjODY5NCx3ZWNoYXI6bnVtYmVyd29sZjExLEJlaWppbmcsV29ya0luOkJhaWR1'

// 配置WebAssembly内存限制，避免浏览器内存分配失败
// 注意：h265web.js的WebAssembly模块需要较大的内存，但浏览器可能无法分配2GB
// 此处不预配置wasmMemory，让missile.js自己管理，但通过修改INITIAL_TOTAL_MEMORY来控制

export default {
  name: 'H265web',
  props: ['videoUrl', 'error', 'hasAudio', 'height', 'showButton'],
  data() {
    return {
      playing: false,
      isNotMute: false,
      quieting: false,
      fullscreen: false,
      loaded: false, // mute
      speed: 0,
      kBps: 0,
      btnDom: null,
      videoInfo: null,
      volume: 1,
      rotate: 0,
      vod: true, // 点播
      forceNoOffscreen: false,
      playerWidth: 0,
      playerHeight: 0,
      inited: false,
      playerLoading: false,
      mediaInfo: null
    }
  },
  watch: {
    videoUrl(newData, oldData) {
      this.play(newData)
    },
    playing(newData, oldData) {
      this.$emit('playStatusChange', newData)
    },
    immediate: true
  },
    mounted() {
      // 🔑 关键修复: 在组件挂载时立即设置全局错误处理，捕获missile.js加载时的WebAssembly错误
      this.setupGlobalErrorHandlers()
      
      const paramUrl = decodeURIComponent(this.$route.params.url)
      this.$resizeHandler = () => {
        this.updatePlayerDomSize()
      }
      window.addEventListener('resize', this.$resizeHandler)
      this.btnDom = document.getElementById('buttonsBox')
      console.log('初始化时的地址为: ' + paramUrl)
      if (paramUrl) {
        this.play(this.videoUrl)
      }
    },
  destroyed() {
    // 移除window resize事件监听器
    if (this.$resizeHandler) {
      window.removeEventListener('resize', this.$resizeHandler)
    }
    // 移除全局错误处理
    if (this._globalErrorHandler) {
      window.removeEventListener('error', this._globalErrorHandler)
      this._globalErrorHandler = null
    }
    // 移除unhandledrejection事件监听器
    if (this._unhandledRejectionHandler) {
      window.removeEventListener('unhandledrejection', this._unhandledRejectionHandler)
      this._unhandledRejectionHandler = null
    }
    // 清理ResizeObserver
    if (this.parentNodeResizeObserver) {
      this.parentNodeResizeObserver.disconnect()
      this.parentNodeResizeObserver = null
    }
    if (h265webPlayer[this._uid] && typeof h265webPlayer[this._uid].release === 'function') {
      h265webPlayer[this._uid].release()
    }
    this.playing = false
    this.loaded = false
    this.playerLoading = false
  },
  methods: {
    setupGlobalErrorHandlers() {
      // 🔑 关键修复: 设置全局错误处理，捕获missile.js加载时的WebAssembly内存分配错误
      // 这个错误发生在页面加载时，早于组件创建
      if (this._globalErrorHandler) {
        return // 已经设置过了
      }
      
      // 保存原始错误处理
      const originalErrorHandler = window.onerror
      const originalUnhandledRejection = window.onunhandledrejection
      
      // 设置全局错误处理
      this._globalErrorHandler = (msg, url, line, col, error) => {
        const errorMsg = msg || (error && error.message) || (error && error.toString()) || ''
        
        // 检查是否是WebAssembly内存分配错误
        if (errorMsg.includes('WebAssembly.Memory') || 
            errorMsg.includes('could not allocate memory') || 
            errorMsg.includes('RangeError') ||
            (url && url.includes('missile.js'))) {
          console.error('[H265web] 检测到WebAssembly内存分配失败:', { msg, url, line, col, error })
          
          // 显示用户友好的错误提示
          if (this.$message) {
            this.$message.error('内存不足，无法使用H265web播放器。建议：1. 关闭其他播放器 2. 刷新页面 3. 使用其他播放器（如WebRTC或FLV）')
          }
          
          // 触发错误事件
          this.$emit('error', new Error(errorMsg))
          
          // 阻止默认错误处理（避免控制台显示错误）
          return true
        }
        
        // 其他错误继续使用原始处理
        if (originalErrorHandler) {
          return originalErrorHandler(msg, url, line, col, error)
        }
        return false
      }
      
      // 设置 unhandledrejection 处理
      this._unhandledRejectionHandler = (event) => {
        const errorMsg = event.reason?.message || event.reason?.toString() || ''
        
        if (errorMsg.includes('WebAssembly.Memory') || 
            errorMsg.includes('could not allocate memory') || 
            errorMsg.includes('RangeError')) {
          console.error('[H265web] 检测到WebAssembly内存分配失败 (Promise):', event.reason)
          
          if (this.$message) {
            this.$message.error('内存不足，无法使用H265web播放器。建议：1. 关闭其他播放器 2. 刷新页面 3. 使用其他播放器（如WebRTC或FLV）')
          }
          
          this.$emit('error', event.reason)
          event.preventDefault() // 阻止默认错误处理
        }
      }
      
      // 注册全局错误处理
      window.addEventListener('error', this._globalErrorHandler)
      window.addEventListener('unhandledrejection', this._unhandledRejectionHandler)
    },
    updatePlayerDomSize() {
      const dom = this.$refs.container
      if (!dom || !dom.parentNode) {
        return
      }
      
      let boxWidth, boxHeight
      try {
        if (!this.parentNodeResizeObserver) {
          this.parentNodeResizeObserver = new ResizeObserver(entries => {
            this.updatePlayerDomSize()
          })
          this.parentNodeResizeObserver.observe(dom.parentNode)
        }
        boxWidth = dom.parentNode.clientWidth
        boxHeight = dom.parentNode.clientHeight
      } catch (e) {
        console.warn('updatePlayerDomSize error:', e)
        return
      }
      
      let width = boxWidth
      let height = (9 / 16) * width
      if (boxHeight > 0 && boxWidth > boxHeight / 9 * 16) {
        height = boxHeight
        width = boxHeight / 9 * 16
      }

      const clientHeight = Math.min(document.body.clientHeight, document.documentElement.clientHeight)
      if (height > clientHeight) {
        height = clientHeight
        width = (16 / 9) * height
      }

      if (this.$refs.playerBox) {
        this.$refs.playerBox.style.width = width + 'px'
        this.$refs.playerBox.style.height = height + 'px'
      }
      this.playerWidth = width
      this.playerHeight = height
      if (this.playing && h265webPlayer[this._uid] && typeof h265webPlayer[this._uid].resize === 'function') {
        h265webPlayer[this._uid].resize(this.playerWidth, this.playerHeight)
      }
    },
    resize(width, height) {
      this.playerWidth = width
      this.playerHeight = height
      if (this.$refs.playerBox) {
        this.$refs.playerBox.style.width = width + 'px'
        this.$refs.playerBox.style.height = height + 'px'
      }
      if (this.playing && h265webPlayer[this._uid] && typeof h265webPlayer[this._uid].resize === 'function') {
        h265webPlayer[this._uid].resize(this.playerWidth, this.playerHeight)
      }
    },
    create(url) {
      this.playerLoading = true
      
      // 检查WebAssembly是否可用（避免内存错误）
      if (typeof WebAssembly === 'undefined') {
        this.$message.error('浏览器不支持WebAssembly，无法使用H265web播放器')
        this.playerLoading = false
        return
      }
      
      const options = {}
      try {
        // 🔑 关键修复: 使用 try-catch 包装，捕获WebAssembly内存分配失败
        h265webPlayer[this._uid] = new window.new265webjs(url, Object.assign(
          {
            player: 'glplayer', // 播放器容器id
            width: this.playerWidth,
            height: this.playerHeight,
            token: token,
            extInfo: {
              coreProbePart: 0.4,
              probeSize: 8192,
              ignoreAudio: this.hasAudio === null ? 0 : (this.hasAudio ? 0 : 1)
            }
          },
          options
        ))
      } catch (e) {
        console.error('H265web播放器初始化失败:', e)
        
        // 检查是否是WebAssembly内存错误
        const errorMsg = e.message || e.toString() || ''
        if (errorMsg.includes('WebAssembly.Memory') || errorMsg.includes('could not allocate memory') || errorMsg.includes('RangeError')) {
          this.$message.error('内存不足，无法使用H265web播放器。建议：1. 关闭其他播放器 2. 刷新页面 3. 使用其他播放器（如WebRTC或FLV）')
        } else {
          this.$message.error('H265web播放器初始化失败，请使用其他播放器')
        }
        this.playerLoading = false
        // 触发错误事件，让父组件知道
        this.$emit('error', e)
        return
      }
      
      // 🔑 关键修复: 添加 unhandledrejection 事件监听，捕获异步WebAssembly错误
      const unhandledRejectionHandler = (event) => {
        const errorMsg = event.reason?.message || event.reason?.toString() || ''
        if (errorMsg.includes('WebAssembly.Memory') || errorMsg.includes('could not allocate memory') || errorMsg.includes('RangeError')) {
          console.error('[H265web] WebAssembly内存分配失败:', event.reason)
          this.$message.error('内存不足，无法使用H265web播放器。建议：1. 关闭其他播放器 2. 刷新页面 3. 使用其他播放器（如WebRTC或FLV）')
          this.playerLoading = false
          this.$emit('error', event.reason)
          event.preventDefault() // 阻止默认错误处理
        }
      }
      
      window.addEventListener('unhandledrejection', unhandledRejectionHandler)
      
      // 在组件销毁时移除监听器
      this._unhandledRejectionHandler = unhandledRejectionHandler
      const h265web = h265webPlayer[this._uid]
      h265web.onOpenFullScreen = () => {
        this.fullscreen = true
      }
      h265web.onCloseFullScreen = () => {
        this.fullscreen = false
      }
      h265web.onReadyShowDone = () => {
        // 准备好显示了，尝试自动播放
        const result = h265web.play()
        this.playing = result
        this.playerLoading = false
      }
      h265web.onLoadFinish = () => {
        this.loaded = true
        // 可以获取mediaInfo
        // @see https://github.com/numberwolf/h265web.js/blob/8b26a31ffa419bd0a0f99fbd5111590e144e36a8/example_normal/index.js#L252C9-L263C11
        this.mediaInfo = h265web.mediaInfo()
      }
      h265web.onPlayTime = (videoPTS) => {
        this.$emit('playTimeChange', videoPTS * 1000)
      }
      h265web.do()
    },
    screenshot: function() {
      if (h265webPlayer[this._uid]) {
        const canvas = document.createElement('canvas')
        console.log(this.mediaInfo)
        canvas.width = this.mediaInfo.meta.size.width
        canvas.height = this.mediaInfo.meta.size.height
        h265webPlayer[this._uid].snapshot(canvas) // snapshot to canvas

        // 下载截图
        const link = document.createElement('a')
        link.download = 'screenshot.png'
        link.href = canvas.toDataURL('image/png').replace('image/png', 'image/octet-stream')
        link.click()
      }
    },
    playBtnClick: function(event) {
      this.play(this.videoUrl)
    },
    refresh: function() {
      this.play(this.videoUrl)
    },
    play: function(url) {
      if (h265webPlayer[this._uid]) {
        this.destroy()
      }
      if (!url) {
        return
      }
      if (this.playerWidth === 0 || this.playerHeight === 0) {
        this.updatePlayerDomSize()
        setTimeout(() => {
          this.play(url)
        }, 300)
        return
      }
      this.create(url)
    },
    unPause: function() {
      if (h265webPlayer[this._uid]) {
        h265webPlayer[this._uid].play()
        this.playing = h265webPlayer[this._uid].isPlaying()
      }
      this.err = ''
    },
    pause: function() {
      if (h265webPlayer[this._uid]) {
        h265webPlayer[this._uid].pause()
        this.playing = h265webPlayer[this._uid].isPlaying()
      }
      this.err = ''
    },
    mute: function() {
      if (h265webPlayer[this._uid]) {
        h265webPlayer[this._uid].setVoice(0.0)
        this.isNotMute = false
      }
    },
    cancelMute: function() {
      if (h265webPlayer[this._uid]) {
        h265webPlayer[this._uid].setVoice(1.0)
        this.isNotMute = true
      }
    },
    destroy: function() {
      if (h265webPlayer[this._uid]) {
        h265webPlayer[this._uid].release()
      }
      h265webPlayer[this._uid] = null
      this.playing = false
      this.err = ''
    },
    fullscreenSwich: function() {
      const isFull = this.isFullscreen()
      if (isFull) {
        h265webPlayer[this._uid].closeFullScreen()
      } else {
        h265webPlayer[this._uid].fullScreen()
      }
      this.fullscreen = !isFull
    },
    isFullscreen: function() {
      return document.fullscreenElement ||
        document.msFullscreenElement ||
        document.mozFullScreenElement ||
        document.webkitFullscreenElement || false
    },
    setPlaybackRate: function(speed) {
      h265webPlayer[this._uid].setPlaybackRate(speed)
    }
  }
}
</script>

<style>
/* 确保h265Player容器满屏显示 */
#h265Player {
  width: 100%;
  height: 100%;
  position: relative;
  overflow: hidden;
}

/* 确保glplayer满屏显示 */
#glplayer {
  width: 100% !important;
  height: 100% !important;
  margin: 0 !important;
  padding: 0 !important;
}

.play-loading {
  width: 100%;
  height: 100%;
  color: rgb(255, 255, 255);
  display: flex;
  align-items: center;
  margin: 0 auto;
  justify-content: center;
  font-size: 18px;
}
.buttons-box {
  width: 100%;
  height: 28px;
  background-color: rgba(43, 51, 63, 0.7);
  position: absolute;
  display: -webkit-box;
  display: -ms-flexbox;
  display: flex;
  left: 0;
  bottom: 0;
  user-select: none;
  z-index: 10;
}

.h265web-btn {
  width: 20px;
  color: rgb(255, 255, 255);
  line-height: 27px;
  margin: 0px 10px;
  padding: 0px 2px;
  cursor: pointer;
  text-align: center;
  font-size: 0.8rem !important;
}

.buttons-box-right {
  position: absolute;
  right: 0;
}
.player-loading {
  width: fit-content;
  height: 30px;
  position: absolute;
  left: calc(50% - 52px);
  top: calc(50% - 52px);
  color: #fff;
  font-size: 16px;
}
.player-loading i{
  font-size: 24px;
  line-height: 24px;
  text-align: center;
  display: block;
}
.player-loading span{
  display: inline-block;
  font-size: 16px;
  height: 24px;
  line-height: 24px;
}
</style>
