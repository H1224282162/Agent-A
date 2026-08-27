<template>
  <el-container style="height:100%;">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '220px'" style="background:#304156;transition:width 0.3s;overflow:hidden;">
      <div class="logo">
        <span v-if="!isCollapse">🤖 Agent-A</span>
        <span v-else>🤖</span>
      </div>
      <el-menu
        :default-active="route.path"
        :collapse="isCollapse"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
        router
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataBoard /></el-icon>
          <span>首页概览</span>
        </el-menu-item>
        <el-menu-item index="/agents">
          <el-icon><Cpu /></el-icon>
          <span>Agent 管理</span>
        </el-menu-item>
        <el-menu-item index="/tools">
          <el-icon><Switch /></el-icon>
          <span>工具管理</span>
        </el-menu-item>
        <el-menu-item index="/knowledge">
          <el-icon><Collection /></el-icon>
          <span>知识库</span>
        </el-menu-item>
        <el-menu-item index="/logs">
          <el-icon><Document /></el-icon>
          <span>调用日志</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- 主体 -->
    <el-container>
      <el-header style="display:flex;align-items:center;justify-content:space-between;border-bottom:1px solid #e6e6e6;">
        <el-button text @click="isCollapse = !isCollapse">
          <el-icon :size="20"><Fold v-if="!isCollapse" /><Expand v-else /></el-icon>
        </el-button>
        <span style="color:#909399;">Agent-A 智能代理管理平台 v1.0</span>
      </el-header>
      <el-main style="background:#f5f7fa;">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import { DataBoard, Cpu, Switch, Document, Collection, Fold, Expand } from '@element-plus/icons-vue'

const route = useRoute()
const isCollapse = ref(false)
</script>

<style scoped>
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  border-bottom: 1px solid #4a5b6e;
  white-space: nowrap;
}
.el-menu { border-right: none; }
.el-header { background: #fff; }
</style>
