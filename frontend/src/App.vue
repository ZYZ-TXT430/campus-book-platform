<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

const route = useRoute()
const router = useRouter()

// 登录页不需要顶栏
const isPublic = computed(() => route.meta.public === true)

// 顶栏菜单高亮
const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/my-books')) {
    return '/my-books'
  }
  if (path.startsWith('/orders')) {
    return '/orders'
  }
  if (path.startsWith('/profile')) {
    return '/profile'
  }
  if (path.startsWith('/admin')) {
    return '/admin/reports'
  }
  return '/books'
})

const currentUser = computed(() => {
  // 依赖路由，登录跳转后重新读取 localStorage
  route.fullPath
  try {
    return JSON.parse(localStorage.getItem('user') || '{}')
  } catch {
    return {}
  }
})

const nickname = computed(() => currentUser.value.nickname || '同学')
const isAdmin = computed(() => currentUser.value.role === 1)

function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  router.push({ name: 'login' })
}
</script>

<template>
  <el-config-provider :locale="zhCn">
    <router-view v-if="isPublic" />

    <el-container v-else class="layout">
      <el-header class="header">
        <div class="brand">📚 校园二手书</div>

        <el-menu
          mode="horizontal"
          :default-active="activeMenu"
          router
          :ellipsis="false"
          class="nav"
        >
          <el-menu-item index="/books">书籍市场</el-menu-item>
          <el-menu-item index="/my-books">我的发布</el-menu-item>
          <el-menu-item index="/orders">我的订单</el-menu-item>
          <el-menu-item index="/profile">个人中心</el-menu-item>
          <el-menu-item v-if="isAdmin" index="/admin/reports">举报管理</el-menu-item>
        </el-menu>

        <div class="user">
          <span class="nickname">{{ nickname }}</span>
          <el-button link type="primary" @click="logout">退出登录</el-button>
        </div>
      </el-header>

      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-config-provider>
</template>

<style scoped>
.nav {
  flex: 1;
  margin-left: 24px;
  border-bottom: none;
}
</style>
