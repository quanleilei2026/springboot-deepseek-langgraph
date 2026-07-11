<template>
  <el-card class="panel-card" shadow="hover">
    <template #header>
      <div class="card-header">
        <el-icon><el-icon-connection /></el-icon>
        <span>LangGraph 控制面板 (LangGraph Panel)</span>
      </div>
    </template>

    <div class="panel-content">
      <!-- Description -->
      <el-alert
        title="LANGGRAPH 功能特性 (LANGGRAPH Features)"
        type="info"
        :closable="false"
        show-icon
      >
        <template #default>
          <p>演示以下核心功能 (Demonstrates core features):</p>
          <ul>
            <li><strong>状态持久化 (State Persistence):</strong> 跨所有节点维护状态 - State maintained across all nodes</li>
            <li><strong>循环工作流 (Cyclic Workflows):</strong> 审查→编码循环进行修改 - Reviewer → Coder loops for revisions</li>
            <li><strong>多智能体协调 (Multi-agent Coordination):</strong> 研究员、编码员、审查员协作 - Researcher, Coder, Reviewer collaboration</li>
            <li><strong>检查点 (Checkpointing):</strong> 保存/恢复任意点工作流 - Save/resume workflow at any point</li>
          </ul>
        </template>
      </el-alert>

      <el-divider />

      <!-- Requirements Input -->
      <el-form :model="form" label-width="120px">
        <el-form-item label="需求描述 (Requirements)">
          <el-input
            v-model="form.requirements"
            type="textarea"
            :rows="4"
            placeholder="在此输入您的代码需求... (Enter your code requirements here...)"
          />
        </el-form-item>
      </el-form>

      <!-- LANGGRAPH Buttons -->
      <div class="button-grid">
        <!-- LANGGRAPH: Start Development Cycle -->
        <el-button
          type="primary"
          :loading="loading"
          :icon="StartDevelopment"
          @click="startDevelopment"
          size="large"
        >
          启动开发周期 (Start Development Cycle)
        </el-button>

        <!-- LANGGRAPH: View Development State -->
        <el-button
          type="success"
          :icon="ViewState"
          @click="viewState"
          :disabled="!currentThreadId"
          size="large"
        >
          查看开发状态 (View Development State)
        </el-button>

        <!-- LANGGRAPH: Pause & Resume Development -->
        <el-button
          type="warning"
          :icon="PauseResume"
          @click="pauseResume"
          :disabled="!currentThreadId"
          size="large"
        >
          暂停/恢复开发 (Pause & Resume Development)
        </el-button>

        <!-- LANGGRAPH: Request Revision -->
        <el-button
          type="info"
          :icon="RequestRevision"
          @click="requestRevision"
          :disabled="!currentThreadId || !currentState"
          size="large"
        >
          请求修改 (Request Revision)
        </el-button>

        <!-- LANGGRAPH: Reset Development -->
        <el-button
          type="danger"
          :icon="ResetDevelopment"
          @click="resetDevelopment"
          :disabled="!currentThreadId"
          size="large"
        >
          重置开发 (Reset Development)
        </el-button>

        <!-- LANGGRAPH: View Workflow Graph -->
        <el-button
          type="default"
          :icon="ViewGraph"
          @click="viewGraph"
          size="large"
        >
          查看工作流图 (View Workflow Graph)
        </el-button>
      </div>

      <!-- Current State Display -->
      <el-divider v-if="currentState" />
      <div v-if="currentState" class="state-display">
        <h3>Current Workflow State</h3>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="Thread ID">
            {{ currentState.threadId }}
          </el-descriptions-item>
          <el-descriptions-item label="Run ID">
            {{ currentState.runId }}
          </el-descriptions-item>
          <el-descriptions-item label="Current Node">
            <el-tag>{{ currentState.currentNode }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="Status">
            <el-tag :type="getStatusType(currentState.status)">
              {{ currentState.status }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="Iteration Count">
            {{ currentState.iterationCount }}
          </el-descriptions-item>
          <el-descriptions-item label="Revision Count">
            {{ currentState.revisionCount }}
          </el-descriptions-item>
        </el-descriptions>

        <!-- Research Findings -->
        <div v-if="currentState.researchFindings" class="state-section">
          <h4>Research Findings:</h4>
          <el-input
            :model-value="currentState.researchFindings"
            type="textarea"
            :rows="4"
            readonly
          />
        </div>

        <!-- Generated Code -->
        <div v-if="currentState.generatedCode" class="state-section">
          <h4>Generated Code:</h4>
          <el-input
            :model-value="currentState.generatedCode"
            type="textarea"
            :rows="6"
            readonly
          />
        </div>

        <!-- Review Comments -->
        <div v-if="currentState.reviewComments" class="state-section">
          <h4>Review Comments:</h4>
          <el-input
            :model-value="currentState.reviewComments"
            type="textarea"
            :rows="4"
            readonly
          />
        </div>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  VideoPlay as StartDevelopment,
  View as ViewState,
  Refresh as PauseResume,
  Edit as RequestRevision,
  Delete as ResetDevelopment,
  Share as ViewGraph
} from '@element-plus/icons-vue'
import * as api from '../api/langgraph.js'

// 表单数据 (Form data)
const form = reactive({
  requirements: 'Create a REST API endpoint that validates user input and returns a structured JSON response'
})

// 状态管理 (State management)
const loading = ref(false)
const currentThreadId = ref(null)
const currentState = ref(null)

// LANGGRAPH: 启动开发周期 (Start Development Cycle)
const startDevelopment = async () => {
  if (!form.requirements) {
    ElMessage.warning('请输入需求描述 (Please enter requirements)')
    return
  }

  loading.value = true
  try {
    // LANGGRAPH: 启动多智能体工作流
    // Initiates multi-agent workflow with state persistence
    const result = await api.startDevelopmentCycle(form.requirements)

    currentThreadId.value = result.threadId
    currentState.value = result

    ElMessage.success(`开发周期已启动！状态 (Development cycle started! Status): ${result.status}`)

    // 显示工作流信息
    ElMessageBox.alert(
      `工作流完成，包含 ${result.iterationCount} 次迭代和 ${result.revisionCount} 次修改。`,
      'LANGGRAPH: 工作流完成 (Workflow Complete)',
      {
        confirmButtonText: '确定 (OK)',
        type: 'success'
      }
    )
  } catch (error) {
    ElMessage.error('启动开发失败 (Failed to start development): ' + error.message)
  } finally {
    loading.value = false
  }
}

// LANGGRAPH: 查看开发状态 (View Development State)
const viewState = async () => {
  try {
    const state = await api.getDevelopmentState(currentThreadId.value)
    currentState.value = state
    ElMessage.success('状态检索成功 (State retrieved successfully)')
  } catch (error) {
    ElMessage.error('检索状态失败 (Failed to retrieve state): ' + error.message)
  }
}

// LANGGRAPH: 暂停和恢复开发 (Pause & Resume Development)
const pauseResume = async () => {
  try {
    ElMessageBox.prompt(
      '输入检查点名称以恢复 (Enter checkpoint name to resume from):',
      'LANGGRAPH: 从检查点恢复 (Resume from Checkpoint)',
      {
        confirmButtonText: '恢复 (Resume)',
        cancelButtonText: '取消 (Cancel)',
        inputPattern: /.+/,
        inputErrorMessage: '检查点名称是必需的 (Checkpoint name is required)'
      }
    ).then(async ({ value }) => {
      const result = await api.resumeDevelopment(
        currentThreadId.value,
        value,
        form.requirements
      )
      currentState.value = result
      ElMessage.success(`已从检查点恢复 (Resumed from checkpoint): ${value}`)
    }).catch(() => {
      ElMessage.info('恢复操作已取消 (Resume cancelled)')
    })
  } catch (error) {
    ElMessage.error('恢复失败 (Failed to resume): ' + error.message)
  }
}

// LANGGRAPH: 请求修改 (Request Revision)
const requestRevision = async () => {
  try {
    ElMessageBox.prompt(
      '输入修改意见 (Enter revision comments):',
      'LANGGRAPH: 请求修改 (Request Revision)',
      {
        confirmButtonText: '请求 (Request)',
        cancelButtonText: '取消 (Cancel)',
        inputPattern: /.+/,
        inputErrorMessage: '修改意见是必需的 (Revision comments are required)'
      }
    ).then(async ({ value }) => {
      const result = await api.requestRevision(currentThreadId.value, value)
      currentState.value = result
      ElMessage.success(`修改请求已提交。新修改计数 (Revision requested. New revision count): ${result.revisionCount}`)
    }).catch(() => {
      ElMessage.info('修改请求已取消 (Revision request cancelled)')
    })
  } catch (error) {
    ElMessage.error('请求修改失败 (Failed to request revision): ' + error.message)
  }
}

// LANGGRAPH: 重置开发 (Reset Development)
const resetDevelopment = async () => {
  try {
    await ElMessageBox.confirm(
      '这将清除所有工作流状态。继续？',
      'LANGGRAPH: 重置工作流 (Reset Workflow)',
      {
        confirmButtonText: '重置 (Reset)',
        cancelButtonText: '取消 (Cancel)',
        type: 'warning'
      }
    )

    await api.resetDevelopment(currentThreadId.value)
    currentThreadId.value = null
    currentState.value = null
    ElMessage.success('工作流重置成功 (Workflow reset successfully)')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('重置失败 (Failed to reset): ' + error.message)
    }
  }
}

// LANGGRAPH: 查看工作流图 (View Workflow Graph)
const viewGraph = async () => {
  try {
    const graph = await api.getWorkflowGraph()

    // 在消息框中显示图结构
    const nodeText = graph.nodes.map(n => `- ${n.name} (${n.type})`).join('\n')
    const edgeText = graph.edges.map(e => `${e.from} → ${e.to}${e.conditional ? ' (conditional)' : ''}`).join('\n')

    ElMessageBox.alert(
      `节点 (Nodes):\n${nodeText}\n\n边 (Edges):\n${edgeText}`,
      'LANGGRAPH: 工作流图结构 (Workflow Graph Structure)',
      {
        confirmButtonText: '关闭 (Close)',
        type: 'info'
      }
    )
  } catch (error) {
    ElMessage.error('检索图失败 (Failed to retrieve graph): ' + error.message)
  }
}

// 状态标签类型辅助函数 (Helper function for status tag type)
const getStatusType = (status) => {
  switch (status) {
    case 'COMPLETED': return 'success'
    case 'FAILED': return 'danger'
    case 'IN_PROGRESS': return 'warning'
    default: return 'info'
  }
}
</script>

<style scoped>
.panel-card {
  height: 100%;
  min-height: 600px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: bold;
}

.panel-content {
  padding: 10px;
}

.button-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
  margin-top: 10px;
}

.state-display {
  margin-top: 20px;
}

.state-section {
  margin-top: 15px;
}

.state-section h4 {
  margin-bottom: 8px;
  color: #333;
}
</style>
