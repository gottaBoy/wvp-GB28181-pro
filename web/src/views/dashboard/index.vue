<template>
  <div id="app" class="app-container" style="height: calc(100vh - 118px); background-color: rgba(242,242,242,0.50)">
    <el-row style="width: 100%;height: 100%;">
      <el-col :xl="{ span: 8 }" :lg="{ span: 8 }" :md="{ span: 12 }" :sm="{ span: 12 }" :xs="{ span: 24 }">
        <div id="ThreadsLoad" class="control-cell">
          <div style="width:100%; height:100%; ">
            <consoleCPU ref="consoleCPU" />
          </div>
        </div>
      </el-col>
      <el-col :xl="{ span: 8 }" :lg="{ span: 8 }" :md="{ span: 12 }" :sm="{ span: 12 }" :xs="{ span: 24 }">
        <div id="WorkThreadsLoad" class="control-cell">
          <div style="width:100%; height:100%; ">
            <consoleResource ref="consoleResource" />
          </div>
        </div>
      </el-col>
      <el-col :xl="{ span: 8 }" :lg="{ span: 8 }" :md="{ span: 12 }" :sm="{ span: 12 }" :xs="{ span: 24 }">
        <div id="WorkThreadsLoad" class="control-cell">
          <div style="width:100%; height:100%; ">
            <consoleNet ref="consoleNet" />
          </div>
        </div>
      </el-col>
      <el-col :xl="{ span: 8 }" :lg="{ span: 8 }" :md="{ span: 12 }" :sm="{ span: 12 }" :xs="{ span: 24 }">
        <div id="WorkThreadsLoad" class="control-cell">
          <div style="width:100%; height:100%; ">

            <consoleMem ref="consoleMem" />
          </div>
        </div>
      </el-col>
      <el-col :xl="{ span: 8 }" :lg="{ span: 8 }" :md="{ span: 12 }" :sm="{ span: 12 }" :xs="{ span: 24 }">
        <div id="WorkThreadsLoad" class="control-cell">
          <div style="width:100%; height:100%; ">
            <consoleNodeLoad ref="consoleNodeLoad" />
          </div>
        </div>
      </el-col>
      <el-col :xl="{ span: 8 }" :lg="{ span: 8 }" :md="{ span: 12 }" :sm="{ span: 12 }" :xs="{ span: 24 }">
        <div id="WorkThreadsLoad" class="control-cell">
          <div style="width:100%; height:100%; ">
            <consoleDisk ref="consoleDisk" />
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import consoleCPU from './console/ConsoleCPU.vue'
import consoleMem from './console/ConsoleMEM.vue'
import consoleNet from './console/ConsoleNet.vue'
import consoleNodeLoad from './console/ConsoleNodeLoad.vue'
import consoleDisk from './console/ConsoleDisk.vue'
import consoleResource from './console/ConsoleResource.vue'

export default {
  name: 'Dashboard',
  components: {
    consoleCPU,
    consoleMem,
    consoleNet,
    consoleNodeLoad,
    consoleDisk,
    consoleResource
  },
  data() {
    return {
      timer: null
    }
  },
  created() {
    // 延迟调用，确保组件已挂载
    this.$nextTick(() => {
      this.getSystemInfo()
      this.getLoad()
      this.getResourceInfo()
      this.loopForSystemInfo()
    })
  },
  destroyed() {
    window.clearImmediate(this.timer)
  },
  methods: {
    loopForSystemInfo: function() {
      if (this.timer != null) {
        window.clearTimeout(this.timer)
      }
      this.timer = setTimeout(() => {
        console.log(this.$route.name)
        if (this.$route.name === '控制台') {
          this.getSystemInfo()
          this.getLoad()
          this.timer = null
          this.loopForSystemInfo()
          this.getResourceInfo()
        }
      }, 2000)
    },
    getSystemInfo: function() {
      this.$store.dispatch('server/getSystemInfo')
        .then(data => {
          // 🔑 关键修复: 检查 ref 是否存在，避免 undefined 错误
          if (this.$refs.consoleCPU && typeof this.$refs.consoleCPU.setData === 'function') {
            this.$refs.consoleCPU.setData(data.cpu)
          }
          if (this.$refs.consoleMem && typeof this.$refs.consoleMem.setData === 'function') {
            this.$refs.consoleMem.setData(data.mem)
          }
          if (this.$refs.consoleNet && typeof this.$refs.consoleNet.setData === 'function') {
            this.$refs.consoleNet.setData(data.net, data.netTotal)
          }
          if (this.$refs.consoleDisk && typeof this.$refs.consoleDisk.setData === 'function') {
            this.$refs.consoleDisk.setData(data.disk)
          }
        })
        .catch(error => {
          console.error('[Dashboard] 获取系统信息失败:', error)
        })
    },
    getLoad: function() {
      this.$store.dispatch('server/getMediaServerLoad')
        .then(data => {
          // 🔑 关键修复: 检查 ref 是否存在，避免 undefined 错误
          if (this.$refs.consoleNodeLoad && typeof this.$refs.consoleNodeLoad.setData === 'function') {
            this.$refs.consoleNodeLoad.setData(data)
          }
        })
        .catch(error => {
          console.error('[Dashboard] 获取负载信息失败:', error)
        })
    },
    getResourceInfo: function() {
      this.$store.dispatch('server/getResourceInfo')
        .then(data => {
          // 🔑 关键修复: 检查 ref 是否存在，避免 undefined 错误
          if (this.$refs.consoleResource && typeof this.$refs.consoleResource.setData === 'function') {
            this.$refs.consoleResource.setData(data)
          } else {
            console.warn('[Dashboard] consoleResource ref 未就绪，延迟重试')
            // 如果组件未就绪，延迟重试
            setTimeout(() => {
              if (this.$refs.consoleResource && typeof this.$refs.consoleResource.setData === 'function') {
                this.$refs.consoleResource.setData(data)
              }
            }, 100)
          }
        })
        .catch(error => {
          console.error('[Dashboard] 获取资源信息失败:', error)
        })
    }
  }
}
</script>

<style>
#app {
  height: 100%;
}
.control-cell {
  padding-top: 10px;
  padding-left: 5px;
  padding-right: 10px;
  height: 360px;
}
</style>
