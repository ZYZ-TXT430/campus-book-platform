<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../utils/axios'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:modelValue'])
const uploading = ref(false)

function beforeUpload(file) {
  const okType = ['image/jpeg', 'image/png', 'image/webp', 'image/gif'].includes(file.type)
  if (!okType) {
    ElMessage.error('只支持 jpg / png / webp / gif')
    return false
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('图片不能超过 5MB')
    return false
  }
  return true
}

async function customUpload(options) {
  const form = new FormData()
  form.append('file', options.file)
  uploading.value = true
  try {
    const url = await request.post('/files/upload', form)
    emit('update:modelValue', url)
    ElMessage.success('封面上传成功')
  } catch {
    // 拦截器已提示
  } finally {
    uploading.value = false
  }
}

function remove() {
  emit('update:modelValue', '')
}
</script>

<template>
  <div class="image-upload">
    <el-upload
      :show-file-list="false"
      :before-upload="beforeUpload"
      :http-request="customUpload"
      accept="image/*"
    >
      <div v-if="props.modelValue" class="preview">
        <img :src="props.modelValue" alt="封面预览" />
        <div class="preview-mask">点击更换</div>
      </div>

      <div v-else class="placeholder">
        <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
          <path d="M4 16.5 9 11l3.5 3.5L16 10l4 4.5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
          <rect x="3.5" y="5" width="17" height="14" rx="3" stroke="currentColor" stroke-width="1.8" />
          <circle cx="9" cy="9.5" r="1.4" fill="currentColor" />
        </svg>
        <span>{{ uploading ? '上传中…' : '上传封面' }}</span>
        <small>jpg / png，不超过 5MB</small>
      </div>
    </el-upload>

    <button v-if="props.modelValue" class="remove" type="button" @click="remove">移除封面</button>
  </div>
</template>

<style scoped>
.image-upload {
  display: flex;
  align-items: flex-end;
  gap: 12px;
}

.placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 120px;
  height: 150px;
  border: 1.5px dashed #a7f3d0;
  border-radius: 12px;
  background: #f0fdf9;
  color: var(--mint-600);
  cursor: pointer;
  transition: all 0.25s ease;
}

.placeholder:hover {
  border-color: var(--mint-500);
  background: var(--mint-50);
}

.placeholder svg {
  width: 28px;
  height: 28px;
}

.placeholder span {
  font-size: 13px;
  font-weight: 700;
}

.placeholder small {
  color: #9ca3af;
  font-size: 11px;
}

.preview {
  position: relative;
  width: 120px;
  height: 150px;
  overflow: hidden;
  border-radius: 12px;
  cursor: pointer;
}

.preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.preview-mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(6, 78, 59, 0.45);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  opacity: 0;
  transition: opacity 0.25s ease;
}

.preview:hover .preview-mask {
  opacity: 1;
}

.remove {
  padding: 6px 0;
  border: none;
  background: transparent;
  color: #9ca3af;
  font-size: 12px;
  cursor: pointer;
}

.remove:hover {
  color: #ef4444;
}
</style>
