import { App } from '@/App'
import '@/index.css'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'

const container = document.getElementById('root')
if (!container) {
  throw new Error('Root container #root was not found in index.html')
}

createRoot(container).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
