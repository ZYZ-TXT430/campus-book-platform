<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../utils/axios'
import ImageUpload from '../components/ImageUpload.vue'

const loading = ref(false)
const books = ref([])
const total = ref(0)
const statusFilter = ref(null)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: ''
})

const categories = ['教材', '计算机', '考研', '四六级', '文学', '其他']

const statusMap = {
  0: { text: '已下架', type: 'info' },
  1: { text: '在售', type: 'success' },
  2: { text: '已预订', type: 'warning' },
  3: { text: '已售出', type: 'danger' }
}

// ---------- 编辑 ----------
const editVisible = ref(false)
const editLoading = ref(false)
const editRef = ref()
const editId = ref(null)

const editForm = reactive({
  title: '',
  author: '',
  category: '',
  originalPrice: null,
  price: null,
  conditionLevel: 9,
  description: '',
  coverUrl: ''
})

const editRules = {
  title: [
    { required: true, message: '请输入书名', trigger: 'blur' },
    { max: 200, message: '书名不能超过 200 字', trigger: 'blur' }
  ],
  price: [{ required: true, message: '请输入售价', trigger: 'blur' }]
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
    const params = { pageNum: query.pageNum, pageSize: query.pageSize }
    if (query.keyword) {
      params.keyword = query.keyword
    }
    if (statusFilter.value !== null && statusFilter.value !== '') {
      params.status = statusFilter.value
    }

    const data = await request.get('/sell/books', { params })
    books.value = data.records || []
    total.value = data.total || 0
  } catch {
    books.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function search() {
  query.pageNum = 1
  load()
}

function changePage(page) {
  query.pageNum = page
  load()
}

function openEdit(row) {
  editId.value = row.id
  editForm.title = row.title
  editForm.author = row.author || ''
  editForm.category = row.category || ''
  editForm.originalPrice = row.originalPrice == null ? null : Number(row.originalPrice)
  editForm.price = Number(row.price)
  editForm.conditionLevel = row.conditionLevel ?? 9
  editForm.description = row.description || ''
  editForm.coverUrl = row.coverUrl || ''
  editVisible.value = true
}

async function submitEdit() {
  const valid = await editRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  editLoading.value = true
  try {
    await request.put(`/sell/books/${editId.value}`, {
      title: editForm.title,
      author: editForm.author || null,
      category: editForm.category || null,
      originalPrice: editForm.originalPrice,
      price: editForm.price,
      conditionLevel: editForm.conditionLevel,
      description: editForm.description || null,
      coverUrl: editForm.coverUrl || null
    })
    ElMessage.success('保存成功')
    editVisible.value = false
    load()
  } catch {
    // 拦截器已提示
  } finally {
    editLoading.value = false
  }
}

async function toggle(row) {
  const target = row.status === 1 ? 0 : 1
  try {
    await request.put(`/sell/books/${row.id}/status`, null, { params: { status: target } })
    ElMessage.success(target === 1 ? '已重新上架' : '已下架')
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
        <el-input
          v-model="query.keyword"
          placeholder="按书名搜索"
          clearable
          style="width: 240px"
          @keyup.enter="search"
        />

        <el-select
          v-model="statusFilter"
          placeholder="全部状态"
          clearable
          style="width: 150px"
          @change="search"
        >
          <el-option
            v-for="(item, key) in statusMap"
            :key="key"
            :label="item.text"
            :value="Number(key)"
          />
        </el-select>

        <el-button type="primary" @click="search">搜索</el-button>
        <el-button @click="load">刷新</el-button>

        <div class="spacer" />
        <el-button type="success" @click="$router.push('/books')">去市场发布</el-button>
      </div>

      <el-table v-loading="loading" :data="books" stripe>
        <el-table-column prop="title" label="书名" min-width="220" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column label="价格" width="100">
          <template #default="{ row }">
            <span class="price">¥{{ row.price }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusInfo(row.status).type" size="small">
              {{ statusInfo(row.status).text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="80" />
        <el-table-column prop="viewCount" label="浏览" width="80" />
        <el-table-column label="发布时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 0 || row.status === 1"
              type="primary"
              link
              @click="openEdit(row)"
            >
              编辑
            </el-button>

            <el-button
              v-if="row.status === 1"
              type="danger"
              link
              @click="toggle(row)"
            >
              下架
            </el-button>
            <el-button
              v-else-if="row.status === 0"
              type="success"
              link
              @click="toggle(row)"
            >
              重新上架
            </el-button>
            <span v-else class="muted">不可操作</span>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="你还没有发布过书籍" />
        </template>
      </el-table>

      <div class="pager">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :total="total"
          :page-size="query.pageSize"
          :current-page="query.pageNum"
          @current-change="changePage"
        />
      </div>
    </el-card>

    <el-dialog v-model="editVisible" title="编辑书籍" width="520px">
      <el-form ref="editRef" :model="editForm" :rules="editRules" label-width="80px">
        <el-form-item label="书名" prop="title">
          <el-input v-model="editForm.title" placeholder="请输入书名" clearable />
        </el-form-item>

        <el-form-item label="作者" prop="author">
          <el-input v-model="editForm.author" placeholder="选填" clearable />
        </el-form-item>

        <el-form-item label="分类" prop="category">
          <el-select v-model="editForm.category" placeholder="请选择分类" clearable style="width: 100%">
            <el-option v-for="item in categories" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>

        <el-form-item label="封面">
          <ImageUpload v-model="editForm.coverUrl" />
        </el-form-item>

        <el-form-item label="售价" prop="price">
          <el-input-number
            v-model="editForm.price"
            :min="0"
            :precision="2"
            :step="1"
            style="width: 200px"
          />
        </el-form-item>

        <el-form-item label="原价" prop="originalPrice">
          <el-input-number
            v-model="editForm.originalPrice"
            :min="0"
            :precision="2"
            :step="1"
            style="width: 200px"
          />
        </el-form-item>

        <el-form-item label="成色" prop="conditionLevel">
          <el-input-number
            v-model="editForm.conditionLevel"
            :min="1"
            :max="10"
            style="width: 200px"
          />
          <span class="hint">10 表示全新</span>
        </el-form-item>

        <el-form-item label="描述" prop="description">
          <el-input v-model="editForm.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="editLoading" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.price {
  color: #f56c6c;
  font-weight: 600;
}

.spacer {
  flex: 1;
}

.muted {
  color: #909399;
  font-size: 12px;
}

.hint {
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
}
</style>
