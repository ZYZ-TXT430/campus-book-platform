<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../utils/axios'
import ImageUpload from '../components/ImageUpload.vue'

const router = useRouter()

const loading = ref(false)
const buyingId = ref(null)

const books = ref([])
const total = ref(0)

const query = reactive({
  pageNum: 1,
  pageSize: 12,
  keyword: '',
  category: ''
})

const categories = [
  { label: '全部', value: '' },
  { label: '教材', value: '教材' },
  { label: '计算机', value: '计算机' },
  { label: '考研', value: '考研' },
  { label: '四六级', value: '四六级' },
  { label: '文学', value: '文学' },
  { label: '其他', value: '其他' }
]

// ---------- 发布 ----------
const publishVisible = ref(false)
const publishLoading = ref(false)
const publishRef = ref()

const emptyPublishForm = () => ({
  title: '',
  author: '',
  category: '',
  originalPrice: null,
  price: null,
  conditionLevel: 9,
  description: '',
  coverUrl: ''
})

const publishForm = reactive(emptyPublishForm())

const publishRules = {
  title: [
    { required: true, message: '请输入书名', trigger: 'blur' },
    { max: 200, message: '书名不能超过 200 字', trigger: 'blur' }
  ],
  price: [{ required: true, message: '请输入售价', trigger: 'blur' }]
}

const categoryOptions = categories.filter((c) => c.value)

const hasKeyword = computed(() => query.keyword.trim().length > 0)

function formatPrice(value) {
  const n = Number(value)
  return Number.isFinite(n) ? n.toFixed(1) : '0.0'
}

async function load() {
  loading.value = true
  try {
    const data = await request.get('/books', { params: query })
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

function pickCategory(value) {
  query.category = value
  query.pageNum = 1
  load()
}

function clearAll() {
  query.keyword = ''
  query.category = ''
  query.pageNum = 1
  load()
}

function changePage(page) {
  query.pageNum = page
  load()
}

function goDetail(book) {
  router.push(`/books/${book.id}`)
}

function openPublish() {
  Object.assign(publishForm, emptyPublishForm())
  publishVisible.value = true
}

async function submitPublish() {
  const valid = await publishRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  publishLoading.value = true
  try {
    await request.post('/sell/books', publishForm)
    ElMessage.success('发布成功，已上架')
    publishVisible.value = false
    query.pageNum = 1
    load()
  } catch {
    // 拦截器已提示
  } finally {
    publishLoading.value = false
  }
}

async function buy(book) {
  try {
    await ElMessageBox.confirm(
      `确认购买《${book.title}》？成交价 ¥${formatPrice(book.price)}`,
      '确认下单',
      { type: 'warning', confirmButtonText: '确认购买', cancelButtonText: '再想想' }
    )
  } catch {
    return
  }

  buyingId.value = book.id
  try {
    await request.post('/orders', { bookId: book.id })
    ElMessage.success('下单成功，订单状态：待支付')
    load()
  } catch {
    // 拦截器已提示（例如"手慢了，该书已被抢走"）
  } finally {
    buyingId.value = null
  }
}

onMounted(load)
</script>

<template>
  <div class="market">
    <!-- 顶部：标题 + 搜索 + 分类 -->
    <section class="hero">
      <div class="hero-text">
        <h1>遇见下一本好书</h1>
        <p>校园里的二手书，同校当面交易，价格更实在</p>
      </div>

      <div class="search-bar">
        <svg class="search-icon" viewBox="0 0 24 24" fill="none" aria-hidden="true">
          <circle cx="11" cy="11" r="7" stroke="currentColor" stroke-width="2" />
          <path d="M20 20L16.65 16.65" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
        </svg>
        <input
          v-model="query.keyword"
          type="text"
          placeholder="搜索书名，例如：高等数学"
          @keyup.enter="search"
        />
        <button v-if="hasKeyword" class="clear-btn" type="button" @click="clearAll">清空</button>
        <button class="search-btn" type="button" @click="search">搜索</button>
      </div>

      <div class="chips">
        <button
          v-for="item in categories"
          :key="item.value"
          type="button"
          class="chip"
          :class="{ active: query.category === item.value }"
          @click="pickCategory(item.value)"
        >
          {{ item.label }}
        </button>
      </div>
    </section>

    <!-- 结果栏 -->
    <div class="results-head">
      <div class="results-count">
        共 <strong>{{ total }}</strong> 本在售
        <span v-if="query.category" class="results-filter">· {{ query.category }}</span>
        <span v-if="hasKeyword" class="results-filter">· “{{ query.keyword }}”</span>
      </div>
      <button class="publish-btn" type="button" @click="openPublish">
        <span class="plus">+</span> 发布书籍
      </button>
    </div>

    <!-- 骨架屏 -->
    <div v-if="loading" class="grid">
      <div v-for="i in 8" :key="i" class="skeleton-card">
        <el-skeleton animated>
          <template #template>
            <el-skeleton-item variant="image" style="width: 100%; height: 230px" />
            <div style="padding: 16px">
              <el-skeleton-item variant="h3" style="width: 80%" />
              <el-skeleton-item variant="text" style="width: 50%; margin-top: 10px" />
              <el-skeleton-item variant="text" style="width: 70%; margin-top: 18px" />
            </div>
          </template>
        </el-skeleton>
      </div>
    </div>

    <!-- 书籍网格 -->
    <div v-else-if="books.length" class="grid">
      <article
        v-for="book in books"
        :key="book.id"
        class="book-card"
        @click="goDetail(book)"
      >
        <div class="cover">
          <img v-if="book.coverUrl" :src="book.coverUrl" :alt="book.title" loading="lazy" />

          <div v-else class="cover-fallback">
            <svg viewBox="0 0 48 48" fill="none" aria-hidden="true">
              <path
                d="M10 8h20a4 4 0 0 1 4 4v28H14a4 4 0 0 1-4-4V8Z"
                fill="rgba(255,255,255,0.65)"
              />
              <path d="M34 12h4v28H14" stroke="rgba(4,120,87,0.45)" stroke-width="2" />
              <path d="M18 18h10M18 25h10M18 32h6" stroke="rgba(4,120,87,0.35)" stroke-width="2" stroke-linecap="round" />
            </svg>
            <span>{{ book.category || '二手书' }}</span>
          </div>

          <div class="cover-mask">
            <button
              class="buy-text"
              type="button"
              :disabled="buyingId === book.id"
              @click.stop="buy(book)"
            >
              {{ buyingId === book.id ? '下单中…' : '立即购买' }}
            </button>
          </div>
        </div>

        <div class="info">
          <h3 class="title" :title="book.title">{{ book.title }}</h3>
          <p class="seller">
            {{ book.sellerNickname || '匿名同学' }}
            <span v-if="book.category">· {{ book.category }}</span>
          </p>

          <div class="meta">
            <div class="price">
              <span class="symbol">¥</span>{{ formatPrice(book.price) }}
            </div>
            <span class="condition">{{ book.conditionLevel }} 成新</span>
          </div>
        </div>
      </article>
    </div>

    <!-- 空状态 -->
    <div v-else class="empty">
      <div class="empty-icon">
        <svg viewBox="0 0 48 48" fill="none" aria-hidden="true">
          <path d="M8 12h14a4 4 0 0 1 4 4v24H12a4 4 0 0 1-4-4V12Z" stroke="#10B981" stroke-width="2.5" />
          <path d="M40 12H26a4 4 0 0 0-4 4v24h14a4 4 0 0 0 4-4V12Z" stroke="#10B981" stroke-width="2.5" />
        </svg>
      </div>
      <p class="empty-title">没有找到相关书籍</p>
      <p class="empty-sub">换个关键词，或者去发布一本</p>
      <button class="search-btn" type="button" @click="clearAll">查看全部</button>
    </div>

    <!-- 分页 -->
    <div v-if="total > 0" class="pager">
      <el-pagination
        background
        layout="prev, pager, next"
        :total="total"
        :page-size="query.pageSize"
        :current-page="query.pageNum"
        @current-change="changePage"
      />
    </div>

    <!-- 发布对话框 -->
    <el-dialog v-model="publishVisible" title="发布二手书" width="520px">
      <el-form ref="publishRef" :model="publishForm" :rules="publishRules" label-width="80px">
        <el-form-item label="书名" prop="title">
          <el-input v-model="publishForm.title" placeholder="请输入书名" clearable />
        </el-form-item>

        <el-form-item label="作者" prop="author">
          <el-input v-model="publishForm.author" placeholder="选填" clearable />
        </el-form-item>

        <el-form-item label="分类" prop="category">
          <el-select v-model="publishForm.category" placeholder="请选择分类" clearable style="width: 100%">
            <el-option
              v-for="item in categoryOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="封面">
          <ImageUpload v-model="publishForm.coverUrl" />
        </el-form-item>

        <el-form-item label="售价" prop="price">
          <el-input-number v-model="publishForm.price" :min="0" :precision="2" :step="1" style="width: 200px" />
        </el-form-item>

        <el-form-item label="原价" prop="originalPrice">
          <el-input-number v-model="publishForm.originalPrice" :min="0" :precision="2" :step="1" style="width: 200px" />
        </el-form-item>

        <el-form-item label="成色" prop="conditionLevel">
          <el-input-number v-model="publishForm.conditionLevel" :min="1" :max="10" style="width: 200px" />
          <span class="hint">10 表示全新</span>
        </el-form-item>

        <el-form-item label="描述" prop="description">
          <el-input v-model="publishForm.description" type="textarea" :rows="3" placeholder="品相、瑕疵、交易地点等" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="publishVisible = false">取消</el-button>
        <el-button type="primary" :loading="publishLoading" @click="submitPublish">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.market {
  max-width: 1200px;
  margin: 0 auto;
  padding: 8px 24px 56px;
}

/* ---------- Hero ---------- */
.hero {
  position: relative;
  overflow: hidden;
  padding: 44px 44px 36px;
  margin-bottom: 28px;
  border-radius: 24px;
  background: linear-gradient(135deg, #ecfdf5 0%, #f0fdfa 55%, #f9fafb 100%);
}

.hero::after {
  content: '';
  position: absolute;
  right: -80px;
  top: -100px;
  width: 320px;
  height: 320px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(16, 185, 129, 0.18), transparent 65%);
  pointer-events: none;
}

.hero-text h1 {
  margin: 0;
  font-size: 30px;
  font-weight: 800;
  letter-spacing: 0.5px;
  color: #064e3b;
}

.hero-text p {
  margin: 10px 0 26px;
  font-size: 14px;
  color: var(--ink-500);
}

.search-bar {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  max-width: 620px;
  padding: 8px 8px 8px 18px;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(16, 185, 129, 0.12);
}

.search-icon {
  width: 20px;
  height: 20px;
  flex: 0 0 auto;
  color: var(--mint-500);
}

.search-bar input {
  flex: 1;
  min-width: 0;
  padding: 10px 0;
  border: none;
  outline: none;
  background: transparent;
  font-size: 15px;
  color: var(--ink-900);
}

.search-bar input::placeholder {
  color: #9ca3af;
}

.clear-btn {
  border: none;
  background: transparent;
  color: var(--ink-500);
  font-size: 13px;
  cursor: pointer;
}

.clear-btn:hover {
  color: var(--mint-600);
}

.search-btn {
  padding: 10px 26px;
  border: none;
  border-radius: 12px;
  background: var(--mint-500);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.25s ease, transform 0.25s ease;
}

.search-btn:hover {
  background: var(--mint-600);
  transform: translateY(-1px);
}

.chips {
  position: relative;
  z-index: 1;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 24px;
}

.chip {
  padding: 8px 20px;
  border: none;
  border-radius: 999px;
  background: #fff;
  color: var(--ink-500);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.25s ease;
}

.chip:hover {
  color: var(--mint-600);
  background: var(--mint-50);
}

.chip.active {
  color: #fff;
  background: var(--mint-500);
  box-shadow: 0 8px 18px rgba(16, 185, 129, 0.35);
}

/* ---------- 结果栏 ---------- */
.results-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.results-count {
  font-size: 14px;
  color: var(--ink-500);
}

.results-count strong {
  font-size: 20px;
  color: var(--ink-900);
}

.results-filter {
  color: var(--mint-600);
}

.publish-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  border: 1.5px solid var(--mint-500);
  border-radius: 12px;
  background: #fff;
  color: var(--mint-600);
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.25s ease;
}

.publish-btn:hover {
  background: var(--mint-500);
  color: #fff;
  box-shadow: 0 8px 18px rgba(16, 185, 129, 0.3);
}

.publish-btn .plus {
  font-size: 18px;
  line-height: 1;
}

/* ---------- 网格 ---------- */
.grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 24px;
}

.book-card,
.skeleton-card {
  overflow: hidden;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.book-card {
  cursor: pointer;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.book-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 18px 36px rgba(16, 185, 129, 0.18), 0 8px 16px rgba(0, 0, 0, 0.06);
}

/* 封面 */
.cover {
  position: relative;
  aspect-ratio: 3 / 4;
  overflow: hidden;
  background: var(--mint-50);
}

.cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 0.4s ease;
}

.book-card:hover .cover img {
  transform: scale(1.04);
}

.cover-fallback {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  height: 100%;
  background: linear-gradient(150deg, #d1fae5 0%, #a7f3d0 100%);
  color: var(--mint-700);
}

.cover-fallback svg {
  width: 56px;
  height: 56px;
}

.cover-fallback span {
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 1px;
  opacity: 0.75;
}

.cover-mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(6, 78, 59, 0.42);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.book-card:hover .cover-mask {
  opacity: 1;
}

.buy-text {
  padding: 10px 24px;
  border: none;
  border-radius: 999px;
  background: #fff;
  color: var(--mint-600);
  font-family: inherit;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.18);
  transition: transform 0.2s ease;
}

.buy-text:hover {
  transform: scale(1.04);
}

.buy-text.loading {
  opacity: 0.8;
}

/* 信息区 */
.info {
  padding: 16px 18px 18px;
}

.title {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 42px;
  margin: 0;
  font-size: 15px;
  font-weight: 800;
  line-height: 1.4;
  color: var(--ink-900);
}

.seller {
  margin: 8px 0 14px;
  font-size: 12px;
  color: var(--ink-500);
}

.meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.price {
  color: var(--mint-500);
  font-size: 22px;
  font-weight: 800;
  line-height: 1;
}

.price .symbol {
  margin-right: 1px;
  font-size: 14px;
  font-weight: 700;
}

.condition {
  padding: 4px 10px;
  border-radius: 8px;
  background: var(--mint-100);
  color: var(--mint-700);
  font-size: 12px;
  font-weight: 700;
}

/* 空状态 */
.empty {
  padding: 72px 0;
  text-align: center;
}

.empty-icon svg {
  width: 72px;
  height: 72px;
  opacity: 0.85;
}

.empty-title {
  margin: 18px 0 6px;
  font-size: 17px;
  font-weight: 700;
  color: var(--ink-900);
}

.empty-sub {
  margin: 0 0 20px;
  font-size: 13px;
  color: var(--ink-500);
}

/* 分页 */
.pager {
  display: flex;
  justify-content: center;
  margin-top: 40px;
}

.hint {
  margin-left: 8px;
  color: var(--ink-500);
  font-size: 12px;
}

/* ---------- 响应式 ---------- */
@media (max-width: 1100px) {
  .grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 820px) {
  .grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .hero {
    padding: 32px 24px 28px;
  }

  .hero-text h1 {
    font-size: 24px;
  }
}

@media (max-width: 520px) {
  .market {
    padding: 8px 16px 40px;
  }

  .grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .search-bar {
    padding: 6px 6px 6px 14px;
  }

  .search-btn {
    padding: 10px 18px;
  }
}
</style>
