import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  { path: '/', redirect: '/dashboard' },
  { path: '/dashboard', name: 'Dashboard', component: () => import('../views/Dashboard.vue') },
  { path: '/agents', name: 'AgentList', component: () => import('../views/AgentList.vue') },
  { path: '/agents/create', name: 'AgentCreate', component: () => import('../views/AgentDetail.vue') },
  { path: '/agents/:id/edit', name: 'AgentEdit', component: () => import('../views/AgentDetail.vue') },
  { path: '/agents/:id/chat', name: 'AgentChat', component: () => import('../views/AgentChat.vue') },
  { path: '/tools', name: 'ToolList', component: () => import('../views/ToolList.vue') },
  { path: '/knowledge', name: 'KnowledgeList', component: () => import('../views/KnowledgeList.vue') },
  { path: '/knowledge/create', name: 'KnowledgeCreate', component: () => import('../views/KnowledgeDetail.vue') },
  { path: '/knowledge/:id/edit', name: 'KnowledgeEdit', component: () => import('../views/KnowledgeDetail.vue') },
  { path: '/knowledge/:id/documents', name: 'KnowledgeDocuments', component: () => import('../views/KnowledgeDocuments.vue') },
  { path: '/logs', name: 'CallLogList', component: () => import('../views/CallLogList.vue') }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

export default router
