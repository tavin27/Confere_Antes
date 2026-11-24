import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";

// Em desenvolvimento o navegador chama /api/* no próprio Vite, que repassa
// para o Spring Boot removendo o prefixo (o back-end não usa /api).
// Assim não é preciso configurar CORS no back-end.
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const target = env.DEV_API_TARGET || "http://localhost:8080";
  return {
    plugins: [react()],
    server: {
      port: 5173,
      proxy: {
        "/api": { target, changeOrigin: true, rewrite: (path) => path.replace(/^\/api/, "") },
      },
    },
    test: { environment: "node" },
  };
});
