import type { Metadata } from 'next';
import Link from 'next/link';
import { AboutCtas } from '@/components/AboutCtas';

export const metadata: Metadata = {
  title: 'About — Threadly',
  description:
    'Threadly educational demo — communities, posts, chat, and microservices. For learning only, not production security.',
};

const FEATURES = [
  {
    title: 'Communities',
    body: 'Create topic spaces, join as a member, and keep conversations organized by interest.',
  },
  {
    title: 'Posts & media',
    body: 'Publish text, links, images, and files. Browse feeds by new, hot, or top.',
  },
  {
    title: 'Comments & votes',
    body: 'Nested comment threads and up/down votes surface what the community values.',
  },
  {
    title: 'Follows & alerts',
    body: 'Follow people you care about. Inbox notifications (and optional email) when they post.',
  },
  {
    title: 'Direct messages',
    body: 'Private 1:1 chats between members, with live updates and unread-style alerts.',
  },
  {
    title: 'Community live chat',
    body: 'Real-time room chat inside a community, saved for members who drop in later.',
  },
  {
    title: 'Profiles & photos',
    body: 'Public profiles with display name, bio, karma, and optional profile pictures.',
  },
  {
    title: 'Roles & admin tools',
    body: 'User, moderator, and admin roles; control center for roles, soft-delete, reports, and ops.',
  },
  {
    title: 'Security foundations',
    body: 'JWT auth, API gateway, rate limits, and soft-delete accounts — for study, not a production guarantee.',
  },
  {
    title: 'Observability',
    body: 'Microservice health, metrics, and audit events to show how real systems are operated.',
  },
];

export default function AboutPage() {
  return (
    <div className="about-page">
      <section className="about-hero">
        <div className="about-hero-text">
          <p className="landing-kicker">About Threadly</p>
          <h1>An educational community platform demo</h1>
          <p className="landing-lead">
            Threadly is a student / portfolio project that demonstrates how a Reddit-style site can be
            built with microservices: communities, posts, chat, follows, notifications, and admin tools —
            with a calm product UI on top.
          </p>
          <p className="about-edu-badge" role="note">
            <strong>For education and demonstration only.</strong>
            {' '}
            This is not a commercial product. Do not treat it as a production service or store anything you cannot afford to lose.
          </p>
          <AboutCtas variant="hero" />
        </div>
        <div className="about-hero-visual">
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img
            src="/brand-mark.svg"
            alt="Threadly brand mark"
            className="about-brand-image"
            width={160}
            height={160}
          />
        </div>
      </section>

      <section id="disclaimer" className="about-section about-disclaimer panel">
        <h2>Important disclaimer</h2>
        <ul className="about-disclaimer-list">
          <li>
            <strong>Educational purpose only.</strong>
            {' '}
            Threadly exists to teach and showcase software architecture, APIs, Docker deployment, and UI/UX —
            for interviews, coursework, and personal learning.
          </li>
          <li>
            <strong>Data may not stay secure.</strong>
            {' '}
            Credentials, posts, messages, files, and profile data may be stored on demo infrastructure that
            is not hardened like a bank or production SaaS.
          </li>
          <li>
            <strong>We are not responsible for data loss or privacy incidents.</strong>
            {' '}
            The owner and operators of this demo accept no liability for leaked, deleted, corrupted, or misused data.
            Accounts may be wiped when the demo environment is reset.
          </li>
          <li>
            <strong>Consent before signup.</strong>
            {' '}
            Creating an account requires accepting this disclaimer on the sign-up screen. By registering you
            confirm you understand the risks.
          </li>
        </ul>
        <p className="muted about-prose" style={{ marginTop: '0.85rem' }}>
          Prefer not to share personal details? Use a throwaway email and password unique to this demo —
          never reuse passwords from real accounts.
        </p>
      </section>

      <section className="about-section">
        <h2>What this website is for</h2>
        <p className="muted about-prose">
          Threadly is a full-stack “community platform in a box.” You explore how product features map onto
          real backend pieces: an API gateway, JWT authentication, separate PostgreSQL schemas per service,
          media uploads, WebSocket chat, notifications, and admin reporting. The goal is a system you can
          walk through end-to-end — from join community → post → comment → message — while still explaining
          the architecture underneath.
        </p>
      </section>

      <section className="about-section">
        <h2>Functionality included</h2>
        <p className="muted about-prose" style={{ marginBottom: '0.75rem' }}>
          Features available in this demo build:
        </p>
        <div className="about-use-grid">
          {FEATURES.map((u) => (
            <article key={u.title} className="about-use-card">
              <h3>{u.title}</h3>
              <p className="muted">{u.body}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="about-owner panel">
        <div className="about-owner-media">
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img
            src="/owner-avatar.svg"
            alt="Darsh Shah"
            className="about-owner-photo"
            width={120}
            height={120}
          />
        </div>
        <div className="about-owner-copy">
          <p className="about-owner-label">Owner & builder</p>
          <h2 className="about-owner-name">Darsh Shah</h2>
          <p className="muted about-prose">
            Designed and developed by <strong>Darsh Shah</strong> as a portfolio-grade microservices project —
            Spring Boot services, Docker Compose, gateway security, and a Next.js client. Built to show system
            design and product craft, not to run your confidential data in production.
          </p>
          <div className="toolbar" style={{ marginTop: '0.85rem' }}>
            <a
              className="btn secondary"
              href="https://github.com/Darsh-Shah3"
              target="_blank"
              rel="noreferrer"
            >
              GitHub — Darsh-Shah3
            </a>
            <a
              className="btn ghost"
              href="https://github.com/Darsh-Shah3/Blog-Application"
              target="_blank"
              rel="noreferrer"
            >
              Source repository
            </a>
          </div>
        </div>
      </section>

      <section className="panel landing-cta">
        <h2 style={{ marginTop: 0 }}>Try the demo</h2>
        <p className="muted">
          Ready to explore? You will be asked to accept the educational disclaimer before creating an account.
          {' '}
          <Link href="/about#disclaimer">Read the full notice</Link>
          {' '}
          above first if you prefer.
        </p>
        <AboutCtas variant="footer" />
      </section>
    </div>
  );
}
