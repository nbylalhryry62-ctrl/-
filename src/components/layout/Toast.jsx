import { useEffect } from 'react';
import { CheckCircle2, Info, XCircle } from 'lucide-react';

export default function Toast({ toast, onClose }) {
  useEffect(() => {
    if (!toast) return undefined;
    const timer = window.setTimeout(onClose, 3500);
    return () => window.clearTimeout(timer);
  }, [toast, onClose]);
  if (!toast) return null;
  const success = toast.type === 'success';
  const error = toast.type === 'error';
  const Icon = success ? CheckCircle2 : error ? XCircle : Info;
  return <div role="status" aria-live="polite" className={`fixed left-4 top-4 z-50 flex max-w-sm items-center gap-2 rounded-xl border px-4 py-3 text-sm font-semibold shadow-lg ${success ? 'border-emerald-700 bg-emerald-900 text-white' : error ? 'border-rose-700 bg-rose-900 text-white' : 'border-slate-700 bg-slate-900 text-white'}`}><Icon className="h-4 w-4 shrink-0" />{toast.message}</div>;
}
