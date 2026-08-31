import type { Metadata } from 'next';
import { Inter } from 'next/font/google';
import { Analytics } from '@vercel/analytics/next';
import Script from 'next/script';
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
        <Script id="zoho-salesiq-init" strategy="afterInteractive">
          {`window.$zoho=window.$zoho || {};$zoho.salesiq=$zoho.salesiq||{ready:function(){}}`}
        </Script>
        <Script
          id="zsiqscript"
          src="https://salesiq.zoho.in/widget?wc=siq9312488e4d8e0df500748f7a2fc1e8769385757ecd612b79f3d0afb943c616a1"
          strategy="afterInteractive"
          defer
        />
      </body>
    </html>
  );
}
