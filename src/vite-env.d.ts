/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly DEV: boolean;
  readonly MODE: string;
  readonly BASE_URL: string;
  readonly PROD: boolean;
  readonly SSR: boolean;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

declare module '*?url' {
  const src: string;
  export default src;
}

declare module 'pdfjs-dist/build/pdf.worker.min.mjs?url' {
  const workerUrl: string;
  export default workerUrl;
}