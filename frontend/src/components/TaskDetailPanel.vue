<template>
  <div class="task-detail-overlay" v-if="visible" @click.self="handleClose">
    <div class="task-detail-panel" :class="{ 'panel-visible': visible }">
      <div class="panel-header">
        <h2 class="panel-title">任务详情</h2>
        <button class="panel-close-btn" @click="handleClose">×</button>
      </div>

      <div class="panel-body">
        <div class="field-group">
          <label class="field-label">任务标题</label>
          <input
            v-model="editingTask.title"
            type="text"
            class="field-input title-input"
            placeholder="输入任务标题"
            @blur="saveField('title')"
            @keyup.enter="($event.target).blur()"
          />
        </div>

        <div class="field-group">
          <label class="field-label">描述</label>
          <textarea
            v-model="editingTask.description"
            class="field-textarea"
            placeholder="输入任务描述..."
            rows="4"
            @input="debouncedSaveDescription"
          ></textarea>
          <div v-if="aiLoading" class="ai-loading-indicator">
            <span class="ai-spinner"></span>
            AI 正在分析任务...
          </div>
        </div>

        <div v-if="suggestions && suggestions.length > 0" class="ai-suggestions">
          <h4 class="suggestions-title">AI 建议</h4>
          <div v-for="(s, idx) in suggestions" :key="idx" class="suggestion-item">
            <p class="suggestion-text">{{ s.text || s }}</p>
            <button v-if="s.action" class="btn-secondary btn-xs" @click="applySuggestion(s)">应用</button>
          </div>
        </div>

        <div class="field-group">
          <label class="field-label">优先级</label>
          <select
            v-model="editingTask.priority"
            class="field-select"
            @change="saveField('priority')"
          >
            <option value="低">低</option>
            <option value="中">中</option>
            <option value="高">高</option>
            <option value="紧急">紧急</option>
          </select>
        </div>

        <div class="field-group">
          <label class="field-label">截止日期</label>
          <input
            v-model="editingTask.dueDate"
            type="datetime-local"
            class="field-input"
            @change="saveField('dueDate')"
          />
        </div>

        <div class="field-group">
          <label class="field-label">负责人</label>
          <select
            v-model="editingTask.assigneeId"
            class="field-select"
            @change="saveField('assigneeId')"
          >
            <option :value="null">未分配</option>
            <option v-for="m in members" :key="m.id" :value="m.id || m.userId">
              {{ m.name || m.email }}
            </option>
          </select>
        </div>

        <div class="field-group">
          <label class="field-label">预估工时（小时）</label>
          <input
            v-model="editingTask.estimatedHours"
            type="number"
            class="field-input"
            min="0"
            step="0.5"
            placeholder="0"
            @blur="saveField('estimatedHours')"
          />
        </div>

        <div class="field-group">
          <label class="field-label">标签</label>
          <div v-if="editingTask.tags && editingTask.tags.length > 0" class="tags-display">
            <span v-for="tag in editingTask.tags" :key="tag" class="tag-item">
              {{ tag }}
              <button class="tag-remove-btn" @click="removeTag(tag)">×</button>
            </span>
          </div>
          <div v-else class="field-hint">暂无标签</div>
        </div>

        <div class="field-group">
          <label class="field-label">附件</label>
          <div
            class="drop-zone"
            @dragover.prevent="dragOver = true"
            @dragleave.prevent="dragOver = false"
            @drop.prevent="handleDrop"
            :class="{ 'drag-active': dragOver }"
          >
            <div class="drop-zone-content">
              <span class="drop-icon">📎</span>
              <span class="drop-text">拖拽文件到此处或点击上传</span>
            </div>
            <input
              type="file"
              class="file-input-hidden"
              @change="handleUpload"
              ref="fileInputRef"
            />
          </div>
          <div v-if="uploadError" class="upload-error">{{ uploadError }}</div>
          <div class="attachment-list">
            <div v-for="att in attachments" :key="att.id" class="attachment-item">
              <span class="attachment-icon">📄</span>
              <span class="attachment-name">{{ att.fileName || att.name }}</span>
              <span class="attachment-size">{{ formatFileSize(att.fileSize || att.size) }}</span>
              <button class="attachment-delete-btn" @click="handleDeleteAttachment(att.id)">×</button>
            </div>
            <div v-if="attachments.length === 0 && !uploading" class="field-hint">暂无附件</div>
          </div>
          <div v-if="uploading" class="upload-progress">文件上传中...</div>
        </div>

        <div class="panel-danger-zone">
          <button class="btn-danger btn-block" @click="handleDelete">
            删除任务
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, watch, onBeforeUnmount } from 'vue'
import { taskApi, projectApi, aiApi } from '../services/apiModules'

const props = defineProps({
  task: { type: Object, required: true },
  visible: { type: Boolean, required: true },
  projectId: { type: Number, required: true }
})

const emit = defineEmits(['close', 'update', 'delete'])

const editingTask = reactive({})
const members = ref([])
const suggestions = ref(null)
const attachments = ref([])
const aiLoading = ref(false)
const uploading = ref(false)
const uploadError = ref('')
const dragOver = ref(false)
const fileInputRef = ref(null)

let saveTimer = null

function initFromTask() {
  if (props.task) {
    Object.assign(editingTask, {
      id: props.task.id,
      title: props.task.title || '',
      description: props.task.description || '',
      priority: props.task.priority || '低',
      dueDate: props.task.dueDate ? props.task.dueDate.substring(0, 16) : '',
      assigneeId: props.task.assigneeId || props.task.assignee?.id || null,
      estimatedHours: props.task.estimatedHours || null,
      tags: props.task.tags ? [...props.task.tags] : []
    })
  }
}

watch(() => props.task, (newTask) => {
  if (newTask) {
    initFromTask()
    fetchMembers()
    fetchAttachments()
    fetchAiSuggestions()
  }
}, { immediate: true, deep: true })

async function fetchMembers() {
  try {
    const res = await projectApi.members(props.projectId)
    members.value = res.data
  } catch (e) {
    console.error('获取成员列表失败:', e)
  }
}

async function fetchAttachments() {
  if (!props.task?.id) return
  try {
    const res = await taskApi.attachments(props.task.id)
    attachments.value = res.data || []
  } catch (e) {
    console.error('获取附件列表失败:', e)
    attachments.value = []
  }
}

async function fetchAiSuggestions() {
  const desc = editingTask.description
  if (!desc || desc.length < 10) {
    suggestions.value = null
    return
  }

  if (saveTimer) clearTimeout(saveTimer)
  saveTimer = setTimeout(async () => {
    aiLoading.value = true
    try {
      const res = await aiApi.suggest(desc)
      suggestions.value = res.data?.suggestions || res.data || []
    } catch (e) {
      console.error('获取AI建议失败:', e)
    } finally {
      aiLoading.value = false
    }
  }, 3000)
}

async function saveField(fieldName) {
  if (!editingTask.id) return
  try {
    const payload = { [fieldName]: editingTask[fieldName] }
    const res = await taskApi.update(editingTask.id, payload)
    emit('update', res.data)
  } catch (e) {
    console.error(`保存字段 ${fieldName} 失败:`, e)
  }
}

function debouncedSaveDescription() {
  if (saveTimer) clearTimeout(saveTimer)
  saveTimer = setTimeout(() => {
    saveField('description')
  }, 2000)

  if (saveTimer) clearTimeout(saveTimer)
  saveTimer = setTimeout(() => {
    fetchAiSuggestions()
  }, 3000)
}

function applySuggestion(suggestion) {
  if (suggestion.action === 'set_priority' && suggestion.value) {
    editingTask.priority = suggestion.value
    saveField('priority')
  } else if (suggestion.action === 'set_due_date' && suggestion.value) {
    editingTask.dueDate = suggestion.value
    saveField('dueDate')
  } else if (suggestion.action === 'set_estimated_hours' && suggestion.value) {
    editingTask.estimatedHours = suggestion.value
    saveField('estimatedHours')
  }
}

function removeTag(tag) {
  editingTask.tags = editingTask.tags.filter(t => t !== tag)
  saveField('tags')
}

async function handleUpload(e) {
  const file = e.target?.files?.[0]
  if (!file) return

  const maxSize = 10 * 1024 * 1024
  if (file.size > maxSize) {
    uploadError.value = '文件大小不能超过 10MB'
    return
  }

  uploading.value = true
  uploadError.value = ''

  try {
    const res = await taskApi.uploadAttachment(editingTask.id, file)
    attachments.value.push(res.data)
    if (fileInputRef.value) {
      fileInputRef.value.value = ''
    }
  } catch (err) {
    uploadError.value = err.response?.data?.message || '文件上传失败'
  } finally {
    uploading.value = false
  }
}

async function handleDrop(e) {
  dragOver.value = false
  const file = e.dataTransfer?.files?.[0]
  if (!file) return

  const maxSize = 10 * 1024 * 1024
  if (file.size > maxSize) {
    uploadError.value = '文件大小不能超过 10MB'
    return
  }

  uploading.value = true
  uploadError.value = ''

  try {
    const res = await taskApi.uploadAttachment(editingTask.id, file)
    attachments.value.push(res.data)
  } catch (err) {
    uploadError.value = err.response?.data?.message || '文件上传失败'
  } finally {
    uploading.value = false
  }
}

async function handleDeleteAttachment(attachmentId) {
  if (!confirm('确认删除此附件？')) return

  try {
    await taskApi.deleteAttachment(editingTask.id, attachmentId)
    attachments.value = attachments.value.filter(a => a.id !== attachmentId)
  } catch (e) {
    console.error('删除附件失败:', e)
  }
}

function handleDelete() {
  if (!confirm('确认删除此任务？此操作不可撤销。')) return
  emit('delete', editingTask.id)
}

function handleClose() {
  emit('close')
}

function formatFileSize(bytes) {
  if (!bytes || bytes === 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  let size = bytes
  while (size >= 1024 && i < units.length - 1) {
    size /= 1024
    i++
  }
  return size.toFixed(1) + ' ' + units[i]
}

onBeforeUnmount(() => {
  if (saveTimer) clearTimeout(saveTimer)
})
</script>

<style scoped>
.task-detail-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.3);
  z-index: 200;
  display: flex;
  justify-content: flex-end;
}

.task-detail-panel {
  width: 420px;
  height: 100vh;
  background: white;
  box-shadow: -4px 0 20px rgba(0, 0, 0, 0.1);
  transform: translateX(100%);
  transition: transform 0.3s ease;
  display: flex;
  flex-direction: column;
}

.task-detail-panel.panel-visible {
  transform: translateX(0);
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--gray-200);
  flex-shrink: 0;
}

.panel-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--gray-800);
}

.panel-close-btn {
  background: none;
  border: none;
  font-size: 24px;
  color: var(--gray-400);
  cursor: pointer;
  padding: 0 4px;
  line-height: 1;
  border-radius: 4px;
}

.panel-close-btn:hover {
  color: var(--gray-600);
  background: var(--gray-100);
}

.panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.field-group {
  margin-bottom: 20px;
}

.field-label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: var(--gray-500);
  margin-bottom: 6px;
}

.field-input {
  width: 100%;
  font-size: 14px;
}

.title-input {
  font-size: 16px;
  font-weight: 500;
}

.field-textarea {
  width: 100%;
  border: 1px solid var(--gray-300);
  border-radius: var(--radius);
  padding: 8px 12px;
  font-size: 14px;
  outline: none;
  resize: vertical;
  font-family: inherit;
  transition: border-color 0.2s;
}

.field-textarea:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 2px rgba(79, 70, 229, 0.1);
}

.field-select {
  width: 100%;
  font-size: 14px;
}

.ai-loading-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--primary);
  margin-top: 6px;
}

.ai-spinner {
  display: inline-block;
  width: 12px;
  height: 12px;
  border: 2px solid var(--primary-light);
  border-top-color: var(--primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.ai-suggestions {
  background: #f5f3ff;
  border: 1px solid #ddd6fe;
  border-radius: var(--radius);
  padding: 12px;
  margin-bottom: 20px;
}

.suggestions-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--primary);
  margin-bottom: 8px;
}

.suggestion-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 0;
  border-bottom: 1px solid #ede9fe;
}

.suggestion-item:last-child {
  border-bottom: none;
}

.suggestion-text {
  font-size: 13px;
  color: var(--gray-700);
}

.btn-xs {
  font-size: 11px;
  padding: 3px 8px;
}

.tags-display {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.tag-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  padding: 2px 8px;
  background: var(--gray-100);
  color: var(--gray-700);
  border-radius: 12px;
}

.tag-remove-btn {
  background: none;
  border: none;
  font-size: 14px;
  color: var(--gray-400);
  cursor: pointer;
  padding: 0 2px;
  line-height: 1;
}

.tag-remove-btn:hover {
  color: var(--danger);
}

.field-hint {
  font-size: 13px;
  color: var(--gray-400);
  padding: 6px 0;
}

.drop-zone {
  border: 2px dashed var(--gray-300);
  border-radius: var(--radius);
  padding: 20px;
  text-align: center;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
  position: relative;
}

.drop-zone:hover {
  border-color: var(--primary-light);
  background: rgba(79, 70, 229, 0.03);
}

.drop-zone.drag-active {
  border-color: var(--primary);
  background: rgba(79, 70, 229, 0.06);
}

.drop-zone-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.drop-icon {
  font-size: 24px;
}

.drop-text {
  font-size: 13px;
  color: var(--gray-500);
}

.file-input-hidden {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  cursor: pointer;
}

.upload-error {
  color: var(--danger);
  font-size: 12px;
  margin-top: 6px;
}

.upload-progress {
  font-size: 13px;
  color: var(--primary);
  margin-top: 6px;
}

.attachment-list {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.attachment-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  background: var(--gray-50);
  border-radius: var(--radius);
}

.attachment-icon {
  font-size: 16px;
  flex-shrink: 0;
}

.attachment-name {
  flex: 1;
  font-size: 13px;
  color: var(--gray-700);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.attachment-size {
  font-size: 11px;
  color: var(--gray-400);
  flex-shrink: 0;
}

.attachment-delete-btn {
  background: none;
  border: none;
  font-size: 16px;
  color: var(--gray-400);
  cursor: pointer;
  padding: 0 4px;
  line-height: 1;
  flex-shrink: 0;
}

.attachment-delete-btn:hover {
  color: var(--danger);
}

.panel-danger-zone {
  margin-top: 32px;
  padding-top: 16px;
  border-top: 1px solid var(--gray-200);
}

.btn-block {
  width: 100%;
}
</style>