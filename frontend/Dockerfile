# Frontend Next.js — bước 13. Chạy sau nginx cùng domain nên gọi API bằng đường dẫn tương đối (/api…).
# Biến NEXT_PUBLIC_* được gắn vào lúc build, nên truyền qua build args.

FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
# Dự án hiện chưa có tài nguyên tĩnh; vẫn tạo public để stage chạy luôn copy được
# và sẵn sàng nhận favicon / ảnh tĩnh về sau.
RUN mkdir -p public
ARG NEXT_PUBLIC_API_BASE_URL=""
ARG NEXT_PUBLIC_WS_URL="wss://localhost/ws"
ENV NEXT_PUBLIC_API_BASE_URL=$NEXT_PUBLIC_API_BASE_URL \
    NEXT_PUBLIC_WS_URL=$NEXT_PUBLIC_WS_URL \
    NEXT_TELEMETRY_DISABLED=1
RUN npm run build

FROM node:22-alpine
WORKDIR /app
ENV NODE_ENV=production NEXT_TELEMETRY_DISABLED=1
COPY --from=build --chown=node /app/package.json /app/package-lock.json /app/next.config.ts ./
COPY --from=build --chown=node /app/node_modules ./node_modules
COPY --from=build --chown=node /app/.next ./.next
COPY --from=build --chown=node /app/public ./public
USER node
EXPOSE 3000
CMD ["npm", "start"]
