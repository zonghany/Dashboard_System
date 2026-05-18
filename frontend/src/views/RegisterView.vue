<template>
  <div class="register-page">
    <div class="register-card card">
      <h1 class="app-title">AI 智能任务协同看板</h1>
      <p class="app-subtitle">注册</p>
      <div class="form-group">
        <label>用户名</label>
        <input
          v-model="form.username"
          type="text"
          placeholder="用户名"
        />
      </div>
      <div class="form-group">
        <label>邮箱</label>
        <input
          v-model="form.email"
          type="email"
          placeholder="邮箱地址"
        />
      </div>
      <div class="form-group">
        <label>密码</label>
        <input
          v-model="form.password"
          type="password"
          placeholder="密码（至少8位，包含大小写字母和数字）"
        />
      </div>
      <div class="form-group">
        <label>确认密码</label>
        <input
          v-model="form.confirmPassword"
          type="password"
          placeholder="确认密码"
        />
      </div>
      <p v-if="errorMsg" class="error-text">{{ errorMsg }}</p>
      <button
        class="btn-primary register-btn"
        :disabled="loading"
        @click="handleRegister"
      >
        {{ loading ? '注册中...' : '注册' }}
      </button>
      <p class="switch-link">
        已有账号？<router-link :to="{ name: 'Login' }">立即登录</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { authApi } from '../services/apiModules'

const router = useRouter()

const form = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: ''
})

const errorMsg = ref('')
const loading = ref(false)

function validateEmail(email) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)
}

function validatePassword(password) {
  if (password.length < 8) return false
  if (!/[A-Z]/.test(password)) return false
  if (!/[a-z]/.test(password)) return false
  if (!/[0-9]/.test(password)) return false
  return true
}

async function handleRegister() {
  errorMsg.value = ''

  if (!form.username.trim()) {
    errorMsg.value = '请输入用户名'
    return
  }
  if (!form.email.trim()) {
    errorMsg.value = '请输入邮箱地址'
    return
  }
  if (!validateEmail(form.email.trim())) {
    errorMsg.value = '请输入有效的邮箱地址'
    return
  }
  if (!form.password) {
    errorMsg.value = '请输入密码'
    return
  }
  if (!validatePassword(form.password)) {
    errorMsg.value = '密码至少8位，需包含大写字母、小写字母和数字'
    return
  }
  if (form.password !== form.confirmPassword) {
    errorMsg.value = '两次输入的密码不一致'
    return
  }

  loading.value = true
  try {
    await authApi.register({
      username: form.username.trim(),
      email: form.email.trim(),
      password: form.password
    })
    alert('注册成功，请登录')
    router.push({ name: 'Login' })
  } catch (err) {
    if (err.response && err.response.data && (err.response.data.error || err.response.data.message)) {
      errorMsg.value = err.response.data.error || err.response.data.message
    } else {
      errorMsg.value = '注册失败，请稍后重试'
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: var(--gray-50);
  padding: 24px;
}

.register-card {
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

.register-btn {
  width: 100%;
  padding: 12px;
  font-size: 16px;
  margin-top: 8px;
}

.register-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.switch-link {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: var(--gray-500);
}

.switch-link a {
  color: var(--primary);
  text-decoration: none;
}

.switch-link a:hover {
  text-decoration: underline;
}
</style>