<template>
  <AppLayout :projectName="project.name">
    <div class="project-detail">
      <div class="sidebar">
        <div class="sidebar-section">
          <div
            v-if="!editingProjectName"
            class="project-name-display"
            @dblclick="isAdmin && startEditProjectName()"
            :class="{ editable: isAdmin }"
          >
            {{ project.name || '未命名项目' }}
          </div>
          <div v-else class="project-name-edit">
            <input
              v-model="editingNameInput"
              class="name-input"
              @blur="saveProjectName"
              @keyup.enter="saveProjectName"
              @keyup.escape="cancelEditProjectName"
              ref="nameInputRef"
            />
          </div>
          <p class="project-description">{{ project.description || '暂无描述' }}</p>
        </div>

        <div class="sidebar-section">
          <h3 class="section-title">成员管理</h3>
          <div class="member-list">
            <div
              v-for="member in members"
              :key="member.id"
              class="member-item"
            >
              <div class="member-info">
                <div class="member-avatar">{{ member.name ? member.name[0] : '?' }}</div>
                <div class="member-details">
                  <span class="member-name">{{ member.name || member.email }}</span>
                  <span class="member-role-badge" :class="'role-' + getRoleClass(member.role)">
                    {{ member.role }}
                  </span>
                </div>
              </div>
              <div v-if="isAdmin && member.role !== '管理员'" class="member-actions">
                <select
                  class="role-select"
                  :value="member.role"
                  @change="handleChangeRole(member.id, ($event.target).value)"
                >
                  <option value="成员">成员</option>
                  <option value="观察者">观察者</option>
                  <option value="管理员">管理员</option>
                </select>
                <button class="btn-danger btn-sm" @click="handleRemoveMember(member.id)">移除</button>
              </div>
            </div>
            <div v-if="members.length === 0" class="empty-hint">暂无成员</div>
          </div>
        </div>

        <div v-if="isAdmin" class="sidebar-section">
          <h3 class="section-title">邀请成员</h3>
          <div class="invite-form">
            <input
              v-model="inviteEmail"
              type="email"
              class="invite-input"
              placeholder="输入邮箱地址"
              @keyup.enter="handleInvite"
            />
            <button class="btn-primary btn-sm" @click="handleInvite" :disabled="!inviteEmail.trim() || inviting">
              {{ inviting ? '发送中...' : '发送邀请' }}
            </button>
          </div>
          <div v-if="inviteResult" class="invite-result" :class="inviteResult.success ? 'success' : 'error'">
            {{ inviteResult.message }}
          </div>
        </div>

        <div class="sidebar-section">
          <h3 class="section-title">AI 助手</h3>
          <AiCommandPanel :projectId="projectId" @execute="onAiCommandExecuted" />
        </div>
      </div>

      <div class="main-area">
        <KanbanBoard
          :projectId="projectId"
          @select-task="handleSelectTask"
        />
        <TaskDetailPanel
          :task="selectedTask"
          :visible="taskPanelVisible"
          :projectId="projectId"
          @close="handleCloseTaskPanel"
          @update="handleTaskUpdated"
          @delete="handleTaskDeleted"
        />
      </div>
    </div>
  </AppLayout>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { projectApi } from '../services/apiModules'
import KanbanBoard from '../components/KanbanBoard.vue'
import AppLayout from '../components/AppLayout.vue'
import AiCommandPanel from '../components/AiPanel.vue'
import TaskDetailPanel from '../components/TaskDetailPanel.vue'

const route = useRoute()
const projectId = computed(() => Number(route.params.id))

const project = ref({})
const members = ref([])
const currentUserRole = ref('')
const inviteEmail = ref('')
const inviting = ref(false)
const inviteResult = ref(null)
const editingProjectName = ref(false)
const editingNameInput = ref('')

const selectedTask = ref(null)
const taskPanelVisible = ref(false)

const isAdmin = computed(() => currentUserRole.value === '管理员')

async function fetchProject() {
  try {
    const res = await projectApi.get(projectId.value)
    project.value = res.data
  } catch (e) {
    console.error('获取项目信息失败:', e)
  }
}

async function fetchMembers() {
  try {
    const res = await projectApi.members(projectId.value)
    members.value = res.data
    const token = localStorage.getItem('token')
    if (token) {
      try {
        const payload = JSON.parse(atob(token.split('.')[1]))
        const currentMember = res.data.find(m => m.userId === payload.userId || m.id === payload.userId)
        if (currentMember) {
          currentUserRole.value = currentMember.role
        }
      } catch {}
    }
  } catch (e) {
    console.error('获取成员列表失败:', e)
  }
}

function startEditProjectName() {
  editingNameInput.value = project.value.name
  editingProjectName.value = true
}

async function saveProjectName() {
  const newName = editingNameInput.value.trim()
  if (newName && newName !== project.value.name) {
    try {
      await projectApi.update(projectId.value, { name: newName })
      project.value.name = newName
    } catch (e) {
      console.error('更新项目名称失败:', e)
    }
  }
  editingProjectName.value = false
}

function cancelEditProjectName() {
  editingProjectName.value = false
}

async function handleInvite() {
  const email = inviteEmail.value.trim()
  if (!email) return

  inviting.value = true
  inviteResult.value = null

  try {
    await projectApi.invite(projectId.value, { email })
    inviteResult.value = { success: true, message: `已向 ${email} 发送邀请` }
    inviteEmail.value = ''
  } catch (e) {
    inviteResult.value = {
      success: false,
      message: e.response?.data?.message || '邀请发送失败'
    }
  } finally {
    inviting.value = false
  }
}

async function handleChangeRole(memberId, role) {
  try {
    await projectApi.changeRole(projectId.value, memberId, role)
    const member = members.value.find(m => m.id === memberId)
    if (member) member.role = role
  } catch (e) {
    console.error('修改角色失败:', e)
  }
}

async function handleRemoveMember(memberId) {
  const member = members.value.find(m => m.id === memberId)
  if (!confirm(`确认将 ${member?.name || member?.email || '该成员'} 移出项目？`)) return

  try {
    await projectApi.removeMember(projectId.value, memberId)
    members.value = members.value.filter(m => m.id !== memberId)
  } catch (e) {
    console.error('移除成员失败:', e)
  }
}

function handleSelectTask(task) {
  selectedTask.value = task
  taskPanelVisible.value = true
}

function handleCloseTaskPanel() {
  taskPanelVisible.value = false
  selectedTask.value = null
}

function handleTaskUpdated(updatedTask) {
  selectedTask.value = updatedTask
}

function handleTaskDeleted(taskId) {
  taskPanelVisible.value = false
  selectedTask.value = null
}

function onAiCommandExecuted() {
  console.log('AI 指令已执行')
}

function getRoleClass(role) {
  if (role === '管理员') return 'admin'
  if (role === '观察者') return 'observer'
  return 'member'
}

onMounted(() => {
  fetchProject()
  fetchMembers()
})
</script>

<style scoped>
.project-detail {
  display: flex;
  height: 100%;
}

.sidebar {
  width: 260px;
  min-width: 260px;
  border-right: 1px solid var(--gray-200);
  padding: 16px;
  overflow-y: auto;
  background: white;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.sidebar-section {
  margin-bottom: 20px;
}

.section-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--gray-500);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 10px;
}

.project-name-display {
  font-size: 20px;
  font-weight: 700;
  color: var(--gray-800);
  padding: 4px 0;
  border-radius: 4px;
}

.project-name-display.editable {
  cursor: pointer;
}

.project-name-display.editable:hover {
  background: var(--gray-100);
}

.project-name-edit .name-input {
  width: 100%;
  font-size: 20px;
  font-weight: 700;
  padding: 4px 8px;
}

.project-description {
  font-size: 13px;
  color: var(--gray-500);
  margin-top: 6px;
  line-height: 1.5;
}

.member-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.member-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 8px;
  border-radius: var(--radius);
  background: var(--gray-50);
}

.member-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.member-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--primary-light);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  flex-shrink: 0;
}

.member-details {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.member-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--gray-800);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.member-role-badge {
  display: inline-block;
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 10px;
  font-weight: 500;
}

.role-admin {
  background: #fef3c7;
  color: #92400e;
}

.role-member {
  background: #dbeafe;
  color: #1e40af;
}

.role-observer {
  background: var(--gray-200);
  color: var(--gray-600);
}

.member-actions {
  display: flex;
  gap: 6px;
}

.role-select {
  flex: 1;
  font-size: 12px;
  padding: 4px 6px;
}

.btn-sm {
  font-size: 12px;
  padding: 4px 10px;
}

.invite-form {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.invite-input {
  width: 100%;
  font-size: 13px;
}

.invite-result {
  margin-top: 8px;
  font-size: 12px;
  padding: 6px 10px;
  border-radius: var(--radius);
}

.invite-result.success {
  background: #ecfdf5;
  color: #065f46;
}

.invite-result.error {
  background: #fef2f2;
  color: #991b1b;
}

.empty-hint {
  font-size: 13px;
  color: var(--gray-400);
  text-align: center;
  padding: 12px 0;
}

.main-area {
  flex: 1;
  overflow: hidden;
  position: relative;
}
</style>