<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:20px;">
      <h2>🤖 Agent 管理</h2>
      <el-button type="primary" @click="$router.push('/agents/create')">+ 新增 Agent</el-button>
    </div>

    <el-card shadow="never">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="agentCode" label="Agent 编码" width="160" />
        <el-table-column prop="agentName" label="显示名称" width="200" />
        <el-table-column prop="modelType" label="默认模型" width="100" />
        <el-table-column prop="version" label="版本" width="70" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : row.status === 2 ? 'warning' : 'info'">
              {{ row.status === 1 ? '启用' : row.status === 2 ? '草稿' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="180">
          <template #default="{ row }">{{ row.updatedAt || row.createdAt }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="340">
          <template #default="{ row }">
            <el-button size="small" @click="$router.push(`/agents/${row.id}/chat`)">💬 对话测试</el-button>
            <el-button size="small" @click="$router.push(`/agents/${row.id}/edit`)">✏️ 编辑</el-button>
            <el-button size="small" @click="handleClone(row)">📋 克隆</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top:16px;display:flex;justify-content:flex-end;">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { agentDefApi } from '../api/agentDef'

const tableData = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)

const loadData = async () => {
  loading.value = true
  try {
    const res = await agentDefApi.list({ page: page.value, size: size.value })
    tableData.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

const handleClone = async (row) => {
  await ElMessageBox.confirm('确定克隆此 Agent 吗？', '提示', { type: 'info' })
  await agentDefApi.clone(row.id)
  ElMessage.success('克隆成功')
  loadData()
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确定删除「${row.agentName}」吗？此操作不可恢复。`, '警告', { type: 'warning' })
  await agentDefApi.delete(row.id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(loadData)
</script>
