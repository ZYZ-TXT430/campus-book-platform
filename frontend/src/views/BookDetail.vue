<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../utils/axios'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const buying = ref(false)
const book = ref(null)
const reviewSummary = ref({ avgRating: 0, count: 0, reviews: [] })

function formatPrice(value) {
  const n = Number(value)
  return Number.isFinite(n) ? n.toFixed(1) : '0.0'
}

function formatDate(value) {
  return value ? String(value).slice(0, 10) : '-'
}

async function load() {
  loading.value = true
  try {
    book.value = await request.get(`/books/${route.params.id}`)
  } catch {
    book.value = null
  } finally {
    loading.value = false
  }
}

async function buy() {
  if (!book.value || book.value.status !== 1) {
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认购买《${book.value.title}》？成交价 ¥${formatPrice(book.value.price)}`,
      '确认下单',
      { type: 'warning', confirmButtonText: '确认购买', cancelButtonText: '再想想' }
    )
  } catch {
    return
  }

  buying.value = true
  try {
    await request.post('/orders', { bookId: book.value.id })
    ElMessage.success('下单成功，请到订单页完成支付')
    router.push('/orders')
  } catch {
    // 拦截器已提示（例如"手慢了，该书已被抢走"）
  } finally {
    buying.value = false
  }
}

function goBack() {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/books')
  }
}

async function loadReviews() {
  try {
    reviewSummary.value = await request.get(`/books/${route.params.id}/reviews`)
  } catch {
    reviewSummary.value = { avgRating: 0, count: 0, reviews: [] }
  }
}

onMounted(() => {
  load()
  loadReviews()
})
</script>

<template>
  <div class="detail-page">
    <button class="back" type="button" @click="goBack">← 返回市场</button>

    <div v-if="loading" class="detail-card loading-card">
      <el-skeleton animated>
        <template #template>
          <div class="skeleton-layout">
            <el-skeleton-item variant="image" style="width: 320px; height: 426px; border-radius: 16px" />
            <div class="skeleton-info">
              <el-skeleton-item variant="h1" style="width: 70%" />
              <el-skeleton-item variant="text" style="width: 40%; margin-top: 14px" />
              <el-skeleton-item variant="text" style="width: 100%; margin-top: 40px" />
              <el-skeleton-item variant="text" style="width: 90%; margin-top: 12px" />
            </div>
          </div>
        </template>
      </el-skeleton>
    </div>

    <div v-else-if="book" class="detail-card">
      <div class="cover-wrap">
        <img v-if="book.coverUrl" :src="book.coverUrl" :alt="book.title" />
        <div v-else class="cover-fallback">
          <svg viewBox="0 0 48 48" fill="none" aria-hidden="true">
            <path d="M10 8h20a4 4 0 0 1 4 4v28H14a4 4 0 0 1-4-4V8Z" fill="rgba(255,255,255,0.65)" />
            <path d="M34 12h4v28H14" stroke="rgba(4,120,87,0.45)" stroke-width="2" />
            <path d="M18 18h10M18 25h10M18 32h6" stroke="rgba(4,120,87,0.35)" stroke-width="2" stroke-linecap="round" />
          </svg>
          <span>{{ book.category || '二手书' }}</span>
        </div>
      </div>

      <div class="info">
        <div class="tags">
          <span class="tag category">{{ book.category || '其他' }}</span>
          <span class="tag condition">{{ book.conditionLevel }} 成新</span>
        </div>

        <h1 class="title">{{ book.title }}</h1>
        <p class="author">{{ book.author || '佚名' }}</p>

        <div class="price-row">
          <span class="price"><span class="symbol">¥</span>{{ formatPrice(book.price) }}</span>
          <span v-if="book.originalPrice" class="original">原价 ¥{{ formatPrice(book.originalPrice) }}</span>
        </div>

        <div class="meta-grid">
          <div class="meta-item">
            <span class="label">卖家</span>
            <strong>{{ book.sellerNickname || '匿名同学' }}</strong>
          </div>
          <div class="meta-item">
            <span class="label">浏览</span>
            <strong>{{ book.viewCount }}</strong>
          </div>
          <div class="meta-item">
            <span class="label">库存</span>
            <strong>{{ book.stock }}</strong>
          </div>
          <div class="meta-item">
            <span class="label">发布时间</span>
            <strong>{{ formatDate(book.createdAt) }}</strong>
          </div>
        </div>

        <div class="desc">
          <h3>书籍描述</h3>
          <p>{{ book.description || '卖家还没有填写描述。' }}</p>
        </div>

        <div class="reviews">
          <div class="reviews-head">
            <h3>买家评价</h3>
            <div v-if="reviewSummary.count" class="rating-line">
              <el-rate :model-value="reviewSummary.avgRating" disabled allow-half />
              <span>{{ reviewSummary.avgRating }} 分 · {{ reviewSummary.count }} 条</span>
            </div>
            <span v-else class="no-review">还没有评价</span>
          </div>

          <ul v-if="reviewSummary.reviews.length" class="review-list">
            <li v-for="r in reviewSummary.reviews" :key="r.id">
              <div class="review-top">
                <strong>{{ r.reviewerNickname }}</strong>
                <el-rate :model-value="r.rating" disabled size="small" />
              </div>
              <p>{{ r.content || '没有填写评价内容' }}</p>
            </li>
          </ul>
        </div>

        <button
          class="buy-btn"
          type="button"
          :disabled="buying || book.status !== 1"
          @click="buy"
        >
          {{ book.status === 1 ? (buying ? '下单中…' : '立即购买') : '已售出 / 已下架' }}
        </button>
        <p class="hint">下单后在「我的订单」完成支付，支持校内自提</p>
      </div>
    </div>

    <div v-else class="empty">
      <p class="empty-title">书籍不存在或已被删除</p>
      <button class="search-btn" type="button" @click="router.push('/books')">回到市场</button>
    </div>
  </div>
</template>

<style scoped>
.detail-page {
  max-width: 1040px;
  margin: 0 auto;
  padding: 8px 24px 56px;
}

.back {
  margin-bottom: 20px;
  padding: 8px 14px;
  border: none;
  border-radius: 10px;
  background: transparent;
  color: var(--ink-500);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.back:hover {
  background: #fff;
  color: var(--mint-600);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.detail-card {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 40px;
  padding: 36px;
  border-radius: 24px;
  background: #fff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.loading-card {
  display: block;
}

.skeleton-layout {
  display: flex;
  gap: 40px;
}

.skeleton-info {
  flex: 1;
}

.cover-wrap {
  overflow: hidden;
  border-radius: 16px;
  aspect-ratio: 3 / 4;
  background: var(--mint-50);
}

.cover-wrap img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.cover-fallback {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  height: 100%;
  background: linear-gradient(150deg, #d1fae5 0%, #a7f3d0 100%);
  color: var(--mint-700);
}

.cover-fallback svg {
  width: 72px;
  height: 72px;
}

.cover-fallback span {
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 1px;
  opacity: 0.75;
}

.tags {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
}

.tag {
  padding: 4px 10px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 700;
}

.tag.category {
  background: #f3f4f6;
  color: var(--ink-500);
}

.tag.condition {
  background: var(--mint-100);
  color: var(--mint-700);
}

.title {
  margin: 0;
  font-size: 28px;
  font-weight: 800;
  line-height: 1.35;
  color: var(--ink-900);
}

.author {
  margin: 10px 0 0;
  font-size: 14px;
  color: var(--ink-500);
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 14px;
  margin: 26px 0;
}

.price {
  color: var(--mint-500);
  font-size: 40px;
  font-weight: 800;
  line-height: 1;
}

.price .symbol {
  margin-right: 2px;
  font-size: 20px;
  font-weight: 700;
}

.original {
  color: #9ca3af;
  font-size: 14px;
  text-decoration: line-through;
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  padding: 18px 20px;
  border-radius: 14px;
  background: #f9fafb;
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.meta-item .label {
  color: var(--ink-500);
  font-size: 12px;
}

.meta-item strong {
  color: var(--ink-900);
  font-size: 14px;
  font-weight: 700;
}

.desc {
  margin: 24px 0 28px;
}

.desc h3 {
  margin: 0 0 8px;
  font-size: 14px;
  font-weight: 700;
  color: var(--ink-900);
}

.desc p {
  margin: 0;
  color: var(--ink-500);
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
}

/* 评价 */
.reviews {
  margin-bottom: 26px;
}

.reviews-head {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 12px;
}

.reviews-head h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
}

.rating-line {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--ink-500);
  font-size: 12px;
}

.no-review {
  color: #9ca3af;
  font-size: 12px;
}

.review-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.review-list li {
  padding: 12px 0;
  border-top: 1px solid #f3f4f6;
}

.review-top {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.review-top strong {
  font-size: 13px;
  color: var(--ink-900);
}

.review-list p {
  margin: 0;
  color: var(--ink-500);
  font-size: 13px;
  line-height: 1.6;
}

.buy-btn {
  width: 100%;
  padding: 15px 0;
  border: none;
  border-radius: 14px;
  background: var(--mint-500);
  color: #fff;
  font-size: 16px;
  font-weight: 800;
  letter-spacing: 1px;
  cursor: pointer;
  transition: all 0.25s ease;
}

.buy-btn:hover:not(:disabled) {
  background: var(--mint-600);
  transform: translateY(-1px);
  box-shadow: 0 10px 24px rgba(16, 185, 129, 0.3);
}

.buy-btn:disabled {
  background: #e5e7eb;
  color: #9ca3af;
  cursor: not-allowed;
}

.hint {
  margin: 10px 0 0;
  text-align: center;
  color: #9ca3af;
  font-size: 12px;
}

.empty {
  padding: 90px 0;
  text-align: center;
}

.empty-title {
  margin: 0 0 20px;
  color: var(--ink-500);
  font-size: 16px;
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
}

.search-btn:hover {
  background: var(--mint-600);
}

@media (max-width: 760px) {
  .detail-card {
    grid-template-columns: 1fr;
    gap: 24px;
    padding: 24px;
  }

  .cover-wrap {
    max-width: 260px;
  }

  .title {
    font-size: 22px;
  }

  .meta-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .skeleton-layout {
    flex-direction: column;
  }
}
</style>
