import type { NextConfig } from 'next';

// Backend mà Next chuyển tiếp tới (chạy phía server Next, trình duyệt không thấy).
const BACKEND_URL = process.env.BACKEND_URL ?? 'http://localhost:8080';

// Không bật cacheComponents / partialPrefetching: dữ liệu lấy từ backend Spring ở phía client
// (token nằm trong localStorage), và antd sinh giá trị ngẫu nhiên khi prerender.
const nextConfig: NextConfig = {
  turbopack: {
    rules: {
      '*.css': {
        loaders: ['@tailwindcss/turbopack'],
        as: '*.css'
      }
    }
  },
  // Chế độ dev chặn domain lạ tải tài nguyên dev (/_next/…): trang hiện HTML nhưng JS không chạy.
  // Cho phép domain của các tunnel thường dùng khi cho người khác thử (chỉ áp dụng `npm run dev`).
  allowedDevOrigins: ['*.trycloudflare.com', '*.ngrok-free.app', '*.ngrok-free.dev', '*.loca.lt', '**.devtunnels.ms'],
  // Proxy cùng origin: khi NEXT_PUBLIC_API_BASE_URL / NEXT_PUBLIC_WS_URL để trống, trình duyệt chỉ gọi tới Next
  // (/api, /uploads, /ws) và Next chuyển tiếp sang backend → chỉ cần mở một cổng (vd một tunnel cho cổng 3000).
  // Chạy sau nginx (Docker) thì nginx đã định tuyến các đường dẫn này trước khi tới Next.
  async rewrites() {
    return [
      { source: '/api/:path*', destination: `${BACKEND_URL}/api/:path*` },
      { source: '/uploads/:path*', destination: `${BACKEND_URL}/uploads/:path*` },
      { source: '/ws', destination: `${BACKEND_URL}/ws` }
    ];
  }
};

export default nextConfig;
