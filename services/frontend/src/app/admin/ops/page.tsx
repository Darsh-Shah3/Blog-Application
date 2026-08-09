import { redirect } from 'next/navigation';

/** Grafana admin page removed — keep bookmarks from 404. */
export default function AdminOpsRedirect() {
  redirect('/admin/infrastructure');
}
