import type { Metadata } from 'next';
import Script from 'next/script';
import { Fraunces, DM_Sans } from 'next/font/google';
import './globals.css';
import { AuthProvider } from '@/lib/auth';
import { AuthorDirectoryProvider } from '@/lib/authorDirectory';
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

/** Runs before paint so the saved theme applies without a flash. Dark by default. */
const themeInitScript = `
(function(){
  try {
    var k = 'threadly-theme';
    var t = localStorage.getItem(k);
    if (t !== 'light' && t !== 'dark') t = 'dark';
    document.documentElement.setAttribute('data-theme', t);
  } catch (e) {
    document.documentElement.setAttribute('data-theme', 'dark');
  }
})();
`;

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" className={`${fraunces.variable} ${dmSans.variable}`} data-theme="dark" suppressHydrationWarning>
      <body>
        <Script id="threadly-theme-init" strategy="beforeInteractive">
          {themeInitScript}
        </Script>
        <ThemeProvider>
          <ToastProvider>
            <AuthProvider>
              <AuthorDirectoryProvider>
                <SplashScreen />
                <AppChrome>{children}</AppChrome>
              </AuthorDirectoryProvider>
            </AuthProvider>
          </ToastProvider>
        </ThemeProvider>
      </body>
    </html>
  );
}
