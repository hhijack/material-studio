# ---- 依赖阶段 ----
FROM node:22-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

# ---- 构建阶段 ----
# 注意：Next.js standalone 的 rewrites 目标在构建期烘焙（routes-manifest.json），
# 运行期环境变量不会生效，因此后端地址必须作为构建参数注入（compose 通过 build.args 覆盖）。
FROM node:22-alpine AS build
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
ARG BACKEND_ORIGIN=http://backend:8080
ENV NEXT_TELEMETRY_DISABLED=1 \
    BACKEND_ORIGIN=${BACKEND_ORIGIN}
RUN npm run build

# ---- 运行阶段（standalone）----
FROM node:22-alpine AS run
WORKDIR /app
ENV NODE_ENV=production \
    NEXT_TELEMETRY_DISABLED=1 \
    PORT=3000 \
    HOSTNAME=0.0.0.0
COPY --from=build /app/.next/standalone ./
COPY --from=build /app/.next/static ./.next/static
COPY --from=build /app/public ./public
EXPOSE 3000
CMD ["node", "server.js"]
