<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../utils/axios'

const role = ref('buyer')
const statusFilter = ref(null)
const loading = ref(false)
const orders = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const detailVisible = ref(false)
const current = ref(null)

const statusMap = {
  0: { text: '待支付', type: 'warning' },
  1: { text: '待交接', type: 'primary' },
  2: { text: '已完成', type: 'success' },
  3: { text: '已取消', type: 'info' },
  4: { text: '已退款', type: 'danger' }
}

function statusInfo(status) {
  return statusMap[status] || { text: '未知', type: 'info' }
}

function formatTime(value) {
  if (!value) {
    return '-'
  }
  return String(value).replace('T', ' ').slice(0, 19)
}

function deliveryText(type) {
  return type === 2 ? '快递' : '校内自提'
}

async function load() {
  loading.value = true
  try {
    const params = { role: role.value, pageNum: pageNum.value, pageSize: pageSize.value }
    if (statusFilter.value !== null && statusFilter.value !== '') {
      params.status = statusFilter.value
    }

    const data = await request.get('/orders', { params })
    orders.value = data.records || []
    total.value = data.total || 0
  } catch {
    orders.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function changeRole() {
  pageNum.value = 1
  load()
}

function changePage(page) {
  pageNum.value = page
  load()
}

function showDetail(row) {
  current.value = row
  detailVisible.value = true
}

async function pay(row) {
  try {
    await ElMessageBox.confirm(
      `确认支付 ¥${row.totalAmount}？支付后等待卖家交接。`,
      '支付确认',
      { type: 'warning', confirmButtonText: '确认支付' }
    )
  } catch {
    return
  }

  try {
    await request.post(`/orders/${row.id}/pay`)
    ElMessage.success('支付成功，等待交接')
    load()
  } catch {
    // 拦截器已提示
  }
}

async function confirm(row) {
  try {
    await ElMessageBox.confirm(
      `确认已收到《${row.bookTitle}》？`,
      '确认收货',
      { type: 'warning', confirmButtonText: '确认收货' }
    )
  } catch {
    return
  }

  try {
    await request.post(`/orders/${row.id}/confirm`)
    ElMessage.success('已确认收货，交易完成')
    load()
  } catch {
    // 拦截器已提示
  }
}

async function cancel(row) {
  const tip =
    row.status === 0
      ? '取消后订单关闭，书籍会重新上架。'
      : '订单已支付，取消将视为退款，书籍会重新上架。'
  try {
    await ElMessageBox.confirm(tip, '取消订单', {
      type: 'warning',
      confirmButtonText: '确认取消',
      cancelButtonText: '再想想'
    })
  } catch {
    return
  }

  try {
    await request.post(`/orders/${row.id}/cancel`)
    ElMessage.success('订单已取消')
    load()
  } catch {
    // 拦截器已提示
  }
}

// ---------- 评价 ----------
const reviewVisible = ref(false)
const reviewLoading = ref(false)
const reviewForm = reactive({ orderId: null, rating: 5, content: '' })

function openReview(row) {
  reviewForm.orderId = row.id
  reviewForm.rating = 5
  reviewForm.content = ''
  reviewVisible.value = true
}

async function submitReview() {
  reviewLoading.value = true
  try {
    await request.post('/reviews', {
      orderId: reviewForm.orderId,
      rating: reviewForm.rating,
      content: reviewForm.content || null
    })
    ElMessage.success('评价成功，感谢反馈')
    reviewVisible.value = false
    load()
  } catch {
    // 拦截器已提示
  } finally {
    reviewLoading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <el-card shadow="never">
      <el-tabs v-model="role" @tab-change="changeRole">
        <el-tab-pane label="我买到的" name="buyer" />
        <el-tab-pane label="我卖出的" name="seller" />
      </el-tabs>

      <div class="toolbar">
        <el-select
          v-model="statusFilter"
          placeholder="全部状态"
          clearable
          style="width: 160px"
          @change="changeRole"
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

      <el-table v-loading="loading" :data="orders" stripe>
        <el-table-column prop="orderNo" label="订单号" width="190" />
        <el-table-column prop="bookTitle" label="书名" min-width="200" show-overflow-tooltip />
        <el-table-column label="金额" width="100">
          <template #default="{ row }">
            <span class="amount">¥{{ row.totalAmount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusInfo(row.status).type" size="small">
              {{ statusInfo(row.status).text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="交接方式" width="100">
          <template #default="{ row }">{{ deliveryText(row.deliveryType) }}</template>
        </el-table-column>
        <el-table-column :label="role === 'buyer' ? '卖家' : '买家'" width="80">
          <template #default="{ row }">
            #{{ role === 'buyer' ? row.sellerId : row.buyerId }}
          </template>
        </el-table-column>
        <el-table-column label="下单时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="role === 'buyer' && row.status === 0"
              type="primary"
              link
              @click="pay(row)"
            >
              支付
            </el-button>

            <el-button
              v-if="role === 'buyer' && row.status === 1"
              type="success"
              link
              @click="confirm(row)"
            >
              确认收货
            </el-button>

            <el-button
              v-if="role === 'buyer' && row.status === 2"
              type="warning"
              link
              @click="openReview(row)"
            >
              评价
            </el-button>

            <el-button
              v-if="(role === 'buyer' || role === 'seller') && (row.status === 0 || row.status === 1)"
              type="danger"
              link
              @click="cancel(row)"
            >
              取消
            </el-button>

            <el-button type="primary" link @click="showDetail(row)">详情</el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty :description="role === 'buyer' ? '还没有买过书' : '还没有卖出过书'" />
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

    <el-dialog v-model="detailVisible" title="订单详情" width="520px">
      <el-descriptions v-if="current" :column="1" border>
        <el-descriptions-item label="订单号">{{ current.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="书名">{{ current.bookTitle }}</el-descriptions-item>
        <el-descriptions-item label="单价">¥{{ current.unitPrice }}</el-descriptions-item>
        <el-descriptions-item label="数量">{{ current.quantity }}</el-descriptions-item>
        <el-descriptions-item label="总额">¥{{ current.totalAmount }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusInfo(current.status).type" size="small">
            {{ statusInfo(current.status).text }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="交接方式">
          {{ deliveryText(current.deliveryType) }}
        </el-descriptions-item>
        <el-descriptions-item label="地址">
          {{ current.deliveryAddr || '未填写' }}
        </el-descriptions-item>
        <el-descriptions-item label="备注">
          {{ current.remark || '无' }}
        </el-descriptions-item>
        <el-descriptions-item label="买家">#{{ current.buyerId }}</el-descriptions-item>
        <el-descriptions-item label="卖家">#{{ current.sellerId }}</el-descriptions-item>
        <el-descriptions-item label="支付时间">{{ formatTime(current.payTime) }}</el-descriptions-item>
        <el-descriptions-item label="完成时间">{{ formatTime(current.finishTime) }}</el-descriptions-item>
        <el-descriptions-item label="取消原因">{{ current.cancelReason || '无' }}</el-descriptions-item>
        <el-descriptions-item label="下单时间">{{ formatTime(current.createdAt) }}</el-descriptions-item>
      </el-descriptions>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reviewVisible" title="评价卖家" width="460px">
      <div class="review-field">
        <span class="review-label">评分</span>
        <el-rate v-model="reviewForm.rating" show-text />
      </div>

      <el-input
        v-model="reviewForm.content"
        type="textarea"
        :rows="4"
        maxlength="500"
        show-word-limit
        placeholder="说说书的状态、卖家的沟通和交接体验"
      />

      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" :loading="reviewLoading" @click="submitReview">
          提交评价
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.amount {
  color: #f56c6c;
  font-weight: 600;
}

.review-field {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 16px;
}

.review-label {
  color: var(--ink-500);
  font-size: 14px;
}
</style>
