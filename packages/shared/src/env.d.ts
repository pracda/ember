// The one env var the shared client reads. Apps provide it via `.env` (VITE_API_BASE);
// Vite statically replaces `import.meta.env.VITE_API_BASE` at build time.
interface ImportMetaEnv {
  readonly VITE_API_BASE?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
