<script setup>
import { onMounted, ref } from 'vue'
import request from '../utils/axios'

const loading = ref(false)
const user = ref(null)

async function load() {
  loading.value = true
  try {
    user.value = await request.get('/users/me')
  } catch {
    user.value = null
  } finally {
    loading.value = false
  }
}

function formatTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 19) : '-'
}

onMounted(load)
</script>

<template>
  <div class="page profile-page">
    <el-card v-loading="loading" shadow="never">
      <div class="profile-header">
        <el-avatar :size="72" class="avatar">
          {{ user?.nickname?.charAt(0) || '书' }}
        </el-avatar>
        <div class="identity">
          <div class="name">{{ user?.nickname || '未登录' }}</div>
          <div class="sub">学号 {{ user?.studentNo || '-' }}</div>
        </div>
      </div>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="学号">{{ user?.studentNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ user?.nickname || '-' }}</el-descriptions-item>
        <el-descriptions-item label="校区">{{ user?.campus || '未填写' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ user?.phone || '未填写' }}</el-descriptions-item>
        <el-descriptions-item label="信用分">
          <el-tag type="success" size="small">{{ user?.creditScore ?? '-' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="角色">
          {{ user?.role === 1 ? '管理员' : '普通用户' }}
        </el-descriptions-item>
        <el-descriptions-item label="注册时间">
          {{ formatTime(user?.createdAt) }}
        </el-descriptions-item>
        <el-descriptions-item label="账号状态">
          {{ user?.status === 1 ? '正常' : '禁用' }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<style scoped>
.profile-page {
  max-width: 860px;
}

.profile-header {
  display: flex;
  align-items: center;
  gap: 18px;
  margin-bottom: 24px;
}

.avatar {
  background: #409eff;
  font-size: 28px;
}

.name {
  font-size: 20px;
  font-weight: 600;
}

.sub {
  margin-top: 6px;
  color: #909399;
  font-size: 13px;
}
</style>
