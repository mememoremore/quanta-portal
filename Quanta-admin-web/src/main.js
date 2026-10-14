import { createApp } from 'vue'
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
import 'nprogress/nprogress.css'

import App from './App.vue'
import { pinia } from './stores'
import router from './router'
import { registerDirectives } from './directives'

import './styles/reset.css'
import './styles/variables.css'
import './styles/global.css'
import './styles/portal-variables.css'
import './styles/portal.css'

const app = createApp(App)

app.use(pinia)
app.use(router)
registerDirectives(app)

app.mount('#app')
