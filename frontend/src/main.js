import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'
import http from './api/http'
import './styles/main.scss'

async function boot() {
  // The public preview is static, so the API is replaced by an in-memory one.
  if (import.meta.env.VITE_DEMO === '1') {
    const { installDemo } = await import('./demo')
    installDemo(http)
  }

  const app = createApp(App)
  for (const [name, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(name, component)
  }
  app.use(ElementPlus).use(router).mount('#app')
}

boot()
