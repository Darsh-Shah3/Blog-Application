import type { Metadata } from 'next';
import { AboutCtas } from '@/components/AboutCtas';

export const metadata: Metadata = {
  title: 'About — Threadly',
  description: 'What Threadly is for, and who built it — Darsh Shah',
};

const USES = [
  {
    title: 'Create communities',
    body: 'Spin up topic spaces, invite people to join, and keep discussions organized by interest.',
  },
  {
    title: 'Post with media',
    body: 'Share text, links, images, and files. Sort feeds by hot, new, or top — the way community sites should feel.',
  },
  {
    title: 'Discuss & vote',
    body: 'Nested comments and votes surface the best ideas. Moderators and admins can keep spaces healthy.',
  },
  {
    title: 'Follow creators',
    body: 'Follow people you trust. When they publish, you get inbox alerts — and optional email when SMTP is on.',
  },
  {
    title: 'Secure microservices',
    body: 'JWT auth, gateway rate limits, role-based access, audit trails, and Prometheus metrics under the hood.',
  },
  {
    title: 'Owner-friendly ops',
    body: 'Admin roles, platform reports, and analytics so you can see how the community is growing.',
  },
];

export default function AboutPage() {
  return (
    <div className="about-page">
      <section className="about-hero">
        <div className="about-hero-text">
          <p className="landing-kicker">About Threadly</p>
          <h1>What this is for — and who built it</h1>
          <p className="landing-lead">
            Threadly is a Reddit-style community platform for focused discussion: communities, posts,
            votes, follows, and notifications — delivered as a modern microservices stack with a calm,
            premium UI.
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

      <section className="about-section">
        <h2>Why Threadly exists</h2>
        <p className="muted about-prose">
          Most people want a place to share ideas without noise: join a community, post something real,
          talk it through, and come back when people they follow publish more. Threadly proves that
          experience can be shipped as production-style microservices — API gateway, JWT security,
          database-per-service Postgres, media uploads, audit history, and observability — while the
          product still feels simple.
        </p>
      </section>

      <section className="about-section">
        <h2>What you can do here</h2>
        <div className="about-use-grid">
          {USES.map((u) => (
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
            Threadly is designed and developed by <strong>Darsh Shah</strong> as a portfolio-grade,
            interview-ready microservices project — from Spring Boot services and Docker Compose to a
            polished Next.js client. The goal: show full-stack system design with clean APIs, security,
            and a product people actually understand.
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
        <h2 style={{ marginTop: 0 }}>Ready to explore?</h2>
        <p className="muted">
          Create an account, join a community, or read more posts — built to demo cleanly in a few minutes.
        </p>
        <AboutCtas variant="footer" />
      </section>
    </div>
  );
}
