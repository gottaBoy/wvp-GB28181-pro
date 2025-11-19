import axios from 'axios'
import { MessageBox, Message } from 'element-ui'
import store from '@/store'
import { getToken } from '@/utils/auth'

// create an axios instance
// 生产环境: baseURL为空字符串，请求路径为 /api/xxx，由nginx代理到后端
// 开发环境: baseURL为 /dev-api，由webpack devServer代理到后端
const service = axios.create({
  baseURL: process.env.VUE_APP_BASE_API || (process.env.NODE_ENV === 'production' ? '' : '/dev-api'),
  // withCredentials: true, // send cookies when cross-domain requests
  timeout: 30000 // request timeout
})

// request interceptor
service.interceptors.request.use(
  config => {
    // do something before request is sent
    if (store.getters.token && config.url.indexOf('api/user/login') < 0) {
      config.headers['access-token'] = getToken()
    }
    return config
  },
  error => {
    // do something with request error
    console.log(error) // for debug
    return Promise.reject(error)
  }
)

// response interceptor
service.interceptors.response.use(
  /**
   * If you want to get http information such as headers or status
   * Please return  response => response
  */

  /**
   * Determine the request status by custom code
   * Here is just an example
   * You can also judge the status by HTTP Status Code
   */
  response => {
    if (response.config.url.indexOf('/api/user/logout') >= 0) {
      return
    }
    const res = response.data
    if (res.code && res.code !== 200) {
      // 优化推流相关的错误提示
      let errorMsg = res.msg
      
      // 特殊处理推流播放的"设备不存在"错误
      if (response.config.url.indexOf('/api/push/start') >= 0 && errorMsg.includes('设备不存在')) {
        errorMsg = '推流通道关联的GB28181设备不存在或未注册。\n请检查：\n1. 设备是否已正确注册到平台\n2. 推流通道的国标编码配置是否正确\n3. 或者取消此通道的"拉起离线推流"设置'
      } else if (response.config.url.indexOf('/api/push/start') >= 0 && errorMsg.includes('通道未推流')) {
        errorMsg = '推流通道当前未推流。\n请确认：\n1. 推流源是否正常运行\n2. 推流地址是否正确\n3. 网络连接是否正常'
      } else if (response.config.url.indexOf('/api/push/start') >= 0 && (errorMsg.includes('timeout') || errorMsg.includes('超时'))) {
        errorMsg = '等待推流超时，推流通道未能及时上线'
      }
      // 车辆相机播放错误：静默处理（不显示弹窗提示）
      else if (response.config.url.indexOf('/api/vehicle/') >= 0 && response.config.url.indexOf('/webrtc/play') >= 0) {
        console.warn('车辆相机播放失败（静默）:', errorMsg)
        return res // 直接返回响应，不显示错误提示
      }
      
      Message({
        message: errorMsg,
        type: 'error',
        duration: 5 * 1000
      })
    } else {
      return res
    }
  },
  error => {
    console.log(error) // for debug
    if (error.response.status === 401) {
      // to re-login
      MessageBox.confirm('登录已经到期， 是否重新登录', '登录确认', {
        confirmButtonText: '重新登录',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        store.dispatch('user/resetToken').then(() => {
          location.reload()
        })
      })
    } else {
      Message({
        message: error.message,
        type: 'error',
        duration: 5 * 1000
      })
    }
    // return Promise.reject(error)
  }
)

export default service
