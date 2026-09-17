import path from 'path'
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { VitePWA } from 'vite-plugin-pwa'

const url = 'http://localhost:8080'
  
export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
    VitePWA({
      registerType: 'autoUpdate',
      includeAssets: ['favicon.ico'],
      manifest: {
        name: 'Cali Arena',
        short_name: 'Cali Arena',
        description: 'Calisthenics tournament management — judge, live, brackets',
        start_url: '/judge',
        display: 'standalone',
        background_color: '#1a1a1a',
        theme_color: '#e05555',
        orientation: 'portrait-primary',
        icons: [
          { src: '/icons/icon-72.png', sizes: '72x72', type: 'image/png', purpose: 'any maskable' },
          { src: '/icons/icon-96.png', sizes: '96x96', type: 'image/png', purpose: 'any maskable' },
          { src: '/icons/icon-128.png', sizes: '128x128', type: 'image/png', purpose: 'any maskable' },
          { src: '/icons/icon-144.png', sizes: '144x144', type: 'image/png', purpose: 'any maskable' },
          { src: '/icons/icon-152.png', sizes: '152x152', type: 'image/png', purpose: 'any maskable' },
          { src: '/icons/icon-192.png', sizes: '192x192', type: 'image/png', purpose: 'any maskable' },
          { src: '/icons/icon-384.png', sizes: '384x384', type: 'image/png', purpose: 'any maskable' },
          { src: '/icons/icon-512.png', sizes: '512x512', type: 'image/png', purpose: 'any maskable' }
        ],
        shortcuts: [
          { name: 'Judge', short_name: 'Judge', description: 'Open judge screen', url: '/judge', icons: [{ src: '/icons/icon-192.png', sizes: '192x192' }] },
          { name: 'Dashboard', short_name: 'Dashboard', description: 'Open admin dashboard', url: '/dashboard', icons: [{ src: '/icons/icon-192.png', sizes: '192x192' }] }
        ],
        categories: ['sports', 'utilities']
      },
      workbox: {
        // Precache the app shell so the SPA loads offline; API/WS traffic passes through to the network.
        cleanupOutdatedCaches: true,
        maximumFileSizeToCacheInBytes: 5 * 1024 * 1024,
        globPatterns: ['**/*.{js,css,html,ico,png,svg,woff2}'],
        navigateFallback: '/index.html',
        navigateFallbackDenylist: [/^\/api/, /^\/ws/],
        skipWaiting: true,
        clientsClaim: true
      },
      devOptions: {
        enabled: true,
        type: 'module'
      }
    })
  ],

  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },

  server: {
    allowedHosts: true,
    proxy: {
      '/api': {
        target: url,
        changeOrigin: true,
      },
      "/ws": {
        target: url,
        ws: true
      },
    }
  },
})