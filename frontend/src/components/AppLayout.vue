<template>
  <div class="app-layout">
    <nav class="navbar">
      <div class="navbar-left">
        <router-link :to="{ name: 'ProjectList' }" class="navbar-brand">
          AI 看板
        </router-link>
      </div>
      <div class="navbar-center" v-if="projectName">
        <span class="project-name">{{ projectName }}</span>
      </div>
      <div class="navbar-right">
        <NotificationBell />
        <div class="user-dropdown" @click="toggleDropdown">
          <span class="username">{{ username }}</span>
          <span class="dropdown-arrow">▾</span>
          <div v-if="showDropdown" class="dropdown-menu">
            <button class="dropdown-item" @click.stop="handleLogout">退出登录</button>
          </div>
        </div>
      </div>
    </nav>
    <main class="main-content">
      <slot />
    </main>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import NotificationBell from './NotificationCenter.vue'

const props = defineProps({
  projectName: {
    type: String,
    default: ''
  }
})

const router = useRouter()
const showDropdown = ref(false)

const username = computed(() => {
  try {
    const user = JSON.parse(localStorage.getItem('user'))
    return user ? user.username || user.email || '用户' : '用户'
  } catch {
    return '用户'
  }
})

function toggleDropdown() {
  showDropdown.value = !showDropdown.value
}

function handleLogout() {
  localStorage.clear()
  showDropdown.value = false
  router.push({ name: 'Login' })
}
</script>

<style scoped>
.app-layout {
  min-height: 100vh;
}

.navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 56px;
  background: var(--primary);
  color: white;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  z-index: 100;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.navbar-brand {
  font-weight: 700;
  font-size: 18px;
  color: white;
  text-decoration: none;
}

.navbar-brand:hover {
  opacity: 0.9;
}

.navbar-center {
  flex: 1;
  text-align: center;
}

.project-name {
  font-size: 16px;
  font-weight: 500;
}

.navbar-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-dropdown {
  position: relative;
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: var(--radius);
  transition: background 0.2s;
}

.user-dropdown:hover {
  background: rgba(255, 255, 255, 0.15);
}

.username {
  font-size: 14px;
}

.dropdown-arrow {
  font-size: 12px;
}

.dropdown-menu {
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: 8px;
  background: white;
  border-radius: var(--radius);
  box-shadow: var(--shadow-md);
  min-width: 140px;
  overflow: hidden;
}

.dropdown-item {
  width: 100%;
  text-align: left;
  padding: 10px 16px;
  background: none;
  border: none;
  color: var(--gray-700);
  font-size: 14px;
  border-radius: 0;
}

.dropdown-item:hover {
  background: var(--gray-100);
  color: var(--danger);
}

.main-content {
  padding-top: 56px;
  min-height: calc(100vh - 56px);
}
</style>