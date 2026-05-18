<template>
  <div v-if="suggestions" class="ai-suggestions">
    <div class="suggestions-title">🤖 AI 建议</div>

    <div class="suggestions-section">
      <div class="section-label">建议标签:</div>
      <div class="tags-wrap">
        <div
          v-for="(tag, idx) in suggestions.tags"
          :key="idx"
          class="tag-item"
          :class="{
            'tag-accepted': tagStatus[idx] === 'accepted',
            'tag-rejected': tagStatus[idx] === 'rejected'
          }"
        >
          <span class="tag-text">{{ tag }}</span>
          <div v-if="tagStatus[idx] === 'accepted'" class="tag-done">
            <span class="checkmark">✓</span>
          </div>
          <div v-else-if="tagStatus[idx] === 'rejected'" class="tag-ignored-text">
            已拒绝
          </div>
          <div v-else class="tag-actions">
            <button class="btn-accept" @click="acceptTag(tag, idx)">采纳</button>
            <button class="btn-reject" @click="rejectTag(idx)">拒绝</button>
          </div>
        </div>
      </div>
    </div>

    <div class="suggestions-section">
      <div class="section-label">预估工时: {{ suggestions.estimatedHours }} 小时</div>
      <div class="hour-actions">
        <template v-if="hourAccepted === false">
          <button class="btn-accept" @click="acceptHours">采纳</button>
          <button class="btn-reject" @click="rejectHours">拒绝</button>
        </template>
        <span v-else-if="hourAccepted === true" class="adopted-text">已采纳</span>
        <span v-else class="rejected-text">已拒绝</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  suggestions: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['accept-tag', 'accept-hours'])

const tagStatus = ref({})
const hourAccepted = ref(false)

watch(
  () => props.suggestions,
  () => {
    tagStatus.value = {}
    hourAccepted.value = false
  }
)

function acceptTag(tag, idx) {
  tagStatus.value[idx] = 'accepted'
  emit('accept-tag', tag)
}

function rejectTag(idx) {
  tagStatus.value[idx] = 'rejected'
}

function acceptHours() {
  hourAccepted.value = true
  emit('accept-hours', props.suggestions.estimatedHours)
}

function rejectHours() {
  hourAccepted.value = 'rejected'
}
</script>

<style scoped>
.ai-suggestions {
  background: #f0f4ff;
  border-radius: var(--radius);
  padding: 12px;
  margin-top: 8px;
}

.suggestions-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--gray-700);
  margin-bottom: 10px;
}

.suggestions-section {
  margin-bottom: 8px;
}

.suggestions-section:last-child {
  margin-bottom: 0;
}

.section-label {
  font-size: 12px;
  color: var(--gray-600);
  margin-bottom: 6px;
  font-weight: 500;
}

.tags-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.tag-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  background: white;
  border-radius: 20px;
  border: 1px solid var(--gray-200);
  font-size: 12px;
}

.tag-text {
  color: var(--gray-700);
}

.tag-accepted {
  background: #ecfdf5;
  border-color: var(--success);
}

.tag-rejected {
  background: var(--gray-100);
  border-color: var(--gray-300);
  opacity: 0.6;
}

.tag-done {
  color: var(--success);
  font-weight: 700;
  font-size: 13px;
}

.tag-ignored-text {
  font-size: 11px;
  color: var(--gray-400);
}

.tag-actions {
  display: flex;
  gap: 4px;
}

.btn-accept {
  padding: 2px 8px;
  font-size: 11px;
  border-radius: 10px;
  background: var(--success);
  color: white;
  border: none;
  cursor: pointer;
  transition: opacity 0.15s;
}

.btn-accept:hover {
  opacity: 0.8;
}

.btn-reject {
  padding: 2px 8px;
  font-size: 11px;
  border-radius: 10px;
  background: var(--gray-200);
  color: var(--gray-500);
  border: none;
  cursor: pointer;
  transition: opacity 0.15s;
}

.btn-reject:hover {
  opacity: 0.8;
}

.hour-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.adopted-text {
  font-size: 12px;
  color: var(--success);
  font-weight: 500;
}

.rejected-text {
  font-size: 12px;
  color: var(--gray-400);
}

.checkmark {
  display: inline-block;
}
</style>