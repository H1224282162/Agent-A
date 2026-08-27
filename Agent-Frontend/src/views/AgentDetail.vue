<template>
  <div>
    <h2 style="margin-bottom:20px;">{{ isEdit ? '✏️ 编辑 Agent' : '➕ 新建 Agent' }}</h2>

    <el-card shadow="never">
      <el-form ref="formRef" :model="form" label-width="120px" style="max-width:900px;">
        <el-form-item label="Agent 编码" required>
          <el-input v-model="form.agentCode" placeholder="唯一标识，如 order_helper" :disabled="isEdit" />
          <div style="color:#909399;font-size:12px;">编码唯一且不可重复，创建后不可修改</div>
        </el-form-item>

        <el-form-item label="显示名称" required>
          <el-input v-model="form.agentName" placeholder="如：蜜雪冰城点单助手" />
        </el-form-item>

        <el-form-item label="默认模型" required>
          <el-select v-model="form.modelType" style="width:200px;">
            <el-option label="DeepSeek" value="deepseek" />
            <el-option label="Kimi" value="kimi" />
          </el-select>
        </el-form-item>

        <el-form-item label="状态" required>
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="2">草稿</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="System Prompt" required>
          <el-input
            v-model="form.systemPrompt"
            type="textarea"
            :rows="16"
            placeholder="输入 System Prompt..."
          />
          <div style="color:#909399;font-size:12px;margin-top:4px;">
            提示词是 Agent 的"灵魂"。定义角色、行为规则、工具使用策略和对话风格。
          </div>
        </el-form-item>

        <!-- 工具绑定 -->
        <el-form-item label="绑定工具">
          <div style="width:100%;">
            <el-checkbox-group v-model="selectedToolIds">
              <el-checkbox
                v-for="tool in allTools"
                :key="tool.id"
                :value="tool.id"
                :label="tool.id"
                style="display:block;margin-bottom:4px;"
              >
                <el-tag size="small" :type="tool.category === '业务工具' ? 'success' : tool.category === '运维工具' ? 'warning' : ''">
                  {{ tool.category }}
                </el-tag>
                <span style="margin-left:6px;font-weight:500;">{{ tool.displayName || tool.toolName }}</span>
                <span style="margin-left:8px;color:#909399;font-size:12px;">{{ tool.toolName }}</span>
              </el-checkbox>
            </el-checkbox-group>
          </div>
          <div style="color:#909399;font-size:12px;">勾选后，Agent 即可调用对应工具</div>
        </el-form-item>

        <!-- 知识库绑定 -->
        <el-form-item label="绑定知识库">
          <div style="width:100%;">
            <el-checkbox-group v-model="selectedKbIds">
              <el-checkbox
                v-for="kb in allKnowledgeBases"
                :key="kb.id"
                :value="kb.id"
                :label="kb.id"
                style="display:block;margin-bottom:4px;"
              >
                <span style="font-weight:500;">{{ kb.kbName }}</span>
                <span style="margin-left:8px;color:#909399;font-size:12px;">{{ kb.kbCode }}</span>
              </el-checkbox>
            </el-checkbox-group>
            <div v-if="allKnowledgeBases.length === 0" style="color:#909399;font-size:12px;">暂无可用知识库，请先创建</div>
          </div>
          <div style="color:#909399;font-size:12px;">勾选后，Agent 对话时将引用知识库内容</div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
          <el-button @click="$router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { agentDefApi } from '../api/agentDef'
import { toolDefApi } from '../api/toolDef'
import { agentToolApi } from '../api/agentTool'
import { knowledgeBaseApi } from '../api/knowledgeBase'
import { agentKnowledgeApi } from '../api/agentKnowledge'

const route = useRoute()
const router = useRouter()
const isEdit = computed(() => !!route.params.id)

const form = ref({
  agentCode: '',
  agentName: '',
  modelType: 'deepseek',
  systemPrompt: '',
  status: 1,
  version: 1
})

const allTools = ref([])
const selectedToolIds = ref([])
const existingBindings = ref([])   // 已存在的工具绑定（编辑模式下）
const allKnowledgeBases = ref([])
const selectedKbIds = ref([])
const existingKbBindings = ref([]) // 已存在的知识库绑定（编辑模式下）
const saving = ref(false)

onMounted(async () => {
  // 加载所有工具
  const res = await toolDefApi.allEnabled()
  allTools.value = res || []

  // 加载所有启用知识库
  const kbRes = await knowledgeBaseApi.allEnabled()
  allKnowledgeBases.value = kbRes || []

  if (isEdit.value) {
    // 编辑模式：加载 Agent 详情 + 已绑定的工具
    const agent = await agentDefApi.getById(route.params.id)
    Object.assign(form.value, {
      id: agent.id,
      agentCode: agent.agentCode,
      agentName: agent.agentName,
      modelType: agent.modelType,
      systemPrompt: agent.systemPrompt,
      status: agent.status,
      version: agent.version
    })

    const bindings = await agentToolApi.list(agent.id)
    existingBindings.value = bindings || []
    selectedToolIds.value = existingBindings.value
      .filter(b => b.enabled === 1 || b.enabled === true)
      .map(b => b.toolId)

    // 加载已绑定的知识库
    const kbBindings = await agentKnowledgeApi.list(agent.id)
    existingKbBindings.value = kbBindings || []
    selectedKbIds.value = existingKbBindings.value
      .filter(b => b.enabled === 1 || b.enabled === true)
      .map(b => b.kbId)
  }
})

const handleSave = async () => {
  saving.value = true
  try {
    // 1. 保存 Agent 定义
    if (isEdit.value) {
      await agentDefApi.update(form.value)
    } else {
      form.value.version = 1
      // save 接口返回 Result<Long>，data 即新建 Agent 的 id
      form.value.id = await agentDefApi.save(form.value)
    }

    // 2. 同步工具绑定
    if (isEdit.value) {
      // 删除旧的绑定
      for (const b of existingBindings.value) {
        await agentToolApi.unbind(form.value.id, b.toolId)
      }
    }
    // 添加新绑定
    for (const toolId of selectedToolIds.value) {
      await agentToolApi.bind({ agentId: form.value.id, toolId, enabled: 1 })
    }

    // 3. 同步知识库绑定
    if (isEdit.value) {
      for (const b of existingKbBindings.value) {
        await agentKnowledgeApi.unbind(form.value.id, b.kbId)
      }
    }
    for (const kbId of selectedKbIds.value) {
      await agentKnowledgeApi.bind({ agentId: form.value.id, kbId, enabled: 1 })
    }

    ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
    router.push('/agents')
  } catch (e) {
    ElMessage.error('保存失败：' + (e.message || e))
  } finally {
    saving.value = false
  }
}
</script>
