'use client';

import { useEffect, useMemo, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { api, friendlyError, getPrometheusUrl, OpsMetrics } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { useToast } from '@/lib/toast';
import { isAdmin } from '@/lib/rbac';
import { AdminSkeleton, SpinnerLabel } from '@/components/loaders';

const HOURS_OPTIONS = [1, 6, 12, 24] as const;
const LINE_COLORS = ['#c45c26', '#2f6f4e', '#3d5a80', '#9a4018', '#5b8e7d', '#bc4749', '#6d597a', '#e07a45'];

function formatValue(unit: string, v: number): string {
  if (!Number.isFinite(v)) return '—';
  if (unit === 'bytes') {
    if (v >= 1e9) return `${(v / 1e9).toFixed(2)} GB`;
    if (v >= 1e6) return `${(v / 1e6).toFixed(1)} MB`;
    if (v >= 1e3) return `${(v / 1e3).toFixed(0)} KB`;
    return `${Math.round(v)} B`;
  }
  if (unit === 's') {
    if (v < 0.001) return `${(v * 1e6).toFixed(0)} µs`;
    if (v < 1) return `${(v * 1000).toFixed(1)} ms`;
    return `${v.toFixed(2)} s`;
  }
  if (unit === 'req/s') return v < 0.01 ? v.toFixed(4) : v.toFixed(2);
  return v.toFixed(2);
}

function MetricLineChart({
  title,
  unit,
  lines,
}: {
  title: string;
  unit: string;
  lines: { label: string; points: { t: number; v: number }[] }[];
}) {
  const width = 640;
  const height = 200;
  const pad = { l: 44, r: 12, t: 16, b: 28 };

  const { paths, legend, yTicks } = useMemo(() => {
    const all = lines.flatMap((l) => l.points.map((p) => p.v));
    const times = lines.flatMap((l) => l.points.map((p) => p.t));
    const minV = all.length ? Math.min(...all, 0) : 0;
    const maxV = all.length ? Math.max(...all, minV + 1e-9) : 1;
    const minT = times.length ? Math.min(...times) : 0;
    const maxT = times.length ? Math.max(...times) : 1;
    const innerW = width - pad.l - pad.r;
    const innerH = height - pad.t - pad.b;
    const x = (t: number) => pad.l + ((t - minT) / Math.max(maxT - minT, 1)) * innerW;
    const y = (v: number) => pad.t + innerH - ((v - minV) / Math.max(maxV - minV, 1e-12)) * innerH;

    const built = lines.map((line, i) => {
      if (!line.points.length) return null;
      const d = line.points
        .map((p, idx) => `${idx === 0 ? 'M' : 'L'} ${x(p.t).toFixed(1)} ${y(p.v).toFixed(1)}`)
        .join(' ');
      return { d, color: LINE_COLORS[i % LINE_COLORS.length], label: line.label };
    }).filter(Boolean) as { d: string; color: string; label: string }[];

    const ticks = [0, 0.5, 1].map((f) => {
      const v = minV + (maxV - minV) * (1 - f);
      return { y: pad.t + innerH * f, label: formatValue(unit, v) };
    });

    return { paths: built, legend: built, yTicks: ticks };
  }, [lines, unit]);

  return (
    <div className="panel analytics-chart">
      <h3 style={{ fontFamily: 'var(--font-display)', marginTop: 0, marginBottom: '0.5rem' }}>{title}</h3>
      {lines.every((l) => !l.points?.length) ? (
        <p className="muted">No samples in this window. Use the site a bit, then refresh.</p>
      ) : (
        <>
          <svg viewBox={`0 0 ${width} ${height}`} className="metrics-svg" role="img" aria-label={title}>
            {yTicks.map((t) => (
              <g key={t.y}>
                <line x1={pad.l} x2={width - pad.r} y1={t.y} y2={t.y} stroke="var(--line)" strokeWidth={1} />
                <text x={pad.l - 6} y={t.y + 4} textAnchor="end" fontSize={10} fill="var(--muted)">{t.label}</text>
              </g>
            ))}
            {paths.map((p) => (
              <path key={p.label} d={p.d} fill="none" stroke={p.color} strokeWidth={2} strokeLinejoin="round" />
            ))}
          </svg>
          <div className="metrics-legend">
            {legend.map((l) => (
              <span key={l.label} className="metrics-legend-item">
                <span className="metrics-swatch" style={{ background: l.color }} />
                {l.label}
              </span>
            ))}
          </div>
        </>
      )}
    </div>
  );
}

function StatGrid({ title, data, unit }: { title: string; data: Record<string, number>; unit: string }) {
  const entries = Object.entries(data || {}).sort((a, b) => b[1] - a[1]);
  return (
    <div className="panel">
      <h3 style={{ fontFamily: 'var(--font-display)', marginTop: 0 }}>{title}</h3>
      {entries.length === 0 ? (
        <p className="muted" style={{ marginBottom: 0 }}>No data yet.</p>
      ) : (
        <div className="report-grid">
          {entries.map(([k, v]) => (
            <div key={k}>
              <strong>{formatValue(unit, v)}</strong>
              <span className="muted"> {k}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default function AdminAnalyticsPage() {
  const { user, token, loading } = useAuth();
  const router = useRouter();
  const { error, info } = useToast();
  const [hours, setHours] = useState<(typeof HOURS_OPTIONS)[number]>(6);
  const [metrics, setMetrics] = useState<OpsMetrics | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (loading) return;
    if (!user || !isAdmin(user)) {
      info('Admin access required.');
      router.replace('/');
    }
  }, [user, loading, router, info]);

  const load = async (h = hours) => {
    if (!token || !isAdmin(user)) return;
    setBusy(true);
    try {
      const m = await api<OpsMetrics>(`/api/v1/reports/ops-metrics?hours=${h}`, {}, token);
      setMetrics(m);
    } catch (e) {
      error(friendlyError(e, 'Could not load ops metrics.'));
    } finally {
      setBusy(false);
    }
  };

  useEffect(() => {
    if (token && isAdmin(user)) load(hours);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token, user?.id, hours]);

  if (loading || !user || !isAdmin(user)) {
    return <AdminSkeleton />;
  }

  return (
    <div style={{ marginTop: '1.25rem' }} className="stack">
      <section className="hero" style={{ paddingTop: '1.5rem' }}>
        <h1>Analytics</h1>
        <p>
          Live service health from Prometheus — the same numbers Grafana uses, shown here so you never leave Threadly.
          Deep dashboards stay available at Grafana for ops.
        </p>
      </section>

      <div className="toolbar">
        <Link className="btn secondary" href="/admin">← Admin home</Link>
        {HOURS_OPTIONS.map((h) => (
          <button
            key={h}
            type="button"
            className={`chip ${hours === h ? 'active' : ''}`}
            onClick={() => setHours(h)}
          >
            {h}h
          </button>
        ))}
        <button type="button" className="btn secondary" disabled={busy} onClick={() => load(hours)}>
          {busy ? <SpinnerLabel label="Refreshing…" /> : 'Refresh'}
        </button>
      </div>

      {metrics && !metrics.prometheusAvailable && (
        <div className="panel" style={{ borderColor: 'var(--error-line)' }}>
          <p style={{ margin: 0 }}>
            Prometheus looks offline. Confirm the <code>prometheus</code> container is up, open{' '}
            <a href={`${getPrometheusUrl()}/targets`} target="_blank" rel="noreferrer">scrape targets</a>
            {' '}(jobs should be UP), and that audit-service can reach <code>http://prometheus:9090</code>.
          </p>
        </div>
      )}

      {metrics && metrics.prometheusAvailable && (
        <p className="muted" style={{ margin: '0 0 0.5rem' }}>
          Graphs use live Prometheus HTTP metrics. Empty 5xx is normal when services are healthy.
          Latency is average duration (sum/count) — Spring Boot does not export histogram buckets by default.
          Request-rate numbers include scrape/health traffic in quiet local stacks so you still see activity.
        </p>
      )}

      {metrics && (
        <>
          <p className="muted" style={{ margin: 0 }}>
            Snapshot {new Date(metrics.generatedAt).toLocaleString()} · window {hours}h
          </p>
          <div className="analytics-grid">
            <StatGrid title="Request rate (req/s)" data={metrics.requestRateByService} unit="req/s" />
            <StatGrid title="Avg latency" data={metrics.p95LatencySecondsByService} unit="s" />
            <StatGrid title="5xx rate (req/s)" data={metrics.error5xxRateByService} unit="req/s" />
            <StatGrid title="JVM heap used" data={metrics.heapUsedBytesByService} unit="bytes" />
          </div>
          <div className="analytics-charts">
            {(metrics.charts || []).map((c) => (
              <MetricLineChart key={c.id} title={c.title} unit={c.unit} lines={c.lines || []} />
            ))}
          </div>
        </>
      )}
      {!metrics && busy && <AdminSkeleton />}
    </div>
  );
}
