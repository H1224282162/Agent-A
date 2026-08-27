<template>
  <div>
    <h2 style="margin-bottom:20px;">🔧 工具管理</h2>
    <el-alert title="工具由 ToolScanner 在应用启动时自动从 @Tool 注解扫描同步，无需手动添加。" type="info" :closable="false" style="margin-bottom:16px;" />

    <el-card shadow="never">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="toolName" label="方法名" width="200" />
        <el-table-column prop="displayName" label="显示名称" width="140" />
        <el-table-column prop="category" label="分类" width="100">
          <template #default="{ row }">
            <el-tag :type="row.category === '业务工具' ? 'success' : row.category === '运维工具' ? 'warning' : ''">
              {{ row.category }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="280" show-overflow-tooltip />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              @change="() => handleToggle(row)"
              active-text="启用"
              inactive-text="禁用"
            />
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top:16px;display:flex;justify-content:flex-end;">
        <el-pagination
          v-model:current-page="page"
          :page-size="size"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { toolDefApi } from '../api/toolDef'

const tableData = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)

const loadData = async () => {
  loading.value = true
  try {
    const res = await toolDefApi.list({ page: page.value, size: size.value })
    tableData.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

const handleToggle = async (row) => {
  await toolDefApi.toggle(row.id)
  ElMessage.success(`已${row.status === 1 ? '禁用' : '启用'}工具：${row.toolName}`)
  loadData()
}

onMounted(loadData)
</script>
