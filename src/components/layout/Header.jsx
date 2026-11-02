import { BookOpen, Database, GraduationCap, History, LogOut, User } from 'lucide-react';

export default function Header({ profile, username, activeRoute, resultCount, onNavigate, onLogout, onOpenSql }) {
  const links = [
    { route: 'dashboard', label: 'المواد', icon: BookOpen, hash: '#/dashboard' },
    { route: 'syllabus', label: 'المنهج', icon: GraduationCap, hash: '#/syllabus' },
    { route: 'results', label: 'النتائج', icon: History, hash: '#/results' }
  ];
  return <header className="sticky top-0 z-30 border-b border-slate-200 bg-white/95 backdrop-blur"><div className="mx-auto flex h-16 max-w-6xl items-center justify-between gap-2 px-3 sm:px-6">
    <button type="button" onClick={() => onNavigate('#/dashboard')} className="flex items-center gap-2 text-right"><span className="text-2xl">🎓</span><span><b className="block text-sm">اختبار القبول الطبي</b><small className="hidden text-slate-500 sm:block">جامعة عدن</small></span></button>
    <nav className="flex items-center gap-1">{links.map(({ route, label, icon: Icon, hash }) => <button key={route} type="button" onClick={() => onNavigate(hash)} className={`flex items-center gap-1 rounded-lg px-2 py-2 text-xs font-semibold sm:px-3 sm:text-sm ${activeRoute === route ? 'bg-indigo-50 text-indigo-700' : 'text-slate-600 hover:bg-slate-100'}`}><Icon className="h-4 w-4" /><span>{label}</span>{route === 'results' && resultCount > 0 && <span className="rounded-full bg-indigo-600 px-1.5 text-white">{resultCount}</span>}</button>)}
      <button type="button" title="حالة قاعدة البيانات" onClick={onOpenSql} className="rounded-lg p-2 text-slate-500 hover:bg-slate-100"><Database className="h-4 w-4" /></button>
      <button type="button" onClick={() => onNavigate('#/profile')} className="flex items-center gap-2 rounded-lg p-2 text-slate-600 hover:bg-slate-100" title={username || 'الملف الشخصي'} aria-label={username || 'الملف الشخصي'}><User className="h-4 w-4" /><span className="hidden max-w-28 truncate text-xs sm:inline">{username}</span></button>
      {profile?.role === 'admin' && <button type="button" onClick={() => onNavigate('#/admin')} className="rounded-lg px-2 py-2 text-xs font-semibold text-violet-700 hover:bg-violet-50">الإدارة</button>}
      <button type="button" onClick={onLogout} className="rounded-lg p-2 text-rose-600 hover:bg-rose-50" title="تسجيل الخروج"><LogOut className="h-4 w-4" /></button>
    </nav>
  </div></header>;
}
