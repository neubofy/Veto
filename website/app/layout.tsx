import type { Metadata } from 'next';
import { Inter } from 'next/font/google';
import { Analytics } from '@vercel/analytics/next';
import './globals.css';
import SmoothScroll from './SmoothScroll';

const inter = Inter({ subsets: ['latin'] });

export const metadata: Metadata = {
  title: 'Veto - Next-Gen Android Security',
  description: 'Control, locate, and secure your Android device remotely with military-grade precision.',
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body className={inter.className}>
        <SmoothScroll>
          {children}
          <Analytics />
        </SmoothScroll>
      </body>
    </html>
  );
}
