import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

/**
 * 响应拦截器 —— 统一解析后端 Result 包装。
 *
 * 后端返回结构：{ code, message, data }
 * - code === 200：成功，直接返回 data
 * - code !== 200：失败，弹出错误提示并 reject
 */
request.interceptors.response.use(
  res => {
    const body = res.data

    // 仅当响应体是 Result 结构（含 code 字段）时才解析
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 200) {
        // 成功：返回业务数据 data
        return body.data
      }
      // 失败：提示错误信息
      ElMessage.error(body.message || '操作失败')
      return Promise.reject(new Error(body.message || '操作失败'))
    }

    // 非 Result 结构（如 SSE 流式、文件下载）原样返回
    return res
  },
  err => {
    // HTTP 层错误（网络错误、404、500 等）
    const msg = err.response?.data?.message || err.message || '请求失败'
    ElMessage.error(msg)
    return Promise.reject(err)
  }
)

export default request
