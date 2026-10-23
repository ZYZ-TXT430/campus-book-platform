<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../utils/axios'

const route = useRoute()
const router = useRouter()

const formRef = ref()
const loading = ref(false)

const form = reactive({
  studentNo: '',
  password: ''
})

const rules = {
  studentNo: [
    { required: true, message: '请输入学号', trigger: 'blur' },
    { pattern: /^\d{6,20}$/, message: '学号为 6-20 位数字', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度为 6-32 位', trigger: 'blur' }
  ]
}

// ---------- 注册 ----------
const registerVisible = ref(false)
const registerLoading = ref(false)
const registerRef = ref()

const registerForm = reactive({
  studentNo: '',
  nickname: '',
  password: '',
  phone: '',
  campus: ''
})

const registerRules = {
  studentNo: [
    { required: true, message: '请输入学号', trigger: 'blur' },
    { pattern: /^\d{6,20}$/, message: '学号为 6-20 位数字', trigger: 'blur' }
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 20, message: '昵称长度为 2-20 位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度为 6-32 位', trigger: 'blur' }
  ],
  phone: [
    {
      validator: (rule, value, callback) => {
        if (!value || /^1[3-9]\d{9}$/.test(value)) {
          callback()
        } else {
          callback(new Error('手机号格式不正确'))
        }
      },
      trigger: 'blur'
    }
  ]
}

function saveLogin(data) {
  localStorage.setItem('token', data.token)
  localStorage.setItem(
    'user',
    JSON.stringify({
      userId: data.userId,
      nickname: data.nickname,
      avatarUrl: data.avatarUrl,
      role: data.role
    })
  )
}

async function submit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  loading.value = true
  try {
    // 拦截器已经把 { code, message, data } 拆成了 data
    const data = await request.post('/auth/login', form)

    saveLogin(data)

    ElMessage.success('登录成功')
    const redirect = route.query.redirect
    router.push(typeof redirect === 'string' && redirect ? redirect : { name: 'books' })
  } catch {
    // 错误提示已由 axios 拦截器统一处理
  } finally {
    loading.value = false
  }
}

async function submitRegister() {
  const valid = await registerRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  registerLoading.value = true
  try {
    // 后端 @Pattern 只对 null 放行，空字符串会校验失败，所以空字段直接不传
    const payload = { ...registerForm }
    if (!payload.phone) delete payload.phone
    if (!payload.campus) delete payload.campus

    const data = await request.post('/auth/register', payload)

    saveLogin(data)
    registerVisible.value = false
    ElMessage.success('注册成功，已自动登录')
    router.push({ name: 'books' })
  } catch {
    // 错误提示已由 axios 拦截器统一处理
  } finally {
    registerLoading.value = false
  }
}
</script>

<template>
  <div class="login-wrap">
    <el-card class="login-card" shadow="always">
      <template #header>
        <div class="login-title">校园二手书交易平台</div>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @keyup.enter="submit"
      >
        <el-form-item label="学号" prop="studentNo">
          <el-input
            v-model="form.studentNo"
            placeholder="请输入学号"
            clearable
            autocomplete="username"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            show-password
            autocomplete="current-password"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" class="submit-btn" :loading="loading" @click="submit">
            登录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="tips">
        首次使用请先注册账号
      </div>

      <div class="register-link">
        没有账号？
        <el-link type="primary" :underline="false" @click="registerVisible = true">
          立即注册
        </el-link>
      </div>
    </el-card>

    <el-dialog v-model="registerVisible" title="注册账号" width="420px">
      <el-form
        ref="registerRef"
        :model="registerForm"
        :rules="registerRules"
        label-position="top"
      >
        <el-form-item label="学号" prop="studentNo">
          <el-input v-model="registerForm.studentNo" placeholder="6-20 位数字" clearable />
        </el-form-item>

        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="registerForm.nickname" placeholder="2-20 个字符" clearable />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="registerForm.password"
            type="password"
            placeholder="6-32 位"
            show-password
          />
        </el-form-item>

        <el-form-item label="手机号（选填）" prop="phone">
          <el-input v-model="registerForm.phone" placeholder="用于联系卖家" clearable />
        </el-form-item>

        <el-form-item label="校区（选填）" prop="campus">
          <el-input v-model="registerForm.campus" placeholder="如：东校区" clearable />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="registerVisible = false">取消</el-button>
        <el-button type="primary" :loading="registerLoading" @click="submitRegister">
          注册并登录
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.login-wrap {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #e0f2fe 0%, #f5f7fa 100%);
}

.login-card {
  width: 380px;
}

.login-title {
  font-size: 18px;
  font-weight: 600;
  text-align: center;
}

.submit-btn {
  width: 100%;
}

.tips {
  color: #909399;
  font-size: 12px;
  text-align: center;
}

.register-link {
  margin-top: 10px;
  text-align: center;
  font-size: 13px;
  color: #909399;
}
</style>
