import type { NextConfig } from "next";

// 前端只请求同源 /api/*，由 rewrites 反代到 Java 后端（compose 内为 http://backend:8080）。
// 业务执行与持久化全部在 Java/MySQL 完成，Next.js 仅做静态资源与反向代理。
const backendOrigin = process.env.BACKEND_ORIGIN ?? "http://127.0.0.1:8080";

const nextConfig: NextConfig = {
  output: "standalone",
  async rewrites() {
    return [
      {
        source: "/api/:path*",
        destination: `${backendOrigin}/api/:path*`,
      },
    ];
  },
};

export default nextConfig;
