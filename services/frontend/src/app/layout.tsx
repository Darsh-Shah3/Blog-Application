import type { Metadata } from 'next';
import Script from 'next/script';
import { Fraunces, DM_Sans } from 'next/font/google';
import './globals.css';
import { AuthProvider } from '@/lib/auth';
import { ToastProvider } from '@/lib/toast';
import { ThemeProvider } from '@/lib/theme';
import { SplashScreen } from '@/components/SplashScreen';
import { AppChrome } from '@/components/AppChrome';

const fraunces = Fraunces({
  subsets: ['latin'],
  variable: '--font-fraunces',
  display: 'swap',
});

const dmSans = DM_Sans({
  subsets: ['latin'],
  variable: '--font-dm-sans',
  display: 'swap',
});

export const metadata: Metadata = {
  title: 'Threadly — communities, posts, and follows',
  description: 'Threadly microservices blog platform by Darsh Shah',
  icons: {
    icon: '/brand-mark.svg',
    apple: '/brand-mark.svg',
  },
};

/** Runs before paint so the saved theme applies without a flash. */
const themeInitScript = `
(function(){
  try {
    var k = 'threadly-theme';
    var t = localStorage.getItem(k);
    if (t !== 'light' && t !== 'dark') {
      t = window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
    }
    document.documentElement.setAttribute('data-theme', t);
  } catch (e) {
    document.documentElement.setAttribute('data-theme', 'light');
  }
})();
`;

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" className={`${fraunces.variable} ${dmSans.variable}`} suppressHydrationWarning>
      <body>
        <Script id="threadly-theme-init" strategy="beforeInteractive">
          {themeInitScript}
        </Script>
        <ThemeProvider>
          <ToastProvider>
            <AuthProvider>
              <SplashScreen />
              <AppChrome>{children}</AppChrome>
            </AuthProvider>
          </ToastProvider>
        </ThemeProvider>
      </body>
    </html>
  );
}
