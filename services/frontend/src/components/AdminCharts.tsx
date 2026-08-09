'use client';

import { useMemo } from 'react';

const PALETTE = [
  'var(--accent)',
  'var(--accent-2)',
  'var(--accent-3)',
  'var(--accent-4)',
  'var(--up)',
  '#6d597a',
  '#bc4749',
  '#3d5a80',
];

export function VBarChart({
  title,
  blurb,
  items,
  emptyHint = 'No data in this window yet. Generate activity (posts, joins) so the audit trail fills.',
  fullWidth = false,
}: {
  title: string;
  blurb?: string;
  items: { label: string; value: number }[];
  emptyHint?: string;
  fullWidth?: boolean;
}) {
  const width = fullWidth ? 960 : 640;
  const height = 200;
  const pad = { l: 28, r: 12, t: 16, b: 36 };
  const max = Math.max(...items.map((i) => i.value), 1);
  const innerW = width - pad.l - pad.r;
  const innerH = height - pad.t - pad.b;
  const n = Math.max(items.length, 1);
  const slot = innerW / n;
  const barW = Math.min(slot * 0.72, fullWidth ? 36 : 28);

  return (
    <div className={`panel analytics-chart${fullWidth ? ' analytics-chart-span' : ''}`}>
      <h3 className="chart-title">{title}</h3>
      {blurb && <p className="muted analytics-chart-blurb">{blurb}</p>}
      {items.length === 0 || items.every((i) => i.value === 0) ? (
        <p className="muted" style={{ marginBottom: 0 }}>{emptyHint}</p>
      ) : (
        <svg viewBox={`0 0 ${width} ${height}`} className="metrics-svg" role="img" aria-label={title}>
          <line x1={pad.l} x2={width - pad.r} y1={pad.t + innerH} y2={pad.t + innerH} stroke="var(--line)" />
          {items.map((item, i) => {
            const h = (item.value / max) * innerH;
            const x = pad.l + i * slot + (slot - barW) / 2;
            const y = pad.t + innerH - h;
            const color = PALETTE[i % PALETTE.length];
            const showLabel = n <= 18 || i % 2 === 0 || i === n - 1;
            return (
              <g key={`${item.label}-${i}`}>
                <rect
                  x={x}
                  y={y}
                  width={barW}
                  height={Math.max(h, item.value > 0 ? 3 : 0)}
                  fill={color}
                  rx={3}
                  opacity={0.92}
                />
                {item.value > 0 && (
                  <text x={x + barW / 2} y={y - 4} textAnchor="middle" fontSize={9} fill="var(--muted)">
                    {item.value}
                  </text>
                )}
                {showLabel && (
                  <text x={x + barW / 2} y={height - 10} textAnchor="middle" fontSize={9} fill="var(--muted)">
                    {item.label.length > 8 ? `${item.label.slice(0, 7)}…` : item.label}
                  </text>
                )}
              </g>
            );
          })}
        </svg>
      )}
    </div>
  );
}

/** 24-hour radial / clock-style bars (UTC). */
export function HourRadialChart({
  title,
  blurb,
  hours,
}: {
  title: string;
  blurb?: string;
  hours: { hour: number; count: number }[];
}) {
  const size = 320;
  const cx = size / 2;
  const cy = size / 2;
  const maxR = 118;
  const minR = 28;
  const max = Math.max(...hours.map((h) => h.count), 1);

  return (
    <div className="panel analytics-chart">
      <h3 className="chart-title">{title}</h3>
      {blurb && <p className="muted analytics-chart-blurb">{blurb}</p>}
      {hours.every((h) => h.count === 0) ? (
        <p className="muted" style={{ marginBottom: 0 }}>
          No hourly peaks yet. Activity appears as wedges once audit events land.
        </p>
      ) : (
        <div className="hour-radial-wrap">
          <svg viewBox={`0 0 ${size} ${size}`} className="metrics-svg hour-radial" role="img" aria-label={title}>
            <circle cx={cx} cy={cy} r={maxR} fill="none" stroke="var(--line)" strokeWidth={1} opacity={0.6} />
            <circle cx={cx} cy={cy} r={minR} fill="var(--panel)" stroke="var(--line)" />
            {hours.map((h) => {
              const start = ((h.hour - 6) / 24) * Math.PI * 2 - Math.PI / 2;
              const sweep = (1 / 24) * Math.PI * 2 * 0.82;
              const r = minR + (h.count / max) * (maxR - minR);
              const x1 = cx + minR * Math.cos(start);
              const y1 = cy + minR * Math.sin(start);
              const x2 = cx + r * Math.cos(start);
              const y2 = cy + r * Math.sin(start);
              const x3 = cx + r * Math.cos(start + sweep);
              const y3 = cy + r * Math.sin(start + sweep);
              const x4 = cx + minR * Math.cos(start + sweep);
              const y4 = cy + minR * Math.sin(start + sweep);
              const large = sweep > Math.PI ? 1 : 0;
              const d = `M ${x1} ${y1} L ${x2} ${y2} A ${r} ${r} 0 ${large} 1 ${x3} ${y3} L ${x4} ${y4} A ${minR} ${minR} 0 ${large} 0 ${x1} ${y1} Z`;
              const color = h.count === 0
                ? 'color-mix(in srgb, var(--line) 55%, transparent)'
                : PALETTE[h.hour % PALETTE.length];
              return <path key={h.hour} d={d} fill={color} opacity={h.count === 0 ? 0.35 : 0.9} />;
            })}
            <text x={cx} y={cy + 4} textAnchor="middle" fontSize={11} fill="var(--muted)" fontWeight={600}>
              UTC
            </text>
          </svg>
          <div className="hour-radial-legend">
            {hours
              .filter((h) => h.count > 0)
              .sort((a, b) => b.count - a.count)
              .slice(0, 8)
              .map((h) => (
                <span key={h.hour}>
                  <strong>{String(h.hour).padStart(2, '0')}:00</strong> · {h.count}
                </span>
              ))}
          </div>
        </div>
      )}
    </div>
  );
}

export function HBarChart({
  title,
  blurb,
  items,
  emptyHint = 'No data yet.',
}: {
  title: string;
  blurb?: string;
  items: { label: string; value: number }[];
  emptyHint?: string;
}) {
  const max = Math.max(...items.map((i) => i.value), 1);
  return (
    <div className="panel analytics-chart">
      <h3 className="chart-title">{title}</h3>
      {blurb && <p className="muted analytics-chart-blurb">{blurb}</p>}
      {items.length === 0 || items.every((i) => i.value === 0) ? (
        <p className="muted" style={{ marginBottom: 0 }}>{emptyHint}</p>
      ) : (
        <ul className="report-bar-list">
          {items.map((item, i) => (
            <li key={item.label} className="report-bar-row">
              <div className="report-bar-meta">
                <span className="report-bar-label" title={item.label}>{item.label}</span>
                <span className="muted report-bar-value">{item.value.toLocaleString()}</span>
              </div>
              <div className="report-bar-track" aria-hidden="true">
                <div
                  className="report-bar-fill"
                  style={{
                    width: `${Math.max(3, (item.value / max) * 100)}%`,
                    background: PALETTE[i % PALETTE.length],
                  }}
                />
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

/** Simple donut from labeled counts. */
export function DonutChart({
  title,
  blurb,
  items,
}: {
  title: string;
  blurb?: string;
  items: { label: string; value: number }[];
}) {
  const total = items.reduce((s, i) => s + i.value, 0);
  const r = 64;
  const circumference = 2 * Math.PI * r;

  const arcs = useMemo(() => {
    if (total <= 0) return [] as { label: string; color: string; dash: string; offset: number; value: number }[];
    let off = 0;
    return items
      .filter((i) => i.value > 0)
      .map((item, i) => {
        const len = (item.value / total) * circumference;
        const a = {
          label: item.label,
          value: item.value,
          color: PALETTE[i % PALETTE.length],
          dash: `${len} ${circumference - len}`,
          offset: -off,
        };
        off += len;
        return a;
      });
  }, [items, total, circumference]);

  return (
    <div className="panel analytics-chart">
      <h3 className="chart-title">{title}</h3>
      {blurb && <p className="muted analytics-chart-blurb">{blurb}</p>}
      {total === 0 ? (
        <p className="muted" style={{ marginBottom: 0 }}>No breakdown yet for this window.</p>
      ) : (
        <div className="donut-wrap">
          <svg viewBox="0 0 160 160" className="donut-svg" role="img" aria-label={title}>
            <g transform="translate(80,80) rotate(-90)">
              {arcs.map((a) => (
                <circle
                  key={a.label}
                  r={r}
                  cx={0}
                  cy={0}
                  fill="none"
                  stroke={a.color}
                  strokeWidth={22}
                  strokeDasharray={a.dash}
                  strokeDashoffset={a.offset}
                  strokeLinecap="butt"
                />
              ))}
            </g>
            <text x="80" y="84" textAnchor="middle" fontSize="18" fontWeight="700" fill="var(--ink)">
              {total}
            </text>
            <text x="80" y="100" textAnchor="middle" fontSize="10" fill="var(--muted)">
              events
            </text>
          </svg>
          <ul className="donut-legend">
            {arcs.map((a) => (
              <li key={a.label}>
                <span className="metrics-swatch" style={{ background: a.color }} />
                {a.label}
                <strong>{a.value}</strong>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}

export function AreaLineChart({
  title,
  blurb,
  points,
  fullWidth = false,
}: {
  title: string;
  blurb?: string;
  points: { label: string; value: number }[];
  fullWidth?: boolean;
}) {
  const width = fullWidth ? 960 : 640;
  const height = 200;
  const pad = { l: 36, r: 12, t: 18, b: 32 };
  const max = Math.max(...points.map((p) => p.value), 1);
  const innerW = width - pad.l - pad.r;
  const innerH = height - pad.t - pad.b;

  const coords = points.map((p, i) => {
    const x = pad.l + (points.length <= 1 ? innerW / 2 : (i / (points.length - 1)) * innerW);
    const y = pad.t + innerH - (p.value / max) * innerH;
    return { x, y, ...p };
  });

  const lineD = coords.map((c, i) => `${i === 0 ? 'M' : 'L'} ${c.x.toFixed(1)} ${c.y.toFixed(1)}`).join(' ');
  const areaD = coords.length
    ? `${lineD} L ${coords[coords.length - 1]!.x} ${pad.t + innerH} L ${coords[0]!.x} ${pad.t + innerH} Z`
    : '';

  return (
    <div className={`panel analytics-chart${fullWidth ? ' analytics-chart-span' : ''}`}>
      <h3 className="chart-title">{title}</h3>
      {blurb && <p className="muted analytics-chart-blurb">{blurb}</p>}
      {points.every((p) => p.value === 0) ? (
        <p className="muted">No trend samples yet — activity builds the line as audits are recorded.</p>
      ) : (
        <svg viewBox={`0 0 ${width} ${height}`} className="metrics-svg" role="img" aria-label={title}>
          <defs>
            <linearGradient id="areaFill" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="var(--accent-2)" stopOpacity="0.35" />
              <stop offset="100%" stopColor="var(--accent-3)" stopOpacity="0.02" />
            </linearGradient>
          </defs>
          <line x1={pad.l} x2={width - pad.r} y1={pad.t + innerH} y2={pad.t + innerH} stroke="var(--line)" />
          <path d={areaD} fill="url(#areaFill)" />
          <path d={lineD} fill="none" stroke="var(--accent)" strokeWidth={2.5} strokeLinejoin="round" />
          {coords.map((c) => (
            <circle
              key={c.label}
              cx={c.x}
              cy={c.y}
              r={3.5}
              fill="var(--accent-2)"
              stroke="var(--panel)"
              strokeWidth={1.5}
            />
          ))}
          {coords.map((c, i) => {
            if (points.length > 14 && i % 2 !== 0 && i !== points.length - 1) return null;
            return (
              <text key={`t-${c.label}`} x={c.x} y={height - 10} textAnchor="middle" fontSize={9} fill="var(--muted)">
                {c.label.length > 5 ? c.label.slice(5) : c.label}
              </text>
            );
          })}
        </svg>
      )}
    </div>
  );
}

export function CumulativeAreaChart({
  title,
  blurb,
  days,
}: {
  title: string;
  blurb?: string;
  days: { day: string; count: number }[];
}) {
  let run = 0;
  const points = days.map((d) => {
    run += d.count;
    return { label: d.day, value: run };
  });
  return (
    <AreaLineChart
      title={title}
      blurb={blurb}
      points={points}
      fullWidth
    />
  );
}
