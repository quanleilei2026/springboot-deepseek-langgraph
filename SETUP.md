# 环境配置说明 / Environment Configuration Guide

## 设置API密钥 / Setting API Keys

### Windows PowerShell
```powershell
$env:DEEPSEEK_API_KEY="your_deepseek_api_key"
$env:LANGSMITH_API_KEY="your_langsmith_api_key"
```

### Windows CMD
```cmd
set DEEPSEEK_API_KEY=your_deepseek_api_key
set LANGSMITH_API_KEY=your_langsmith_api_key
```

### Linux/Mac
```bash
export DEEPSEEK_API_KEY=your_deepseek_api_key
export LANGSMITH_API_KEY=your_langsmith_api_key
```

## 获取API密钥 / Getting API Keys

- DeepSeek: https://platform.deepseek.com/
- LangSmith: https://smith.langchain.com/

## 启动应用 / Starting the Application

### 后端 / Backend
```bash
mvn spring-boot:run
```

### 前端 / Frontend
```bash
cd frontend
npm run dev
```

## 编译打包 / Building

### 后端 / Backend
```bash
mvn clean package -DskipTests
```

### 前端 / Frontend
```bash
cd frontend
npm run build
```
