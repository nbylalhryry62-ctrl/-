export default function ProfileScreen({ user, username, onCancel }) {
  const email = user?.email || '';

  return <main className="mx-auto flex w-full max-w-xl flex-1 items-center px-4 py-10"><section className="w-full rounded-3xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
    <h1 className="text-2xl font-extrabold">بيانات الحساب</h1>
    <p className="mb-6 mt-2 text-sm text-slate-500">بيانات حسابك المسجل في بوابة اختبار القبول الطبي.</p>
    <div className="space-y-4">
      <div className="rounded-xl border border-slate-200 p-3">
        <span className="block text-sm font-semibold text-slate-600">اسم المستخدم</span>
        <b className="mt-1 block text-left" dir="ltr">{username || 'مستخدم'}</b>
      </div>
      <label className="block text-sm font-semibold">البريد الإلكتروني<input value={email} disabled dir="ltr" className="mt-1 w-full rounded-xl border bg-slate-100 p-3 text-left" /></label>
      <p className="text-xs leading-6 text-slate-500">اسم المستخدم هو الجزء الذي يسبق علامة @ في بريدك الإلكتروني. الاسم الكامل غير مطلوب لاستخدام التطبيق.</p>
      <button type="button" onClick={onCancel} className="w-full rounded-xl bg-indigo-600 p-3 font-bold text-white">العودة إلى المواد</button>
    </div>
  </section></main>;
}
