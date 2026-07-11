<template>
  <div id="app">
    <el-container>
      <!-- Header -->
      <el-header>
        <div class="header-content">
          <h1>
            <el-icon><el-icon-cpu /></el-icon>
            LangGraph & LangSmith Demo
          </h1>
          <p class="subtitle">Multi-Agent Code Development Workflow with Full Observability</p>
        </div>
      </el-header>

      <!-- Main Content -->
      <el-main>
        <el-row :gutter="20">
          <!-- LangGraph Panel -->
          <el-col :span="12">
            <lang-graph-panel />
          </el-col>

          <!-- LangSmith Panel -->
          <el-col :span="12">
            <lang-smith-panel />
          </el-col>
        </el-row>
      </el-main>

      <!-- Footer -->
      <el-footer>
        <p>Author: quanleilei | Date: 2026-02-28 | Tech: Vue3 + SpringBoot + DeepSeek + LangGraph + LangSmith</p>
      </el-footer>
    </el-container>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import LangGraphPanel from './components/LangGraphPanel.vue'
import LangSmithPanel from './components/LangSmithPanel.vue'

onMounted(() => {
  console.log('LangGraph & LangSmith Demo loaded')
})
</script>

<style scoped>
#app {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.el-header {
  background: white;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  padding: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.header-content {
  text-align: center;
}

.header-content h1 {
  margin: 0;
  color: #333;
  font-size: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.subtitle {
  margin: 10px 0 0 0;
  color: #666;
  font-size: 14px;
}

.el-main {
  padding: 20px;
}

.el-footer {
  background: white;
  text-align: center;
  color: #999;
  font-size: 12px;
  padding: 15px;
}
</style>
