<template>
  <div class="notification-center" ref="containerRef">
    <button class="bell-btn" @click="toggleDropdown">
      <svg class="bell-icon" viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
        <path d="M13.73 21a2 2 0 0 1-3.46 0" />
      </svg>
      <span v-if="unreadCount > 0" class="badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
    </button>

    <div v-if="showDropdown" class="dropdown-panel">
      <div class="panel-header">
        <span class="panel-title">通知</span>
        <button class="mark-all-btn" @click="markAllRead">全部已读</button>
      </div>

      <div v-if="notifications.length === 0" class="empty-state">暂无通知</div>

      <div
        v-for="notification in notifications"
        :key="notification.id"
        class="notification-item"
        :class="{ unread: !notification.isRead }"
        @click="handleClickNotification(notification)"
      >
        <div class="notification-text">{{ notification.message }}</div>
        <div class="notification-time">{{ formatTime(notification.createdAt) }}</div>
      </div>

      <div v-if="loading" class="loading-state">加载中...</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { notificationApi } from '../services/apiModules'

const emit = defineEmits(['navigate-task'])

const notifications = ref([])
const unreadCount = ref(0)
const showDropdown = ref(false)
const loading = ref(false)
const containerRef = ref(null)

async function fetchNotifications() {
  loading.value = true
  try {
    const res = await notificationApi.list()
    notifications.value = res.data
  } catch {
    notifications.value = []
  } finally {
    loading.value = false
  }
}

async function fetchUnreadCount() {
  try {
    const res = await notificationApi.unreadCount()
    unreadCount.value = res.data.count ?? res.data ?? 0
  } catch {
    unreadCount.value = 0
  }
}

function toggleDropdown() {
  showDropdown.value = !showDropdown.value
  if (showDropdown.value) {
    fetchNotifications()
  }
}

async function handleClickNotification(notif) {
  if (!notif.isRead) {
    try {
      await notificationApi.markRead(notif.id)
      notif.isRead = true
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    } catch {
      // silently fail
    }
  }
  if (notif.relatedTaskId) {
    emit('navigate-task', notif.relatedTaskId)
  }
  showDropdown.value = false
}

async function markAllRead() {
  const unread = notifications.value.filter(n => !n.isRead)
  for (const notif of unread) {
    try {
      await notificationApi.markRead(notif.id)
      notif.isRead = true
    } catch {
      // silently fail
    }
  }
  fetchUnreadCount()
}

function handleClickOutside(e) {
  if (containerRef.value && !containerRef.value.contains(e.target)) {
    showDropdown.value = false
  }
}

function formatTime(dateStr) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now - date
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + ' 分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + ' 小时前'
  if (diff < 604800000) return Math.floor(diff / 86400000) + ' 天前'
  return date.toLocaleDateString('zh-CN')
}

onMounted(() => {
  fetchUnreadCount()
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.notification-center {
  position: relative;
}

.bell-btn {
  position: relative;
  background: transparent;
  border: none;
  padding: 4px;
  cursor: pointer;
  color: var(--gray-600);
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  width: 36px;
  height: 36px;
}

.bell-btn:hover {
  background: var(--gray-100);
  color: var(--gray-800);
}

.bell-icon {
  display: block;
}

.badge {
  position: absolute;
  top: -4px;
  right: -4px;
  background: var(--danger);
  color: white;
  border-radius: 50%;
  min-width: 18px;
  height: 18px;
  font-size: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
  line-height: 1;
}

.dropdown-panel {
  position: absolute;
  right: 0;
  top: 100%;
  width: 360px;
  max-height: 480px;
  overflow-y: auto;
  background: white;
  box-shadow: var(--shadow-md);
  border-radius: var(--radius);
  z-index: 1000;
  margin-top: 4px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--gray-200);
  position: sticky;
  top: 0;
  background: white;
  z-index: 1;
}

.panel-title {
  font-weight: 600;
  font-size: 15px;
  color: var(--gray-800);
}

.mark-all-btn {
  background: none;
  border: none;
  color: var(--primary);
  font-size: 13px;
  cursor: pointer;
  padding: 2px 4px;
}

.mark-all-btn:hover {
  color: var(--primary-light);
}

.empty-state {
  padding: 32px 16px;
  text-align: center;
  color: var(--gray-400);
  font-size: 14px;
}

.notification-item {
  padding: 12px 16px;
  border-bottom: 1px solid var(--gray-100);
  cursor: pointer;
  transition: background 0.15s;
}

.notification-item:hover {
  background: var(--gray-50);
}

.notification-item.unread {
  background: var(--gray-50);
}

.notification-item.unread:hover {
  background: var(--gray-100);
}

.notification-text {
  font-size: 14px;
  color: var(--gray-700);
  line-height: 1.5;
}

.notification-time {
  font-size: 12px;
  color: var(--gray-400);
  margin-top: 4px;
}

.loading-state {
  padding: 16px;
  text-align: center;
  color: var(--gray-400);
  font-size: 13px;
}
</style>