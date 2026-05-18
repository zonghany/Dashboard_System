<template>
  <div class="login-page">
    <div class="login-card card">
      <h1 class="app-title">AI 智能任务协同看板</h1>
      <p class="app-subtitle">登录</p>
      <div class="form-group">
        <label>邮箱</label>
        <input
          v-model="form.email"
          type="email"
          placeholder="邮箱地址"
          @keyup.enter="handleLogin"
        />
      </div>
      <div class="form-group">
        <label>密码</label>
        <input
          v-model="form.password"
          type="password"
          placeholder="密码"
          @keyup.enter="handleLogin"
        />
      </div>
      <p v-if="errorMsg" class="error-text">{{ errorMsg }}</p>
      <button
        class="btn-primary login-btn"
        :disabled="loading"
        @click="handleLogin"
      >
        {{ loading ? '登录中...' : '登录' }}
      </button>
      <p class="switch-link">
        还没有账号？<router-link :to="{ name: 'Register' }">立即注册</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { authApi } from '../services/apiModules'

const router = useRouter()
const route = useRoute()

const form = reactive({
  email: '',
  password: ''
})

const errorMsg = ref('')
const loading = ref(false)

async function handleLogin() {
  errorMsg.value = ''

  if (!form.email.trim()) {
    errorMsg.value = '请输入邮箱地址'
    return
  }
  if (!form.password) {
    errorMsg.value = '请输入密码'
    return
  }

  loading.value = true
  try {
    const res = await authApi.login({
      email: form.email.trim(),
      password: form.password
    })
    localStorage.setItem('token', res.data.token)
    localStorage.setItem('user', JSON.stringify(res.data))
    router.push(route.query.redirect || { name: 'ProjectList' })
  } catch (err) {
    if (err.response && err.response.data && (err.response.data.error || err.response.data.message)) {
      errorMsg.value = err.response.data.error || err.response.data.message
    } else {
      errorMsg.value = '邮箱或密码错误'
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: var(--gray-50);
  padding: 24px;
}

.login-card {
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

.login-btn {
  width: 100%;
  padding: 12px;
  font-size: 16px;
  margin-top: 8px;
}

.login-btn:disabled {
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