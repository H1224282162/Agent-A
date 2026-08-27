<template>
  <div>
    <h2 style="margin-bottom:20px;">📊 首页概览</h2>

    <el-row :gutter="20">
      <el-col :span="6" v-for="card in cards" :key="card.title">
        <el-card shadow="hover" style="margin-bottom:20px;">
          <div style="display:flex;align-items:center;justify-content:space-between;">
            <div>
              <div style="color:#909399;font-size:14px;">{{ card.title }}</div>
              <div style="font-size:28px;font-weight:bold;margin-top:8px;">{{ card.value }}</div>
            </div>
            <el-icon :size="40" :color="card.color"><component :is="card.icon" /></el-icon>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" style="margin-top:20px;">
      <template #header>📋 快速导航</template>
      <el-row :gutter="12">
        <el-col :span="8" v-for="nav in navs" :key="nav.path">
          <el-button style="width:100%;height:80px;" @click="$router.push(nav.path)">
            <div style="font-size:16px;">{{ nav.title }}</div>
            <div style="font-size:12px;color:#909399;margin-top:4px;">{{ nav.desc }}</div>
          </el-button>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { agentDefApi } from '../api/agentDef'
import { toolDefApi } from '../api/toolDef'
import { callLogApi } from '../api/callLog'
import { Cpu, Switch, Document, DataLine } from '@element-plus/icons-vue'

const cards = ref([
  { title: 'Agent 总数', value: 0, color: '#409EFF', icon: Cpu },
  { title: '可用工具数', value: 0, color: '#67C23A', icon: Switch },
  { title: '今日调用次数', value: 0, color: '#E6A23C', icon: DataLine },
  { title: '调用日志总数', value: 0, color: '#909399', icon: Document }
])

const navs = [
  { title: '🤖 Agent 管理', desc: '创建、编辑、测试你的智能代理', path: '/agents' },
  { title: '🔧 工具管理', desc: '查看和管理所有已注册的工具', path: '/tools' },
  { title: '📋 调用日志', desc: '查看 Agent 的调用记录和效果', path: '/logs' }
]

onMounted(async () => {
  try {
    const [agents, tools, logs] = await Promise.all([
      agentDefApi.list({ page: 1, size: 1 }),
      toolDefApi.allEnabled(),
      callLogApi.list({ page: 1, size: 1 })
    ])
    cards.value[0].value = agents?.total || 0
    cards.value[1].value = tools?.length || 0
    cards.value[2].value = '—'
    cards.value[3].value = logs?.total || 0
  } catch (e) { /* ignore */ }
})
</script>
