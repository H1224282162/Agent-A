<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px;">
      <h3>知识库管理</h3>
      <el-button type="primary" @click="$router.push('/knowledge/create')">+ 新建知识库</el-button>
    </div>

    <el-card shadow="never">
      <div style="display:flex;gap:12px;margin-bottom:16px;">
        <el-input v-model="keyword" placeholder="输入编码或名称搜索" clearable style="width:300px;" @keyup.enter="handleSearch" />
        <el-button type="primary" @click="handleSearch">搜索</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="kbCode" label="编码" width="160" />
        <el-table-column prop="kbName" label="名称" />
        <el-table-column prop="chunkStrategy" label="分块策略" width="100" />
        <el-table-column prop="topK" label="召回数" width="80" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" width="160" />
        <el-table-column label="操作" width="260">
          <template #default="{ row }">
            <el-button size="small" @click="$router.push(`/knowledge/${row.id}/edit`)">编辑</el-button>
            <el-button size="small" type="primary" @click="$router.push(`/knowledge/${row.id}/documents`)">文档</el-button>
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { knowledgeBaseApi } from '../api/knowledgeBase'

const tableData = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const keyword = ref('')

const loadData = async () => {
  loading.value = true
  try {
    const res = await knowledgeBaseApi.list({ page: page.value, size: size.value, keyword: keyword.value })
    tableData.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  page.value = 1
  loadData()
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除知识库 "${row.kbName}" 吗？`, '提示', { type: 'warning' })
    await knowledgeBaseApi.delete(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(loadData)
</script>
