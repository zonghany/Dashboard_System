<template>
  <div class="task-card" :class="'priority-' + task.priority" @click="$emit('click')">
    <div class="task-card-body">
      <div class="task-title" :class="{ 'empty-title': !task.title }">
        {{ task.title || '新任务' }}
      </div>
      <div v-if="task.tags && task.tags.length > 0" class="task-tags">
        <span v-for="tag in task.tags" :key="tag" class="task-tag">{{ tag }}</span>
      </div>
      <div class="task-card-footer">
        <div class="task-priority">
          <span class="priority-dot" :class="'dot-' + task.priority"></span>
          <span class="priority-text">{{ getPriorityText(task.priority) }}</span>
        </div>
        <div class="task-meta-right">
          <span v-if="task.dueDate" class="task-due" :class="{ overdue: isOverdue(task.dueDate) }">
            📅 {{ formatDate(task.dueDate) }}
          </span>
          <span v-if="task.attachmentCount > 0" class="task-attachments">
            📎 {{ task.attachmentCount }}
          </span>
          <span v-if="task.assignee" class="task-assignee">
            {{ task.assignee.name ? task.assignee.name[0] : '?' }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  task: { type: Object, required: true }
})

defineEmits(['click'])

function getPriorityText(priority) {
  const map = { '低': '低', '中': '中', '高': '高', '紧急': '紧急' }
  return map[priority] || '低'
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${month}-${day}`
}

function isOverdue(dateStr) {
  if (!dateStr) return false
  return new Date(dateStr) < new Date()
}
</script>

<style scoped>
.task-card {
  background: white;
  border-radius: 6px;
  margin-bottom: 8px;
  cursor: pointer;
  box-shadow: var(--shadow);
  border-left: 4px solid transparent;
  transition: box-shadow 0.2s;
}

.task-card:hover {
  box-shadow: var(--shadow-md);
}

.task-card.priority-低 {
  border-left-color: var(--gray-300);
}

.task-card.priority-中 {
  border-left-color: #3b82f6;
}

.task-card.priority-高 {
  border-left-color: var(--warning);
}

.task-card.priority-紧急 {
  border-left-color: var(--danger);
}

.task-card-body {
  padding: 8px 12px;
}

.task-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--gray-800);
  line-height: 1.4;
  word-break: break-word;
}

.task-title.empty-title {
  color: var(--gray-400);
  font-style: italic;
}

.task-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 6px;
}

.task-tag {
  display: inline-block;
  font-size: 11px;
  padding: 1px 6px;
  background: var(--gray-100);
  color: var(--gray-600);
  border-radius: 8px;
}

.task-card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}

.task-priority {
  display: flex;
  align-items: center;
  gap: 4px;
}

.priority-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.dot-低 {
  background: var(--gray-400);
}

.dot-中 {
  background: #3b82f6;
}

.dot-高 {
  background: var(--warning);
}

.dot-紧急 {
  background: var(--danger);
}

.priority-text {
  font-size: 11px;
  color: var(--gray-500);
}

.task-meta-right {
  display: flex;
  align-items: center;
  gap: 6px;
}

.task-due {
  font-size: 11px;
  color: var(--gray-500);
}

.task-due.overdue {
  color: var(--danger);
  font-weight: 500;
}

.task-attachments {
  font-size: 11px;
  color: var(--gray-500);
}

.task-assignee {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: var(--primary-light);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 600;
  flex-shrink: 0;
}
</style>