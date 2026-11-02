import { useState } from 'react';
import { AlertCircle, FileCode } from 'lucide-react';

export default function LoginScreen({ onEmailAuth, onGoogleOAuth, onOpenSqlModal }) {
  const [mode, setMode] = useState('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const submit = async (event) => {
    event.preventDefault(); setError(''); setLoading(true);
    const result = await onEmailAuth({ mode, email, password });
    setLoading(false); if (result?.error) setError(result.error);
  };
  const google = async () => {
    setError(''); setLoading(true); const result = await onGoogleOAuth(); setLoading(false); if (result?.error) setError(result.error);
  };
  return (
    <main className="min-h-screen flex items-center justify-center p-4 bg-gradient-to-br from-indigo-600 to-purple-700">
      <section className="w-full max-w-md rounded-3xl bg-white p-6 sm:p-8 shadow-2xl">
        <header className="mb-6 text-center">
          <div className="mx-auto mb-4 flex h-20 w-20 items-center justify-center rounded-full bg-indigo-600 text-4xl text-white">🎓</div>
          <h1 className="text-2xl font-extrabold">اختبار القبول</h1>
          <p className="mt-1 text-sm text-slate-500">كلية الطب والعلوم الصحية — جامعة عدن</p>
        </header>
        <button type="button" onClick={google} disabled={loading} className="mb-4 w-full rounded-xl border border-slate-300 py-3 font-semibold hover:bg-slate-50 disabled:opacity-60">الدخول بحساب Google</button>
        <div className="mb-4 text-center text-xs text-slate-400">أو بالبريد الإلكتروني</div>
        {error && <div role="alert" className="mb-4 flex gap-2 rounded-xl border border-rose-200 bg-rose-50 p-3 text-sm text-rose-700"><AlertCircle className="h-5 w-5 shrink-0" />{error}</div>}
        <form onSubmit={submit} className="space-y-4">
          <label className="block text-sm font-semibold">البريد الإلكتروني<input type="email" required dir="ltr" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} className="mt-1 w-full rounded-xl border border-slate-300 p-3 text-left outline-none focus:border-indigo-600" /></label>
          <label className="block text-sm font-semibold">كلمة المرور<input type="password" required minLength={6} dir="ltr" autoComplete={mode === 'login' ? 'current-password' : 'new-password'} value={password} onChange={(e) => setPassword(e.target.value)} className="mt-1 w-full rounded-xl border border-slate-300 p-3 text-left outline-none focus:border-indigo-600" /></label>
          <button disabled={loading} className="w-full rounded-xl bg-indigo-600 py-3 font-bold text-white hover:bg-indigo-700 disabled:opacity-60">{loading ? 'جارٍ المعالجة…' : mode === 'login' ? 'تسجيل الدخول' : 'إنشاء حساب جديد'}</button>
        </form>
        <button type="button" onClick={() => { setMode(mode === 'login' ? 'signup' : 'login'); setError(''); }} className="mt-5 w-full text-sm font-semibold text-indigo-700 hover:underline">{mode === 'login' ? 'ليس لديك حساب؟ أنشئ حسابًا' : 'لديك حساب؟ سجّل الدخول'}</button>
        <footer className="mt-6 flex items-center justify-between border-t border-slate-100 pt-4 text-xs text-slate-500"><span>بوابة اختبار القبول الطبي</span><button type="button" onClick={onOpenSqlModal} className="flex items-center gap-1 text-indigo-600"><FileCode className="h-4 w-4" />اتصال قاعدة البيانات</button></footer>
      </section>
    </main>
  );
}
