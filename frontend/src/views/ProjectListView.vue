<template>
  <AppLayout>
    <div class="project-list-page">
      <div class="page-header">
        <h2 class="page-title">我的项目</h2>
        <button class="btn-primary" @click="showCreateModal = true">新建项目</button>
      </div>

      <div v-if="loading" class="loading">加载中...</div>

      <div v-else-if="projects.length === 0" class="empty-state">
        <p>还没有项目，点击上方按钮创建第一个项目</p>
      </div>

      <div v-else class="project-grid">
        <router-link
          v-for="project in projects"
          :key="project.id"
          :to="{ name: 'ProjectDetail', params: { id: project.id } }"
          class="project-card card"
        >
          <h3 class="project-card-name">{{ project.name }}</h3>
          <p class="project-card-desc">{{ project.description || '暂无描述' }}</p>
          <div class="project-card-meta">
            <span>{{ project.memberCount || 0 }} 名成员</span>
            <span>{{ formatDate(project.updatedAt || project.createdAt) }}</span>
          </div>
        </router-link>
      </div>

      <div v-if="totalPages > 1" class="pagination">
        <button
          class="btn-secondary"
          :disabled="currentPage === 0"
          @click="currentPage--; fetchProjects()"
        >
          上一页
        </button>
        <span class="page-info">{{ currentPage + 1 }} / {{ totalPages }}</span>
        <button
          class="btn-secondary"
          :disabled="currentPage >= totalPages - 1"
          @click="currentPage++; fetchProjects()"
        >
          下一页
        </button>
      </div>
    </div>

    <div v-if="showCreateModal" class="modal-overlay" @click.self="showCreateModal = false">
      <div class="modal-card card">
        <h3 class="modal-title">新建项目</h3>
        <div class="form-group">
          <label>项目名称</label>
          <input
            v-model="newProject.name"
            type="text"
            placeholder="请输入项目名称"
            @keyup.enter="handleCreate"
          />
        </div>
        <div class="form-group">
          <label>项目描述</label>
          <textarea
            v-model="newProject.description"
            placeholder="请输入项目描述（可选）"
            rows="3"
          ></textarea>
        </div>
        <p v-if="createError" class="error-text">{{ createError }}</p>
        <div class="modal-actions">
          <button class="btn-secondary" @click="showCreateModal = false">取消</button>
          <button class="btn-primary" :disabled="creating" @click="handleCreate">
            {{ creating ? '创建中...' : '创建' }}
          </button>
        </div>
      </div>
    </div>
  </AppLayout>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { projectApi } from '../services/apiModules'
import AppLayout from '../components/AppLayout.vue'

const projects = ref([])
const loading = ref(true)
const currentPage = ref(0)
const totalPages = ref(0)

async function fetchProjects() {
  loading.value = true
  try {
    const res = await projectApi.list(currentPage.value)
    if (res.data.content) {
      projects.value = res.data.content
      totalPages.value = res.data.totalPages || 1
    } else if (Array.isArray(res.data)) {
      projects.value = res.data
      totalPages.value = 1
    } else {
      projects.value = []
      totalPages.value = 0
    }
  } catch {
    projects.value = []
    totalPages.value = 0
  } finally {
    loading.value = false
  }
}

const showCreateModal = ref(false)
const creating = ref(false)
const createError = ref('')
const newProject = reactive({
  name: '',
  description: ''
})

async function handleCreate() {
  createError.value = ''

  if (!newProject.name.trim()) {
    createError.value = '请输入项目名称'
    return
  }

  creating.value = true
  try {
    await projectApi.create({
      name: newProject.name.trim(),
      description: newProject.description.trim()
    })
    showCreateModal.value = false
    newProject.name = ''
    newProject.description = ''
    await fetchProjects()
  } catch (err) {
    if (err.response && err.response.data && err.response.data.message) {
      createError.value = err.response.data.message
    } else {
      createError.value = '创建项目失败，请稍后重试'
    }
  } finally {
    creating.value = false
  }
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

onMounted(() => {
  fetchProjects()
})
</script>

<style scoped>
.project-list-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--gray-800);
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 300px;
  color: var(--gray-500);
  font-size: 16px;
}

.project-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.project-card {
  display: block;
  text-decoration: none;
  color: inherit;
  padding: 20px;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.project-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.project-card-name {
  font-size: 18px;
  font-weight: 600;
  color: var(--gray-800);
  margin-bottom: 8px;
}

.project-card-desc {
  font-size: 14px;
  color: var(--gray-500);
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.project-card-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--gray-400);
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 32px;
}

.page-info {
  font-size: 14px;
  color: var(--gray-600);
}

.pagination button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 200;
}

.modal-card {
  width: 100%;
  max-width: 480px;
  padding: 24px;
}

.modal-title {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 20px;
  color: var(--gray-800);
}

.modal-card textarea {
  width: 100%;
  resize: vertical;
  font-family: inherit;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 8px;
}

.loading {
  text-align: center;
  padding: 60px;
  color: var(--gray-500);
  font-size: 16px;
}
</style>