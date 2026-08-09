'use client';

import { useEffect, useMemo, useState } from 'react';
import { useRouter } from 'next/navigation';
import { api, friendlyError, PlatformReport } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { useToast } from '@/lib/toast';
import { isAdmin } from '@/lib/rbac';
import { AdminSkeleton, SpinnerLabel } from '@/components/loaders';
import { AdminPageShell } from '@/components/AdminShell';
import { TopBar } from '@/components/TopBar';
import {
  AreaLineChart,
  CumulativeAreaChart,
  DonutChart,
  HBarChart,
  HourRadialChart,
  VBarChart,
} from '@/components/AdminCharts';

const REPORT_DAYS = [7, 14, 30] as const;

function matchesFilter(label: string, q: string) {
  if (!q.trim()) return true;
  return label.toLowerCase().includes(q.trim().toLowerCase());
}

/**
 * Admin platform reports only — audit / catalog data.
 * Infrastructure Prometheus views live on /admin/infrastructure.
 */
export default function AdminAnalyticsPage() {
  const { user, token, loading } = useAuth();
  const router = useRouter();
  const { error, info } = useToast();
  const [reportDays, setReportDays] = useState<(typeof REPORT_DAYS)[number]>(14);
  const [report, setReport] = useState<PlatformReport | null>(null);
  const [busy, setBusy] = useState(false);
  const [filterQ, setFilterQ] = useState('');
  const [appliedFilter, setAppliedFilter] = useState('');

  useEffect(() => {
    if (loading) return;
    if (!user || !isAdmin(user)) {
      info('Admin access required.');
      router.replace('/');
    }
  }, [user, loading, router, info]);

  const loadReport = async (d = reportDays) => {
    if (!token || !isAdmin(user)) return;
    setBusy(true);
    try {
      const r = await api<PlatformReport>(`/api/v1/reports/overview?days=${d}`, {}, token);
      setReport(r);
    } catch (e) {
      error(friendlyError(e, 'Could not load platform reports.'));
    } finally {
      setBusy(false);
    }
  };

  useEffect(() => {
    if (token && isAdmin(user)) loadReport(reportDays);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token, user?.id, reportDays]);

  const filtered = useMemo(() => {
    const q = appliedFilter;
    const dayBars = (report?.auditEventsPerDay || []).map((d) => ({
      label: d.day.slice(5),
      value: d.count,
    }));
    const dayPoints = (report?.auditEventsPerDay || []).map((d) => ({
      label: d.day,
      value: d.count,
    }));
    const actionBars = Object.entries(report?.actionsLast7d || {})
      .map(([label, value]) => ({ label, value }))
      .filter((i) => matchesFilter(i.label, q))
      .sort((a, b) => b.value - a.value);
    const serviceBars = Object.entries(report?.servicesLast7d || {})
      .map(([label, value]) => ({ label, value }))
      .filter((i) => matchesFilter(i.label, q))
      .sort((a, b) => b.value - a.value);
    const resourceBars = Object.entries(report?.resourceTypesLast7d || {})
      .map(([label, value]) => ({ label, value }))
      .filter((i) => matchesFilter(i.label, q))
      .sort((a, b) => b.value - a.value);
    const actorBars = (report?.topActorsLast7d || [])
      .map((a) => ({ label: a.name, value: a.count }))
      .filter((i) => matchesFilter(i.label, q));
    const resourceActionBars = (report?.resourceActionsLast7d || [])
      .map((r) => ({
        label: `${r.resourceType} · ${r.action}`,
        value: r.count,
      }))
      .filter((i) => matchesFilter(i.label, q))
      .sort((a, b) => b.value - a.value)
      .slice(0, 14);
    const hoursRaw = report?.activityByHourUtc || [];
    const hours: { hour: number; count: number }[] = Array.from({ length: 24 }, (_, hour) => {
      const found = hoursRaw.find((h) => h.hour === hour);
      return { hour, count: found?.count ?? 0 };
    });
    const hourColumns = hours.map((h) => ({
      label: String(h.hour).padStart(2, '0'),
      value: h.count,
    }));

    return {
      dayBars,
      dayPoints,
      actionBars,
      serviceBars,
      resourceBars,
      actorBars,
      resourceActionBars,
      hours,
      hourColumns,
    };
  }, [report, appliedFilter]);

  if (loading || !user || !isAdmin(user)) {
    return <AdminSkeleton />;
  }

  return (
    <AdminPageShell
      kicker="Platform reports"
      title="Reports & analytics"
      blurb="Audit trail and catalog counts only. Live service metrics live under Infra."
      trailing={(
        <button type="button" className="btn secondary" disabled={busy} onClick={() => loadReport(reportDays)}>
          {busy ? <SpinnerLabel label="Refreshing…" /> : 'Refresh'}
        </button>
      )}
    >
      <TopBar
        crumb="Reports"
        searchPlaceholder="Filter charts by action, service, actor…"
        defaultSearch={filterQ}
        onSearch={(value) => {
          setFilterQ(value);
          setAppliedFilter(value);
        }}
        filters={(
          <>
            {REPORT_DAYS.map((d) => (
              <button
                key={d}
                type="button"
                className={`chip ${reportDays === d ? 'active' : ''}`}
                onClick={() => setReportDays(d)}
              >
                {d}d
              </button>
            ))}
            {appliedFilter && (
              <button
                type="button"
                className="chip"
                onClick={() => {
                  setFilterQ('');
                  setAppliedFilter('');
                }}
              >
                Clear
              </button>
            )}
          </>
        )}
      />

      {report && (
        <>
          <section className="panel report-section">
            <header className="report-section-head">
              <h2>Catalog overview</h2>
              <p className="muted">Live totals from auth, community, post, and comment services.</p>
            </header>
            <div className="report-grid report-kpi">
              <div><strong>{report.activeUsers}</strong><span className="muted"> users</span></div>
              <div><strong>{report.totalPosts}</strong><span className="muted"> posts</span></div>
              <div><strong>{report.totalCommunities}</strong><span className="muted"> communities</span></div>
              <div><strong>{report.totalComments}</strong><span className="muted"> comments</span></div>
              <div><strong>{report.totalAuditEvents}</strong><span className="muted"> audit total</span></div>
              <div><strong>{report.auditEventsLast24h}</strong><span className="muted"> audit / 24h</span></div>
              <div><strong>{report.auditEventsLast7d}</strong><span className="muted"> audit / 7d</span></div>
            </div>
          </section>

          <section className="report-section">
            <header className="report-section-head">
              <h2>Activity over time</h2>
              <p className="muted">
                Multi-view trends for the last {report.reportDays || reportDays} days (audit volume).
              </p>
            </header>
            <div className="analytics-charts analytics-charts-dense">
              <VBarChart
                title="Events per day"
                blurb="Multi-color column view — one bar per UTC calendar day."
                items={filtered.dayBars}
                fullWidth
              />
              <AreaLineChart
                title="Daily volume (line)"
                blurb="Same series as a continuous line for growth shape."
                points={filtered.dayPoints}
              />
              <CumulativeAreaChart
                title="Cumulative audit total"
                blurb="Running sum across the selected window."
                days={report.auditEventsPerDay || []}
              />
            </div>
          </section>

          <section className="report-section">
            <header className="report-section-head">
              <h2>Activity by hour</h2>
              <p className="muted">
                Hour-of-day pattern (UTC) as a clock and full-height columns — not a flat zero list.
              </p>
            </header>
            <div className="analytics-charts">
              <HourRadialChart
                title="Peak hours (radial)"
                blurb="Longer wedges = busier UTC hours across the trend window."
                hours={filtered.hours}
              />
              <VBarChart
                title="Hour columns (UTC)"
                blurb="All 24 hours side by side — empty hours stay short."
                items={filtered.hourColumns}
              />
            </div>
          </section>

          <section className="report-section">
            <header className="report-section-head">
              <h2>Mutation breakdown (7d)</h2>
              <p className="muted">Who changed what — from service-published audit events.</p>
            </header>
            <div className="analytics-charts analytics-charts-dense">
              <DonutChart
                title="Actions mix"
                blurb="CREATE / UPDATE / DELETE share of audited mutations."
                items={filtered.actionBars}
              />
              <DonutChart
                title="Service mix"
                blurb="Which microservice wrote the most audit events."
                items={filtered.serviceBars}
              />
              <HBarChart
                title="Actions (ranking)"
                blurb="Horizontal ranking by count."
                items={filtered.actionBars}
              />
              <HBarChart
                title="By service"
                blurb="Microservice writers."
                items={filtered.serviceBars}
              />
              <VBarChart title="Resource types" blurb="POST, COMMUNITY, COMMENT, …" items={filtered.resourceBars} />
              <HBarChart
                title="Resource × action"
                blurb="Top combinations in the last 7 days."
                items={filtered.resourceActionBars}
              />
              <HBarChart
                title="Top actors"
                blurb="Most active usernames in the audit trail."
                items={filtered.actorBars}
              />
            </div>
          </section>
        </>
      )}
      {!report && busy && <AdminSkeleton />}
    </AdminPageShell>
  );
}
