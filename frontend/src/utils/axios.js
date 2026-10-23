import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

/**
 * 统一的 axios 实例。
 *
 * 约定：后端所有接口都返回 { code, message, data }，
 * code === 200 才算成功，业务错误也走 HTTP 200。
 */
const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截：自动带上 JWT
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截：剥离 Result 外壳，业务代码直接拿 data
request.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && body.code === 200) {
      return body.data
    }

    // 未登录 / token 过期：清掉本地状态并回登录页
    if (body && body.code === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      ElMessage.error(body.message || '登录已过期，请重新登录')
      const current = router.currentRoute.value
      router.push({ name: 'login', query: { redirect: current.fullPath } })
      return Promise.reject(new Error(body.message || '未登录'))
    }

    ElMessage.error((body && body.message) || '请求失败')
    return Promise.reject(new Error((body && body.message) || '请求失败'))
  },
  (error) => {
    // 网络错误、超时、5xx 等
    const message =
      error.response?.data?.message ||
      (error.code === 'ECONNABORTED' ? '请求超时，请稍后重试' : error.message) ||
      '网络异常'
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export { request }
export default request
