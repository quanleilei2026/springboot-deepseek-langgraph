<template>
  <el-card class="panel-card" shadow="hover">
    <template #header>
      <div class="card-header">
        <el-icon><el-icon-data-analysis /></el-icon>
        <span>LangSmith 控制面板 (LangSmith Panel)</span>
      </div>
    </template>

    <div class="panel-content">
      <!-- Description -->
      <el-alert
        title="LANGSMITH 功能特性 (LANGSMITH Features)"
        type="info"
        :closable="false"
        show-icon
      >
        <template #default>
          <p>演示以下核心功能 (Demonstrates core features):</p>
          <ul>
            <li><strong>端到端追踪 (End-to-end Tracing):</strong> 追踪执行的每个步骤 - Every step of execution is traced</li>
            <li><strong>调试视图 (Debugging View):</strong> 检查每步输入/输出 - Inspect inputs/outputs at each step</li>
            <li><strong>评估框架 (Evaluation Framework):</strong> 测试不同提示/配置 - Test different prompts/configurations</li>
            <li><strong>性能指标 (Performance Metrics):</strong> Token使用、延迟跟踪 - Token usage, latency tracking</li>
          </ul>
        </template>
      </el-alert>

      <el-divider />

      <!-- LANGSMITH Buttons -->
      <div class="button-grid">
        <!-- LANGSMITH: View Development Traces -->
        <el-button
          type="primary"
          :icon="ViewTraces"
          @click="viewTraces"
          :loading="tracesLoading"
          size="large"
        >
          查看开发追踪 (View Development Traces)
        </el-button>

        <!-- LANGSMITH: Trace Detail View -->
        <el-button
          type="success"
          :icon="TraceDetail"
          @click="viewTraceDetail"
          :disabled="!selectedRunId"
          size="large"
        >
          追踪详情视图 (Trace Detail View)
        </el-button>

        <!-- LANGSMITH: Run Development Evaluation -->
        <el-button
          type="warning"
          :icon="RunEvaluation"
          @click="runEvaluation"
          :disabled="!selectedRunId"
          size="large"
        >
          运行开发评估 (Run Development Evaluation)
        </el-button>

        <!-- LANGSMITH: Performance Metrics -->
        <el-button
          type="info"
          :icon="PerformanceMetrics"
          @click="viewMetrics"
          :disabled="!selectedRunId"
          size="large"
        >
          性能指标 (Performance Metrics)
        </el-button>

        <!-- LANGSMITH: Compare Development Cycles -->
        <el-button
          type="default"
          :icon="CompareRuns"
          @click="compareRuns"
          :disabled="traces.length < 2"
          size="large"
        >
          比较开发周期 (Compare Development Cycles)
        </el-button>
      </div>

      <!-- Traces List -->
      <el-divider v-if="traces.length > 0" />
      <div v-if="traces.length > 0" class="traces-list">
        <h3>Recent Development Cycles</h3>
        <el-table
          :data="traces"
          style="width: 100%"
          @row-click="selectRun"
          :row-class-name="getRowClassName"
        >
          <el-table-column prop="id" label="Run ID" width="180" />
          <el-table-column prop="threadId" label="Thread ID" width="180" />
          <el-table-column prop="runType" label="Type" width="150" />
          <el-table-column prop="status" label="Status" width="120">
            <template #default="scope">
              <el-tag :type="getStatusType(scope.row.status)">
                {{ scope.row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="startedAt" label="Started" width="120">
            <template #default="scope">
              {{ formatTime(scope.row.startedAt) }}
            </template>
          </el-table-column>
          <el-table-column prop="iterationCount" label="Iterations" width="100" />
          <el-table-column prop="revisionCount" label="Revisions" width="100" />
        </el-table>
      </div>

      <!-- Evaluation Results -->
      <el-divider v-if="evaluationResults.length > 0" />
      <div v-if="evaluationResults.length > 0" class="evaluation-results">
        <h3>Evaluation Results</h3>
        <el-table :data="evaluationResults" style="width: 100%">
          <el-table-column prop="evaluationType" label="Type" width="150" />
          <el-table-column prop="score" label="Score" width="100">
            <template #default="scope">
              <el-tag :type="getScoreType(scope.row.score)">
                {{ scope.row.score.toFixed(1) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="passed" label="Passed" width="100">
            <template #default="scope">
              <el-tag :type="scope.row.passed ? 'success' : 'danger'">
                {{ scope.row.passed ? 'Yes' : 'No' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="feedback" label="Feedback" />
        </el-table>
      </div>

      <!-- Performance Metrics Display -->
      <el-divider v-if="performanceMetrics" />
      <div v-if="performanceMetrics" class="metrics-display">
        <h3>Performance Metrics</h3>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="Total Tokens">
            {{ performanceMetrics.totalTokens?.toLocaleString() }}
          </el-descriptions-item>
          <el-descriptions-item label="Total Latency">
            {{ (performanceMetrics.totalLatencyMs / 1000).toFixed(2) }}s
          </el-descriptions-item>
          <el-descriptions-item label="Total Cost">
            ${{ performanceMetrics.totalCost?.toFixed(4) }}
          </el-descriptions-item>
          <el-descriptions-item label="Iteration Count">
            {{ performanceMetrics.iterationCount }}
          </el-descriptions-item>
          <el-descriptions-item label="Revision Count">
            {{ performanceMetrics.revisionCount }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  List as ViewTraces,
  ZoomIn as TraceDetail,
  Document as RunEvaluation,
  DataLine as PerformanceMetrics,
  TrendCharts as CompareRuns
} from '@element-plus/icons-vue'
import * as api from '../api/langgraph.js'

// 状态管理 (State management)
const tracesLoading = ref(false)
const traces = ref([])
const selectedRunId = ref(null)
const evaluationResults = ref([])
const performanceMetrics = ref(null)

// LANGSMITH: 查看开发追踪 (View Development Traces)
const viewTraces = async () => {
  tracesLoading.value = true
  try {
    // LANGSMITH: 检索所有追踪的运行记录
    // Retrieves all traced runs from backend
    const result = await api.getTraces()
    traces.value = result || []
    ElMessage.success(`检索到 ${traces.value.length} 条追踪记录 (Retrieved ${traces.value.length} traces)`)
  } catch (error) {
    ElMessage.error('检索追踪记录失败 (Failed to retrieve traces): ' + error.message)
  } finally {
    tracesLoading.value = false
  }
}

// LANGSMITH: 追踪详情视图 (Trace Detail View)
const viewTraceDetail = async () => {
  try {
    // LANGSMITH: 检索包含所有事件的详细追踪信息
    // Retrieves detailed trace with all events and timings
    const detail = await api.getTraceDetail(selectedRunId.value)

    const runText = `
运行ID (Run ID): ${detail.run.id}
线程ID (Thread ID): ${detail.run.threadId}
类型 (Type): ${detail.run.runType}
状态 (Status): ${detail.run.status}
开始时间 (Started): ${formatTime(detail.run.startedAt)}
迭代次数 (Iterations): ${detail.run.iterationCount}
修改次数 (Revisions): ${detail.run.revisionCount}
总Token数 (Total Tokens): ${detail.run.totalTokens || 0}
持续时间 (Duration): ${detail.run.totalDuration ? (detail.run.totalDuration / 1000).toFixed(2) + 's' : 'N/A'}
    `.trim()

    const eventsText = detail.events.map((e, i) => `
${i + 1}. ${e.nodeName} (${e.traceType})
   状态 (Status): ${e.status}
   Tokens: ${e.tokenUsage || 0}
   延迟 (Latency): ${e.latencyMs ? (e.latencyMs / 1000).toFixed(2) + 's' : 'N/A'}
    `).join('')

    ElMessageBox.alert(
      `${runText}\n\n追踪事件 (Trace Events):\n${eventsText}`,
      'LANGSMITH: 追踪详情视图 (Trace Detail View)',
      {
        confirmButtonText: '关闭 (Close)',
        type: 'info'
      }
    )
  } catch (error) {
    ElMessage.error('检索追踪详情失败 (Failed to retrieve trace detail): ' + error.message)
  }
}

// LANGSMITH: 运行开发评估 (Run Development Evaluation)
const runEvaluation = async () => {
  try {
    const { value } = await ElMessageBox.prompt(
      '选择评估类型 (Select evaluation type):',
      'LANGSMITH: 运行评估 (Run Evaluation)',
      {
        confirmButtonText: '评估 (Evaluate)',
        cancelButtonText: '取消 (Cancel)',
        inputPlaceholder: 'EFFICIENCY, QUALITY, 或 PERFORMANCE',
        inputValue: 'QUALITY',
        inputValidator: (val) => {
          if (!['EFFICIENCY', 'QUALITY', 'PERFORMANCE'].includes(val.toUpperCase())) {
            return '无效的评估类型。请使用EFFICIENCY、QUALITY或PERFORMANCE'
          }
          return true
        }
      }
    )

    const evaluationType = value.toUpperCase()

    // LANGSMITH: 根据指定标准评估运行
    // Evaluates run against specified criteria (EFFICIENCY, QUALITY, or PERFORMANCE)
    const result = await api.evaluateRun(selectedRunId.value, evaluationType)

    evaluationResults.value = [result]

    ElMessage.success(`评估完成 (Evaluation complete): ${result.score.toFixed(1)}/100 - ${result.passed ? '通过 (PASSED)' : '失败 (FAILED)'}`)
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('运行评估失败 (Failed to run evaluation): ' + error.message)
    }
  }
}

// LANGSMITH: 性能指标 (Performance Metrics)
const viewMetrics = async () => {
  try {
    // LANGSMITH: 检索性能指标
    // Retrieves performance metrics including token usage, latency, and cost
    const metrics = await api.getPerformanceMetrics(selectedRunId.value)
    performanceMetrics.value = metrics
    ElMessage.success('性能指标检索成功 (Performance metrics retrieved)')
  } catch (error) {
    ElMessage.error('检索指标失败 (Failed to retrieve metrics): ' + error.message)
  }
}

// LANGSMITH: 比较开发周期 (Compare Development Cycles)
const compareRuns = async () => {
  try {
    const runIds = traces.value.map(t => t.id).join(',')

    // LANGSMITH: 并排比较多个运行
    // Compares multiple runs side-by-side for performance analysis
    const comparisons = await api.compareRuns(runIds)

    const comparisonText = comparisons.map(c => `
运行ID (Run ID): ${c.runId}
指标 (Metrics):
- Tokens: ${c.metrics?.totalTokens?.toLocaleString() || 0}
- 持续时间 (Duration): ${c.metrics?.totalLatencyMs ? (c.metrics.totalLatencyMs / 1000).toFixed(2) + 's' : 'N/A'}
- 迭代次数 (Iterations): ${c.trace?.iterationCount || 0}
- 修改次数 (Revisions): ${c.trace?.revisionCount || 0}
    `).join('\n')

    ElMessageBox.alert(
      comparisonText,
      'LANGSMITH: 比较开发周期 (Compare Development Cycles)',
      {
        confirmButtonText: '关闭 (Close)',
        type: 'info'
      }
    )
  } catch (error) {
    ElMessage.error('比较运行失败 (Failed to compare runs): ' + error.message)
  }
}

// 辅助函数 (Helper functions)
const selectRun = (row) => {
  selectedRunId.value = row.id
  ElMessage.info(`已选择运行 (Selected run): ${row.id}`)
}

const getRowClassName = ({ row }) => {
  return row.id === selectedRunId.value ? 'selected-row' : ''
}

const getStatusType = (status) => {
  switch (status) {
    case 'COMPLETED': return 'success'
    case 'FAILED': return 'danger'
    case 'IN_PROGRESS': return 'warning'
    default: return 'info'
  }
}

const getScoreType = (score) => {
  if (score >= 70) return 'success'
  if (score >= 50) return 'warning'
  return 'danger'
}

const formatTime = (timestamp) => {
  if (!timestamp) return 'N/A'
  const date = new Date(timestamp)
  return date.toLocaleTimeString()
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

.traces-list,
.evaluation-results,
.metrics-display {
  margin-top: 20px;
}

.traces-list h3,
.evaluation-results h3,
.metrics-display h3 {
  margin-bottom: 10px;
  color: #333;
}

:deep(.el-table__row) {
  cursor: pointer;
}

:deep(.selected-row) {
  background-color: #f0f9ff;
}
</style>
