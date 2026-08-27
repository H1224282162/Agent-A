<template>
  <div>
    <div style="display:flex;align-items:center;gap:12px;margin-bottom:16px;">
      <el-button text @click="$router.back()">← 返回</el-button>
      <h3>{{ kbName || '文档管理' }}</h3>
    </div>

    <el-card shadow="never" style="margin-bottom:16px;">
      <el-upload
        :action="uploadAction"
        :http-request="customUpload"
        :show-file-list="false"
        :before-upload="beforeUpload"
        accept=".txt,.md,.markdown,.pdf,.doc,.docx"
      >
        <el-button type="primary" :loading="uploading">+ 上传文档</el-button>
        <template #tip>
          <div style="color:#909399;font-size:12px;">支持 TXT、Markdown、PDF、Word（docx）格式</div>
        </template>
      </el-upload>
    </el-card>

    <el-card shadow="never">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="docName" label="文件名" />
        <el-table-column prop="fileType" label="类型" width="80" />
        <el-table-column prop="fileSize" label="大小" width="100">
          <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column prop="chunkCount" label="分块数" width="80" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="上传时间" width="160" />
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button size="small" @click="openChunks(row)">分块</el-button>
            <el-button size="small" @click="handleReparse(row)" :loading="row.reparsing">重解析</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page"
        :page-size="size"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="loadData"
        style="margin-top:16px;"
      />
    </el-card>

    <!-- 分块预览弹窗 -->
    <el-dialog v-model="chunkDialogVisible" title="分块预览" width="800px">
      <el-table :data="chunkData" v-loading="chunkLoading" stripe height="500px">
        <el-table-column prop="chunkIndex" label="序号" width="60" />
        <el-table-column prop="content" label="内容">
          <template #default="{ row }">
            <div style="max-height:120px;overflow:hidden;text-overflow:ellipsis;">{{ row.content }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="tokenCount" label="Token" width="80" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { knowledgeBaseApi } from '../api/knowledgeBase'
import { knowledgeDocumentApi } from '../api/knowledgeDocument'
import { knowledgeChunkApi } from '../api/knowledgeChunk'

const route = useRoute()
const kbId = route.params.id
const kbName = ref('')

const tableData = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const uploading = ref(false)

const chunkDialogVisible = ref(false)
const chunkLoading = ref(false)
const chunkData = ref([])

const loadKbName = async () => {
  try {
    const res = await knowledgeBaseApi.getById(kbId)
    kbName.value = res.kbName
  } catch (e) {
    // ignore
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await knowledgeDocumentApi.list({ kbId, page: page.value, size: size.value })
    tableData.value = res.records.map(r => ({ ...r, reparsing: false }))
    total.value = res.total
  } finally {
    loading.value = false
  }
}

const beforeUpload = (file) => {
  const valid = /\.(txt|md|markdown|pdf|doc|docx)$/i.test(file.name)
  if (!valid) {
    ElMessage.error('不支持的文件类型')
    return false
  }
  return true
}

const customUpload = async (options) => {
  uploading.value = true
  try {
    await knowledgeDocumentApi.upload(kbId, options.file)
    ElMessage.success('上传成功')
    loadData()
  } catch (e) {
    ElMessage.error('上传失败')
  } finally {
    uploading.value = false
  }
}

const handleReparse = async (row) => {
  row.reparsing = true
  try {
    await knowledgeDocumentApi.reparse(row.id)
    ElMessage.success('重新解析已触发')
    loadData()
  } catch (e) {
    ElMessage.error('重解析失败')
  } finally {
    row.reparsing = false
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除文档 "${row.docName}" 吗？`, '提示', { type: 'warning' })
    await knowledgeDocumentApi.delete(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

const openChunks = async (row) => {
  chunkDialogVisible.value = true
  chunkLoading.value = true
  try {
    const res = await knowledgeChunkApi.list({ docId: row.id, page: 1, size: 100 })
    chunkData.value = res.records
  } catch (e) {
    ElMessage.error('加载分块失败')
  } finally {
    chunkLoading.value = false
  }
}

const formatSize = (bytes) => {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(1) + ' MB'
}

const statusText = (status) => {
  switch (status) {
    case 0: return '待解析'
    case 1: return '解析中'
    case 2: return '已完成'
    case 9: return '失败'
    default: return '未知'
  }
}

const statusType = (status) => {
  switch (status) {
    case 2: return 'success'
    case 9: return 'danger'
    case 1: return 'warning'
    default: return 'info'
  }
}

onMounted(() => {
  loadKbName()
  loadData()
})
</script>
