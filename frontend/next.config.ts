import type { NextConfig } from 'next';

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
  }
};

export default nextConfig;
