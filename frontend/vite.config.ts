import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // In dev, forward API calls to the backend started by docker compose.
    proxy: { '/api': 'http://localhost:8080' },
  },
});
