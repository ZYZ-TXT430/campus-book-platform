<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="not-found">
    <div class="code">404</div>
    <h1>这个页面走丢了</h1>
    <p>链接可能已经失效，或者你输错了地址。</p>
    <button class="home-btn" type="button" @click="router.push('/books')">
      回到书籍市场
    </button>
  </div>
</template>

<style scoped>
.not-found {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 70vh;
  padding: 40px 24px;
  text-align: center;
}

.code {
  font-size: 96px;
  font-weight: 800;
  line-height: 1;
  color: var(--mint-500);
  letter-spacing: 4px;
  opacity: 0.9;
}

h1 {
  margin: 18px 0 8px;
  font-size: 22px;
  color: var(--ink-900);
}

p {
  margin: 0 0 26px;
  color: var(--ink-500);
  font-size: 14px;
}

.home-btn {
  padding: 12px 28px;
  border: none;
  border-radius: 12px;
  background: var(--mint-500);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.25s ease;
}

.home-btn:hover {
  background: var(--mint-600);
  transform: translateY(-1px);
  box-shadow: 0 10px 22px rgba(16, 185, 129, 0.3);
}
</style>
