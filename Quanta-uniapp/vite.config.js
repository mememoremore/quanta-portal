import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'
// https://vitejs.dev/config/
export default defineConfig({
  plugins: [ //这里可以配置proxy代理,通过vite服务器
    uni(),
  ],
})
