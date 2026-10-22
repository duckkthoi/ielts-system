import { AntdRegistry } from '@ant-design/nextjs-registry';
import type { Metadata } from 'next';
import { JetBrains_Mono, Noto_Serif, Roboto } from 'next/font/google';

import './globals.css';
import Providers from './providers';

// Tên biến CSS phải khác token font của Tailwind (--font-sans/serif/mono) để tránh tham chiếu vòng.
const sans = Roboto({
  variable: '--font-roboto',
  subsets: ['latin', 'vietnamese'],
  weight: ['400', '500', '600', '700'],
  display: 'swap'
});

/** Font của vùng bài làm (soạn và đọc bài viết). */
const serif = Noto_Serif({
  variable: '--font-noto-serif',
  subsets: ['latin', 'vietnamese'],
  weight: ['400', '700'],
  display: 'swap'
});

const mono = JetBrains_Mono({
  variable: '--font-jetbrains-mono',
  subsets: ['latin'],
  weight: ['500', '600'],
  display: 'swap'
});

export const metadata: Metadata = {
  title: 'IELTS Writing — Giao bài & chấm bài',
  description: 'Hệ thống giao bài và chấm bài IELTS Writing trực tuyến'
};

export default function RootLayout({ children }: LayoutProps<'/'>) {
  return (
    <html lang="vi" className={`${sans.variable} ${serif.variable} ${mono.variable} h-full antialiased`}>
      <body className="min-h-full">
        <AntdRegistry layer>
          <Providers>{children}</Providers>
        </AntdRegistry>
      </body>
    </html>
  );
}
