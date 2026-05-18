<template>
  <div class="invitation-page">
    <div class="invitation-card card">
      <h1 class="app-title">AI 智能任务协同看板</h1>
      <p class="app-subtitle">项目邀请</p>

      <div v-if="loading" class="status-message">
        <p class="loading-text">正在处理邀请...</p>
      </div>

      <div v-else-if="errorMsg" class="status-message">
        <p class="error-text center">{{ errorMsg }}</p>
        <button class="btn-secondary" @click="handleGoProjectList">返回项目列表</button>
      </div>

      <div v-else-if="successMsg" class="status-message">
        <p class="success-text center">{{ successMsg }}</p>
        <button class="btn-primary" @click="handleGoProject">进入项目</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { invitationApi } from '../services/apiModules'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const errorMsg = ref('')
const successMsg = ref('')
const projectId = ref('')

onMounted(async () => {
  const token = route.params.token
  if (!token) {
    errorMsg.value = '无效的邀请链接'
    loading.value = false
    return
  }

  try {
    const res = await invitationApi.accept(token)
    projectId.value = res.data.projectId || res.data.id
    successMsg.value = '成功加入项目！'
  } catch (err) {
    if (err.response && err.response.data && err.response.data.message) {
      errorMsg.value = err.response.data.message
    } else {
      errorMsg.value = '邀请链接无效或已过期'
    }
  } finally {
    loading.value = false
  }
})

function handleGoProject() {
  if (projectId.value) {
    router.push({ name: 'ProjectDetail', params: { id: projectId.value } })
  }
}

function handleGoProjectList() {
  router.push({ name: 'ProjectList' })
}
</script>

<style scoped>
.invitation-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: var(--gray-50);
  padding: 24px;
}

.invitation-card {
  width: 100%;
  max-width: 400px;
  padding: 40px 32px;
}

.app-title {
  text-align: center;
  font-size: 24px;
  color: var(--primary);
  margin-bottom: 4px;
}

.app-subtitle {
  text-align: center;
  font-size: 14px;
  color: var(--gray-500);
  margin-bottom: 32px;
}

.status-message {
  text-align: center;
}

.loading-text {
  color: var(--gray-500);
  font-size: 16px;
  padding: 20px 0;
}

.center {
  text-align: center;
}

.success-text {
  color: var(--success);
  font-size: 16px;
  margin-bottom: 20px;
}

.error-text.center {
  margin-bottom: 20px;
}

button {
  margin-top: 12px;
  min-width: 160px;
}
</style>