import { ShieldCheck, X } from 'lucide-react';

export default function SqlModal({ onClose }) {
  return <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/60 p-4" role="dialog" aria-modal="true" aria-labelledby="db-modal-title">
    <section className="w-full max-w-lg rounded-2xl bg-white p-6 shadow-2xl">
      <header className="mb-4 flex items-center justify-between"><h2 id="db-modal-title" className="flex items-center gap-2 text-lg font-extrabold"><ShieldCheck className="h-5 w-5 text-emerald-600" />حالة اتصال قاعدة البيانات</h2><button type="button" onClick={onClose} aria-label="إغلاق"><X className="h-5 w-5" /></button></header>
      <div className="space-y-3 text-sm leading-7 text-slate-700"><p className="rounded-xl bg-emerald-50 p-3 text-emerald-900">يستخدم التطبيق Supabase Auth وجداول <code>profiles</code> و<code>results</code> مع سياسات RLS.</p><p>يُستخدم المفتاح العام فقط في الواجهة. لا تضع <code>service_role</code> في ملفات الواجهة أو المستودع.</p><p className="rounded-xl bg-amber-50 p-3 text-amber-900">لا تعِد تنفيذ ملف SQL التأسيسي على قاعدة البيانات الحية. هذا العرض للتوثيق فقط ولا ينفذ أي استعلام.</p></div>
    </section>
  </div>;
}
