'use client';

import { useEffect, useMemo, useState } from 'react';
import { useRouter } from 'next/navigation';
import { api, friendlyError, getPrometheusUrl, getGrafanaUrl, OpsMetrics } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { useToast } from '@/lib/toast';
import { isAdmin } from '@/lib/rbac';
import { AdminSkeleton, SpinnerLabel } from '@/components/loaders';
import { AdminPageShell } from '@/components/AdminShell';
import { TopBar } from '@/components/TopBar';
import { HBarChart, VBarChart } from '@/components/AdminCharts';

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
  if (unit === 'ratio') return `${(v * 100).toFixed(1)}%`;
  if (unit === 'count') return Number.isInteger(v) ? String(v) : v.toFixed(1);
  return v.toFixed(2);
}

function toBarItems(
  data: Record<string, number> | undefined,
  unit: string,
  filter: string,
  scale?: (v: number) => number
) {
  const q = filter.trim().toLowerCase();
  return Object.entries(data || {})
    .filter(([k]) => !q || k.toLowerCase().includes(q))
    .map(([label, value]) => ({
      label: `${label} (${formatValue(unit, value)})`,
      value: scale ? scale(value) : value,
    }))
    .sort((a, b) => b.value - a.value);
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

    const built = lines
      .map((line, i) => {
        if (!line.points.length) return null;
        const d = line.points
          .map((p, idx) => `${idx === 0 ? 'M' : 'L'} ${x(p.t).toFixed(1)} ${y(p.v).toFixed(1)}`)
          .join(' ');
        return { d, color: LINE_COLORS[i % LINE_COLORS.length], label: line.label };
      })
      .filter(Boolean) as { d: string; color: string; label: string }[];

    const ticks = [0, 0.5, 1].map((f) => {
      const v = minV + (maxV - minV) * (1 - f);
      return { y: pad.t + innerH * f, label: formatValue(unit, v) };
    });

    return { paths: built, legend: built, yTicks: ticks };
  }, [lines, unit]);

  return (
    <div className="panel analytics-chart">
      <h3 className="chart-title">{title}</h3>
      {lines.every((l) => !l.points?.length) ? (
        <p className="muted">
          No time series from Prometheus for this panel yet. Confirm scrape targets are UP, wait one
          scrape cycle (~15s), then refresh. Healthy 4xx/5xx should draw as flat zero lines when
          targets are known.
        </p>
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

/**
 * Prometheus / JVM / latency infrastructure — separate from platform audit reports.
 */
export default function AdminInfrastructurePage() {
  const { user, token, loading } = useAuth();
  const router = useRouter();
  const { error, info } = useToast();
  const [hours, setHours] = useState<(typeof HOURS_OPTIONS)[number]>(6);
  const [metrics, setMetrics] = useState<OpsMetrics | null>(null);
  const [busy, setBusy] = useState(false);
  const [filterQ, setFilterQ] = useState('');
  const [appliedFilter, setAppliedFilter] = useState('');
  const prometheus = getPrometheusUrl();

  useEffect(() => {
    if (loading) return;
    if (!user || !isAdmin(user)) {
      info('Admin access required.');
      router.replace('/');
    }
  }, [user, loading, router, info]);

  const loadOps = async (h = hours) => {
    if (!token || !isAdmin(user)) return;
    setBusy(true);
    try {
      const m = await api<OpsMetrics>(`/api/v1/reports/ops-metrics?hours=${h}`, {}, token);
      setMetrics(m);
    } catch (e) {
      error(friendlyError(e, 'Could not load infrastructure metrics.'));
    } finally {
      setBusy(false);
    }
  };

  useEffect(() => {
    if (token && isAdmin(user)) loadOps(hours);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token, user?.id, hours]);

  const charts = useMemo(() => {
    if (!metrics) return [];
    const q = appliedFilter.trim().toLowerCase();
    return (metrics.charts || []).filter((c) => {
      if (!q) return true;
      if (c.title.toLowerCase().includes(q)) return true;
      return (c.lines || []).some((l) => l.label.toLowerCase().includes(q));
    }).map((c) => ({
      ...c,
      lines: (c.lines || []).filter((l) => !q || l.label.toLowerCase().includes(q) || c.title.toLowerCase().includes(q)),
    }));
  }, [metrics, appliedFilter]);

  if (loading || !user || !isAdmin(user)) {
    return <AdminSkeleton />;
  }

  const rateBars = toBarItems(metrics?.requestRateByService, 'req/s', appliedFilter, (v) => Math.max(v, 0));
  const latencyBars = toBarItems(
    metrics?.p95LatencySecondsByService,
    's',
    appliedFilter,
    (v) => (v < 1 ? v * 1000 : v)
  );
  const err4xx = toBarItems(metrics?.error4xxRateByService, 'req/s', appliedFilter);
  const err5xx = toBarItems(metrics?.error5xxRateByService, 'req/s', appliedFilter);
  const heapBars = toBarItems(metrics?.heapUsedBytesByService, 'bytes', appliedFilter, (v) => v / 1e6);
  const cpuBars = toBarItems(metrics?.cpuUsageByService, 'ratio', appliedFilter, (v) => v * 100);
  const threadBars = toBarItems(metrics?.threadsByService, 'count', appliedFilter);

  return (
    <AdminPageShell
      kicker="Infrastructure"
      title="Service metrics"
      blurb="Live Prometheus scrapes — separate from platform audit reports. Latency uses avg (sum/count)."
      trailing={(
        <div className="admin-head-actions">
          <a className="btn secondary" href={`${prometheus}/targets`} target="_blank" rel="noreferrer">
            Targets
          </a>
          <button type="button" className="btn secondary" disabled={busy} onClick={() => loadOps(hours)}>
            {busy ? <SpinnerLabel label="Refreshing…" /> : 'Refresh'}
          </button>
        </div>
      )}
    >
      <TopBar
        crumb="Infra"
        searchPlaceholder="Filter by service name…"
        defaultSearch={filterQ}
        onSearch={(value) => {
          setFilterQ(value);
          setAppliedFilter(value);
        }}
        filters={(
          <>
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
          </>
        )}
      />

      {metrics && !metrics.prometheusAvailable && (
        <div className="admin-card admin-card-warn">
          <p style={{ margin: 0 }}>
            Prometheus looks offline. Confirm targets at{' '}
            <a href={`${prometheus}/targets`} target="_blank" rel="noreferrer">/targets</a> are UP.
          </p>
        </div>
      )}

      {metrics && (
        <>
          <section className="report-section">
            <header className="report-section-head">
              <h2>Service snapshots</h2>
              <p className="muted">
                Window {hours}h · {new Date(metrics.generatedAt).toLocaleString()}
              </p>
            </header>
            <div className="analytics-charts analytics-charts-dense">
              <VBarChart title="Request rate" blurb="req/s by service" items={rateBars} />
              <HBarChart title="Avg latency" blurb="Scaled for ranking (ms when sub-second)." items={latencyBars} />
              <VBarChart title="4xx rate" blurb="Client error rate (req/s)" items={err4xx} />
              <VBarChart title="5xx rate" blurb="Server error rate (req/s)" items={err5xx} />
              <HBarChart title="JVM heap (MB scale)" blurb="Heap used for ranking." items={heapBars} />
              <HBarChart title="CPU usage" blurb="Percent scale for comparison." items={cpuBars} />
              <HBarChart title="Live threads" blurb="Thread count by service." items={threadBars} />
            </div>
          </section>

          <section className="report-section">
            <header className="report-section-head">
              <h2>Time series</h2>
              <p className="muted">Prometheus range queries for the selected window.</p>
            </header>
            <div className="analytics-charts">
              {charts.map((c) => (
                <MetricLineChart key={c.id} title={c.title} unit={c.unit} lines={c.lines || []} />
              ))}
              {charts.length === 0 && (
                <p className="muted">No chart series match the filter.</p>
              )}
            </div>
          </section>
        </>
      )}
      {!metrics && busy && <AdminSkeleton />}
    </AdminPageShell>
  );
}
