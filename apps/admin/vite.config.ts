import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Ember admin (manager back-office) — dev server on port 5176.
export default defineConfig({
  plugins: [react()],
  define: { global: 'globalThis' },
  server: { port: 5176 },
});
