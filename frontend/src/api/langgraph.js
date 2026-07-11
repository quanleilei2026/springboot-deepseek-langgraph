import axios from 'axios'

// API base URL - proxied through Vite dev server
const API_BASE = '/api/workflow'

/**
 * LANGGRAPH & LANGSMITH Demo API Service
 *
 * Provides methods to interact with the backend API for:
 * - LANGGRAPH: Workflow execution, state management, checkpointing
 * - LANGSMITH: Tracing, evaluation, performance metrics
 */

// LANGGRAPH: Start Development Cycle
export const startDevelopmentCycle = async (requirements, threadId = null) => {
  const response = await axios.post(`${API_BASE}/start`, {
    requirements,
    threadId
  })
  return response.data
}

// LANGGRAPH: View Development State
export const getDevelopmentState = async (threadId) => {
  const response = await axios.get(`${API_BASE}/state/${threadId}`)
  return response.data
}

// LANGGRAPH: Pause & Resume Development
export const resumeDevelopment = async (threadId, checkpointName, updatedRequirements = null) => {
  const response = await axios.post(`${API_BASE}/resume`, {
    threadId,
    checkpointName,
    updatedRequirements
  })
  return response.data
}

// LANGGRAPH: Request Revision
export const requestRevision = async (threadId, revisionComments) => {
  const response = await axios.post(`${API_BASE}/revision`, {
    threadId,
    revisionComments
  })
  return response.data
}

// LANGGRAPH: Reset Development
export const resetDevelopment = async (threadId) => {
  const response = await axios.delete(`${API_BASE}/reset/${threadId}`)
  return response.data
}

// LANGGRAPH: View Workflow Graph
export const getWorkflowGraph = async () => {
  const response = await axios.get(`${API_BASE}/graph`)
  return response.data
}

// LANGSMITH: View Development Traces
export const getTraces = async () => {
  const response = await axios.get(`${API_BASE}/traces`)
  return response.data
}

// LANGSMITH: Trace Detail View
export const getTraceDetail = async (runId) => {
  const response = await axios.get(`${API_BASE}/traces/${runId}`)
  return response.data
}

// LANGSMITH: Run Development Evaluation
export const evaluateRun = async (runId, evaluationType) => {
  const response = await axios.post(`${API_BASE}/evaluate`, {
    runId,
    evaluationType
  })
  return response.data
}

// LANGSMITH: Performance Metrics
export const getPerformanceMetrics = async (runId) => {
  const response = await axios.get(`${API_BASE}/metrics/${runId}`)
  return response.data
}

// LANGSMITH: Compare Development Cycles
export const compareRuns = async (runIds) => {
  const response = await axios.get(`${API_BASE}/compare`, {
    params: { runIds }
  })
  return response.data
}
