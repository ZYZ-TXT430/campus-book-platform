import { createApp } from 'vue'

import App from './App.vue'
import router from './router'
import './styles/main.css'

// 组件和样式由 unplugin 按需自动导入；ElMessage / ElMessageBox 是 JS API，
// 代码里显式 import，样式需要在这里手动引一次。
import 'element-plus/theme-chalk/el-message.css'
import 'element-plus/theme-chalk/el-message-box.css'

const app = createApp(App)

app.use(router)

app.mount('#app')
