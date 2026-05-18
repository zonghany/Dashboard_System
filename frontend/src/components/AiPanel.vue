<template>
  <div class="ai-panel">
    <div class="panel-title">🤖 AI 指令</div>
    <div class="input-group">
      <input
        v-model="command"
        type="text"
        placeholder="输入自然语言指令，如：把所有逾期任务标记为紧急"
        class="command-input"
        @keyup.enter="handleExecute"
      />
      <button
        class="btn-primary execute-btn"
        :disabled="loading || !command.trim()"
        @click="handleExecute"
      >
        执行
      </button>
    </div>

    <div v-if="loading" class="loading-text">AI 正在分析...</div>

    <div v-if="result" class="result-message" :class="result.type === 'success' ? 'result-success' : 'result-error'">
      {{ result.message }}
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { aiApi } from '../services/apiModules'

const props = defineProps({
  projectId: {
    type: Number,
    required: true
  }
})

const command = ref('')
const result = ref(null)
const loading = ref(false)

async function handleExecute() {
  if (!command.value.trim()) return
  loading.value = true
  result.value = null

  try {
    const res = await aiApi.executeCommand(command.value, props.projectId)
    result.value = { type: 'success', message: res.data.message ?? res.data }
    command.value = ''
  } catch (err) {
    result.value = {
      type: 'error',
      message: err.response?.data?.error || 'AI 指令解析失败'
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.ai-panel {
  padding: 8px;
  border-left: 3px solid var(--primary);
  background: white;
  border-radius: var(--radius);
}

.panel-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--gray-700);
  margin-bottom: 8px;
}

.input-group {
  display: flex;
  gap: 6px;
}

.command-input {
  flex: 1;
  font-size: 13px;
}

.execute-btn {
  padding: 6px 14px;
  font-size: 13px;
  flex-shrink: 0;
}

.execute-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.loading-text {
  font-size: 12px;
  color: var(--gray-400);
  margin-top: 6px;
}

.result-message {
  margin-top: 8px;
  padding: 8px 12px;
  border-radius: var(--radius);
  font-size: 13px;
  line-height: 1.5;
}

.result-success {
  background: #ecfdf5;
  color: #065f46;
}

.result-error {
  background: #fef2f2;
  color: #991b1b;
}
</style>