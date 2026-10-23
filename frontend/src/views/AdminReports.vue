<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../utils/axios'

const loading = ref(false)
const reports = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const statusFilter = ref(null)

const statusMap = {
  0: { text: '待处理', type: 'warning' },
  1: { text: '已下架', type: 'success' },
  2: { text: '已驳回', type: 'info' }
}

function statusInfo(status) {
  return statusMap[status] || { text: '未知', type: 'info' }
}

function formatTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 19) : '-'
}

async function load() {
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (statusFilter.value !== null && statusFilter.value !== '') {
      params.status = statusFilter.value
    }
    const data = await request.get('/admin/reports', { params })
    reports.value = data.records || []
    total.value = data.total || 0
  } catch {
    reports.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function changePage(page) {
  pageNum.value = page
  load()
}

function handleFilter() {
  pageNum.value = 1
  load()
}

async function handle(row, action) {
  const isOffline = action === 'offline'
  const tip = isOffline
    ? `确认下架《${row.bookTitle}》？书籍会从市场移除。`
    : `确认驳回对《${row.bookTitle}》的举报？`
  try {
    await ElMessageBox.confirm(tip, isOffline ? '下架书籍' : '驳回举报', {
      type: 'warning',
      confirmButtonText: '确认'
    })
  } catch {
    return
  }

  try {
    await request.post(`/admin/reports/${row.id}/handle`, null, { params: { action } })
    ElMessage.success(isOffline ? '已下架并处理' : '已驳回')
    load()
  } catch {
    // 拦截器已提示
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <el-card shadow="never">
      <div class="toolbar">
        <el-select
          v-model="statusFilter"
          placeholder="全部状态"
          clearable
          style="width: 150px"
          @change="handleFilter"
        >
          <el-option
            v-for="(item, key) in statusMap"
            :key="key"
            :label="item.text"
            :value="Number(key)"
          />
        </el-select>

        <el-button @click="load">刷新</el-button>
      </div>

      <el-table v-loading="loading" :data="reports" stripe>
        <el-table-column prop="bookTitle" label="被举报书籍" min-width="200" show-overflow-tooltip />
        <el-table-column prop="reporterNickname" label="举报人" width="120" />
        <el-table-column prop="reason" label="举报原因" min-width="240" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusInfo(row.status).type" size="small">
              {{ statusInfo(row.status).text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="举报时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="处理时间" width="170">
          <template #default="{ row }">{{ formatTime(row.handledAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 0">
              <el-button type="danger" link @click="handle(row, 'offline')">下架书籍</el-button>
              <el-button type="info" link @click="handle(row, 'reject')">驳回</el-button>
            </template>
            <span v-else class="muted">已处理</span>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="没有举报记录" />
        </template>
      </el-table>

      <div class="pager">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :total="total"
          :page-size="pageSize"
          :current-page="pageNum"
          @current-change="changePage"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.muted {
  color: #909399;
  font-size: 12px;
}
</style>
