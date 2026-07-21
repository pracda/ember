import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Ember pos — dev server on port 5173.
// `global` shim: sockjs-client (used by @ember/shared's stream hook) references
// the Node `global`, which doesn't exist in the browser.
export default defineConfig({
  plugins: [react()],
  define: { global: 'globalThis' },
  server: { port: 5173 },
});
