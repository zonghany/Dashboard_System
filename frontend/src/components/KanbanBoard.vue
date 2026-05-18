<template>
  <div class="kanban-board">
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else class="board-scroll">
      <div class="board-columns">
        <div
          v-for="column in columns"
          :key="column.id"
          class="board-column"
        >
          <div class="column-header">
            <div class="column-header-left">
              <div
                v-if="editingColumnId !== column.id"
                class="column-name"
                @dblclick="startEditColumnName(column)"
              >
                {{ column.name }}
              </div>
              <input
                v-else
                v-model="editingColumnName"
                class="column-name-input"
                @blur="finishEditColumnName(column)"
                @keyup.enter="finishEditColumnName(column)"
                @keyup.escape="cancelEditColumnName"
                ref="columnNameInputRef"
              />
              <span class="task-count-badge">{{ column.tasks.length }}</span>
            </div>
            <button class="column-delete-btn" @click="handleDeleteColumn(column.id)" title="删除列">×</button>
          </div>

          <div class="column-tasks">
            <draggable
              v-model="column.tasks"
              group="tasks"
              item-key="id"
              ghost-class="ghost"
              :animation="200"
              @change="handleDragChange(column, $event)"
              class="task-list"
            >
              <template #item="{ element }">
                <TaskCard
                  :task="element"
                  @click="emit('select-task', element)"
                />
              </template>
            </draggable>
          </div>

          <button class="add-task-btn" @click="handleAddTask(column.id)">
            + 添加任务
          </button>
        </div>

        <div class="add-column-area">
          <button class="add-column-btn" @click="showAddColumnModal = true">
            + 添加列
          </button>
        </div>
      </div>
    </div>

    <div v-if="showAddColumnModal" class="modal-overlay" @click.self="showAddColumnModal = false">
      <div class="modal-content">
        <h3 class="modal-title">添加新列</h3>
        <div class="form-group">
          <label>列名称</label>
          <input
            v-model="newColumnName"
            type="text"
            placeholder="输入列名称"
            @keyup.enter="handleAddColumn"
          />
        </div>
        <div class="modal-actions">
          <button class="btn-secondary" @click="showAddColumnModal = false">取消</button>
          <button class="btn-primary" @click="handleAddColumn" :disabled="!newColumnName.trim()">确认</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { VueDraggableNext as draggable } from 'vuedraggable'
import websocket from '../services/websocket'
import { boardApi, taskApi } from '../services/apiModules'
import TaskCard from './TaskCard.vue'

const props = defineProps({
  projectId: { type: Number, required: true }
})

const emit = defineEmits(['select-task'])

const columns = ref([])
const loading = ref(true)
const showAddColumnModal = ref(false)
const newColumnName = ref('')
const editingColumnId = ref(null)
const editingColumnName = ref('')

async function fetchColumns() {
  loading.value = true
  try {
    const res = await boardApi.columns(props.projectId)
    const cols = res.data
    for (const col of cols) {
      try {
        const taskRes = await taskApi.list(col.id)
        col.tasks = taskRes.data
      } catch {
        col.tasks = []
      }
    }
    columns.value = cols
  } catch (e) {
    console.error('获取列数据失败:', e)
    columns.value = []
  } finally {
    loading.value = false
  }
}

function connectWebSocket() {
  websocket.connect(props.projectId, handleWsMessage)
}

function handleWsMessage(data) {
  switch (data.type) {
    case 'COLUMN_CREATED': {
      const newCol = data.payload
      newCol.tasks = []
      columns.value.push(newCol)
      break
    }
    case 'COLUMN_UPDATED': {
      const col = columns.value.find(c => c.id === data.payload.id)
      if (col) {
        col.name = data.payload.name
      }
      break
    }
    case 'COLUMN_DELETED': {
      columns.value = columns.value.filter(c => c.id !== data.payload.id)
      break
    }
    case 'TASK_CREATED': {
      const col = columns.value.find(c => c.id === data.payload.columnId)
      if (col) {
        col.tasks.push(data.payload)
      }
      break
    }
    case 'TASK_UPDATED': {
      for (const col of columns.value) {
        const idx = col.tasks.findIndex(t => t.id === data.payload.id)
        if (idx !== -1) {
          col.tasks[idx] = { ...col.tasks[idx], ...data.payload }
          break
        }
      }
      break
    }
    case 'TASK_MOVED': {
      const { taskId, fromColumnId, toColumnId, newIndex } = data.payload
      let task = null
      for (const col of columns.value) {
        const idx = col.tasks.findIndex(t => t.id === taskId)
        if (idx !== -1) {
          task = col.tasks.splice(idx, 1)[0]
          break
        }
      }
      if (task) {
        const targetCol = columns.value.find(c => c.id === toColumnId)
        if (targetCol) {
          if (newIndex !== undefined && newIndex < targetCol.tasks.length) {
            targetCol.tasks.splice(newIndex, 0, task)
          } else {
            targetCol.tasks.push(task)
          }
        }
      }
      break
    }
    case 'TASK_DELETED': {
      for (const col of columns.value) {
        col.tasks = col.tasks.filter(t => t.id !== data.payload.id)
      }
      break
    }
  }
}

function handleDragChange(column, event) {
  if (event.added) {
    const task = event.added.element
    const newIndex = event.added.newIndex
    taskApi.move(task.id, { columnId: column.id, position: newIndex }).catch(e => {
      console.error('移动任务失败:', e)
    })
  }
}

async function handleAddTask(columnId) {
  try {
    const res = await taskApi.create(columnId)
    const col = columns.value.find(c => c.id === columnId)
    if (col) {
      col.tasks.push(res.data)
    }
  } catch (e) {
    console.error('创建任务失败:', e)
  }
}

async function handleAddColumn() {
  const name = newColumnName.value.trim()
  if (!name) return

  try {
    const res = await boardApi.createColumn(props.projectId, { name })
    const newCol = res.data
    newCol.tasks = []
    columns.value.push(newCol)
    newColumnName.value = ''
    showAddColumnModal.value = false
  } catch (e) {
    console.error('创建列失败:', e)
  }
}

async function handleDeleteColumn(columnId) {
  if (!confirm('确认删除此列？列中所有任务也将被删除。')) return

  try {
    await boardApi.deleteColumn(props.projectId, columnId)
    columns.value = columns.value.filter(c => c.id !== columnId)
  } catch (e) {
    console.error('删除列失败:', e)
  }
}

function startEditColumnName(column) {
  editingColumnId.value = column.id
  editingColumnName.value = column.name
}

async function finishEditColumnName(column) {
  const newName = editingColumnName.value.trim()
  editingColumnId.value = null

  if (newName && newName !== column.name) {
    try {
      await boardApi.renameColumn(props.projectId, column.id, { name: newName })
      column.name = newName
    } catch (e) {
      console.error('重命名列失败:', e)
    }
  }
}

function cancelEditColumnName() {
  editingColumnId.value = null
}

onMounted(() => {
  fetchColumns()
  connectWebSocket()
})

onBeforeUnmount(() => {
  websocket.disconnect()
})
</script>

<style scoped>
.kanban-board {
  height: 100%;
  overflow: hidden;
}

.board-scroll {
  height: 100%;
  overflow-x: auto;
  overflow-y: hidden;
}

.board-columns {
  display: flex;
  gap: 12px;
  padding: 12px;
  height: 100%;
  min-width: max-content;
}

.board-column {
  min-width: 280px;
  max-width: 320px;
  width: 280px;
  background: var(--gray-100);
  border-radius: var(--radius);
  padding: 12px;
  display: flex;
  flex-direction: column;
  max-height: 100%;
}

.column-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  flex-shrink: 0;
}

.column-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  flex: 1;
}

.column-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--gray-700);
  cursor: default;
  padding: 2px 4px;
  border-radius: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.column-name:hover {
  background: var(--gray-200);
}

.column-name-input {
  width: 100%;
  font-size: 14px;
  font-weight: 600;
  padding: 2px 4px;
  border: 1px solid var(--primary);
  border-radius: 4px;
  background: white;
}

.task-count-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  font-size: 11px;
  font-weight: 600;
  background: var(--gray-300);
  color: var(--gray-600);
  border-radius: 10px;
  flex-shrink: 0;
}

.column-delete-btn {
  background: none;
  border: none;
  font-size: 18px;
  color: var(--gray-400);
  cursor: pointer;
  padding: 0 4px;
  line-height: 1;
  border-radius: 4px;
}

.column-delete-btn:hover {
  color: var(--danger);
  background: rgba(239, 68, 68, 0.1);
}

.column-tasks {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
}

.task-list {
  min-height: 40px;
}

.add-task-btn {
  width: 100%;
  background: none;
  border: 1px dashed var(--gray-300);
  color: var(--gray-500);
  padding: 8px;
  border-radius: var(--radius);
  font-size: 13px;
  cursor: pointer;
  margin-top: 8px;
  flex-shrink: 0;
}

.add-task-btn:hover {
  border-color: var(--primary);
  color: var(--primary);
  background: rgba(79, 70, 229, 0.05);
}

.add-column-area {
  min-width: 280px;
  width: 280px;
  display: flex;
  align-items: flex-start;
  padding-top: 12px;
}

.add-column-btn {
  width: 100%;
  background: white;
  border: 2px dashed var(--gray-300);
  color: var(--gray-500);
  padding: 12px;
  border-radius: var(--radius);
  font-size: 14px;
  cursor: pointer;
}

.add-column-btn:hover {
  border-color: var(--primary);
  color: var(--primary);
}

.ghost {
  opacity: 0.5;
  background: var(--gray-200);
  border-radius: var(--radius);
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.modal-content {
  background: white;
  border-radius: var(--radius);
  padding: 24px;
  width: 360px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.15);
}

.modal-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--gray-800);
  margin-bottom: 16px;
}

.modal-content .form-group {
  margin-bottom: 16px;
}

.modal-content .form-group input {
  width: 100%;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
}

.modal-actions button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>