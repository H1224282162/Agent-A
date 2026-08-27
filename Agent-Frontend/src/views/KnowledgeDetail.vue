<template>
  <div>
    <div style="display:flex;align-items:center;gap:12px;margin-bottom:16px;">
      <el-button text @click="$router.back()">← 返回</el-button>
      <h3>{{ isEdit ? '编辑知识库' : '新建知识库' }}</h3>
    </div>

    <el-card shadow="never">
      <el-form :model="form" label-width="140px" style="max-width:700px;">
        <el-form-item label="知识库编码" required>
          <el-input v-model="form.kbCode" :disabled="isEdit" placeholder="如 order_faq" />
        </el-form-item>

        <el-form-item label="知识库名称" required>
          <el-input v-model="form.kbName" placeholder="显示名称" />
        </el-form-item>

        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="知识库用途描述" />
        </el-form-item>

        <el-form-item label="Embedding 模型">
          <el-select v-model="form.embeddingModel" style="width:100%;">
            <el-option label="BGE-large-zh（本地）" value="BAAI/bge-large-zh" />
          </el-select>
        </el-form-item>

        <el-form-item label="分块策略">
          <el-radio-group v-model="form.chunkStrategy">
            <el-radio label="fixed">固定长度</el-radio>
            <el-radio label="sliding">滑动窗口</el-radio>
            <el-radio label="paragraph">按段落</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="分块大小">
          <el-input-number v-model="form.chunkSize" :min="100" :max="4096" :step="100" />
        </el-form-item>

        <el-form-item label="重叠长度" v-if="form.chunkStrategy === 'sliding'">
          <el-input-number v-model="form.chunkOverlap" :min="0" :max="2048" :step="10" />
        </el-form-item>

        <el-form-item label="默认召回数量">
          <el-input-number v-model="form.topK" :min="1" :max="20" />
        </el-form-item>

        <el-form-item label="相似度阈值">
          <el-slider v-model="form.similarityThreshold" :min="0" :max="1" :step="0.05" show-input />
        </el-form-item>

        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
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
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { knowledgeBaseApi } from '../api/knowledgeBase'

const route = useRoute()
const router = useRouter()
const isEdit = !!route.params.id

const form = ref({
  kbCode: '',
  kbName: '',
  description: '',
  embeddingModel: 'BAAI/bge-large-zh',
  chunkStrategy: 'fixed',
  chunkSize: 512,
  chunkOverlap: 50,
  topK: 5,
  similarityThreshold: 0.75,
  status: 1
})

const saving = ref(false)

const loadDetail = async () => {
  if (!isEdit) return
  try {
    const res = await knowledgeBaseApi.getById(route.params.id)
    form.value = { ...form.value, ...res }
  } catch (e) {
    ElMessage.error('加载详情失败')
  }
}

const handleSave = async () => {
  if (!form.value.kbCode || !form.value.kbName) {
    ElMessage.warning('请填写编码和名称')
    return
  }
  saving.value = true
  try {
    if (isEdit) {
      await knowledgeBaseApi.update(route.params.id, form.value)
    } else {
      await knowledgeBaseApi.save(form.value)
    }
    ElMessage.success('保存成功')
    router.push('/knowledge')
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(loadDetail)
</script>
