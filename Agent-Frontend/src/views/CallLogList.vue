<template>
  <div>
    <h2 style="margin-bottom:20px;">📋 调用日志</h2>

    <el-card shadow="never">
      <el-form inline style="margin-bottom:16px;">
        <el-form-item label="Agent">
          <el-select v-model="filterAgentId" clearable placeholder="全部" @change="loadData" style="width:200px;">
            <el-option v-for="a in agents" :key="a.id" :label="a.agentName" :value="a.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button @click="loadData">查询</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="Agent" width="160">
          <template #default="{ row }">
            {{ agentsMap[row.agentId] || 'ID:' + row.agentId }}
          </template>
        </el-table-column>
        <el-table-column prop="sessionId" label="会话 ID" width="160" show-overflow-tooltip />
        <el-table-column prop="userInput" label="用户输入" min-width="200" show-overflow-tooltip />
        <el-table-column prop="agentOutput" label="Agent 回复" min-width="250" show-overflow-tooltip />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'success' ? 'success' : 'danger'" size="small">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="latencyMs" label="耗时" width="80">
          <template #default="{ row }">{{ row.latencyMs }}ms</template>
        </el-table-column>
        <el-table-column label="时间" width="180">
          <template #default="{ row }">{{ row.createdAt }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="showDetail(row)">详情</el-button>
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

      <!-- 详情弹窗 -->
      <el-dialog v-model="dialogVisible" title="调用详情" width="700px">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="Agent">{{ agentsMap[detail.agentId] }}</el-descriptions-item>
          <el-descriptions-item label="会话 ID">{{ detail.sessionId }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ detail.status }}</el-descriptions-item>
          <el-descriptions-item label="耗时">{{ detail.latencyMs }}ms</el-descriptions-item>
          <el-descriptions-item label="时间">{{ detail.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="工具调用">{{ detail.toolCalls || '无' }}</el-descriptions-item>
        </el-descriptions>
        <div style="margin-top:16px;">
          <div style="font-weight:bold;margin-bottom:4px;">用户输入：</div>
          <div style="background:#f5f7fa;padding:12px;border-radius:4px;white-space:pre-wrap;">{{ detail.userInput }}</div>
        </div>
        <div style="margin-top:12px;">
          <div style="font-weight:bold;margin-bottom:4px;">Agent 回复：</div>
          <div style="background:#f5f7fa;padding:12px;border-radius:4px;white-space:pre-wrap;">{{ detail.agentOutput }}</div>
        </div>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { callLogApi } from '../api/callLog'
import { agentDefApi } from '../api/agentDef'

const tableData = ref([])
const loading = ref(false)
const filterAgentId = ref(null)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const agents = ref([])
const agentsMap = computed(() => {
  const m = {}
  agents.value.forEach(a => { m[a.id] = a.agentName })
  return m
})

const dialogVisible = ref(false)
const detail = ref({})

const loadData = async () => {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (filterAgentId.value) params.agentId = filterAgentId.value
    const res = await callLogApi.list(params)
    tableData.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

const showDetail = async (row) => {
  detail.value = await callLogApi.getById(row.id)
  dialogVisible.value = true
}

onMounted(async () => {
  const agentRes = await agentDefApi.list({ page: 1, size: 100 })
  agents.value = agentRes.records
  loadData()
})
</script>
