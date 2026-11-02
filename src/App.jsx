import { useCallback, useEffect, useMemo, useState } from 'react';
import { useAuth } from './hooks/useAuth';
import { useExamHistory } from './hooks/useExam';
import Header from './components/layout/Header';
import Toast from './components/layout/Toast';
import SqlModal from './components/layout/SqlModal';
import LoginScreen from './components/auth/LoginScreen';
import ProfileScreen from './components/auth/ProfileScreen';
import DashboardScreen from './components/dashboard/DashboardScreen';
import SubjectLevelsScreen from './components/dashboard/SubjectLevelsScreen';
import SyllabusScreen from './components/dashboard/SyllabusScreen';
import ResultsHistoryScreen from './components/dashboard/ResultsHistoryScreen';
import AdminDashboard from './components/admin/AdminDashboard';
import ExamScreen from './components/exam/ExamScreen';
import ResultScreen from './components/exam/ResultScreen';

function parseRoute(hash) {
  const [path, query = ''] = String(hash || '#/login').replace(/^#/, '').split('?');
  const params = Object.fromEntries(new URLSearchParams(query));
  if (path.startsWith('/levels/')) return { name: 'levels', subjectId: path.slice(8), params };
  if (path.startsWith('/exam/')) return { name: 'exam', subjectId: path.slice(6), params };
  const name = path.replace(/^\//, '') || 'login';
  return { name: ['login', 'profile', 'dashboard', 'syllabus', 'result', 'results', 'admin'].includes(name) ? name : 'login', params };
}

export default function App() {
  const [hash, setHash] = useState(() => window.location.hash || '#/login');
  const [toast, setToast] = useState(null);
  const [sqlOpen, setSqlOpen] = useState(false);
  const [currentResult, setCurrentResult] = useState(null);
  const { user, profile, loading: authLoading, login, loginWithGoogle, logout } = useAuth();
  const { results, loading: resultsLoading, refresh: refreshResults } = useExamHistory(user?.id);
  const route = useMemo(() => parseRoute(hash), [hash]);
  const username = String(user?.email || profile?.email || '').split('@')[0] || 'مستخدم';
  const navigate = useCallback((next) => { window.location.hash = next; }, []);
  const closeToast = useCallback(() => setToast(null), []);
  const notify = useCallback((message, type = 'info') => setToast({ message, type, id: Date.now() }), []);

  useEffect(() => {
    const onHashChange = () => setHash(window.location.hash || '#/login');
    window.addEventListener('hashchange', onHashChange);
    return () => window.removeEventListener('hashchange', onHashChange);
  }, []);
  useEffect(() => {
    if (authLoading) return;
    if (!user && route.name !== 'login') navigate('#/login');
    else if (user && route.name === 'login') navigate('#/dashboard');
  }, [authLoading, user, route.name, navigate]);

  const handleEmailAuth = useCallback(async ({ mode, email, password }) => {
    const response = await login(mode, email, password);
    if (response.error) return response;
    if (mode === 'signup' && !response.data?.session) notify('تم إنشاء الحساب. تحقق من بريدك لتأكيده ثم سجّل الدخول.', 'success');
    else { notify(mode === 'signup' ? 'تم إنشاء الحساب.' : 'تم تسجيل الدخول بنجاح.', 'success'); navigate('#/dashboard'); }
    return response;
  }, [login, navigate, notify]);
  const handleGoogleAuth = useCallback(async () => {
    const response = await loginWithGoogle();
    if (response.error) notify(response.error, 'error');
    return response;
  }, [loginWithGoogle, notify]);
  const handleLogout = useCallback(async () => {
    const response = await logout();
    if (response.error) notify(response.error, 'error');
    else { notify('تم تسجيل الخروج.', 'info'); navigate('#/login'); }
  }, [logout, navigate, notify]);
  const handleExamFinish = useCallback(async (result) => {
    setCurrentResult(result);
    if (result.saveError) notify('انتهى الاختبار، لكن تعذر حفظ النتيجة في قاعدة البيانات.', 'error');
    else { notify('تم حفظ نتيجة الاختبار.', 'success'); await refreshResults(); }
    navigate(`#/result?subject=${encodeURIComponent(result.subject)}&level=${encodeURIComponent(result.level)}&score=${result.score}&total=${result.total}`);
  }, [navigate, notify, refreshResults]);
  const openLevels = useCallback((subject) => navigate(`#/levels/${subject}`), [navigate]);
  const startExam = useCallback((subject, level = '1') => navigate(`#/exam/${subject}?level=${encodeURIComponent(level)}`), [navigate]);
  const retryExam = useCallback((subject, level) => startExam(subject, level || '1'), [startExam]);

  if (authLoading) return <div className="grid min-h-screen place-items-center text-slate-600">جارٍ التحقق من الجلسة…</div>;
  return <div dir="rtl" lang="ar" className="min-h-screen bg-slate-50 text-slate-900">
    <Toast toast={toast} onClose={closeToast} />
    {user && route.name !== 'exam' && <Header profile={profile} username={username} activeRoute={route.name} resultCount={results.length} onNavigate={navigate} onLogout={handleLogout} onOpenSql={() => setSqlOpen(true)} />}
    {route.name === 'login' && <LoginScreen onEmailAuth={handleEmailAuth} onGoogleOAuth={handleGoogleAuth} onOpenSqlModal={() => setSqlOpen(true)} />}
    {route.name === 'profile' && user && <ProfileScreen user={user} username={username} onCancel={() => navigate('#/dashboard')} />}
    {route.name === 'dashboard' && user && <DashboardScreen username={username} results={results} onOpenLevels={openLevels} onStartRandom={(subject) => startExam(subject, 'random')} onOpenSyllabus={(subject) => navigate(subject ? `#/syllabus?subject=${subject}` : '#/syllabus')} onOpenResults={() => navigate('#/results')} />}
    {route.name === 'levels' && user && <SubjectLevelsScreen subjectId={route.subjectId} results={results} onBack={() => navigate('#/dashboard')} onSelectLevel={startExam} onOpenSyllabus={(subject) => navigate(`#/syllabus?subject=${subject}`)} />}
    {route.name === 'syllabus' && user && <SyllabusScreen initialSubject={route.params.subject} onBack={() => navigate('#/dashboard')} onOpenLevels={openLevels} />}
    {route.name === 'exam' && user && <ExamScreen key={`${route.subjectId}:${route.params.level || '1'}`} subjectId={route.subjectId} levelParam={route.params.level || '1'} user={user} onFinish={handleExamFinish} onNavigate={navigate} />}
    {route.name === 'result' && user && <ResultScreen result={currentResult} params={route.params} onBack={() => navigate('#/dashboard')} onRetry={retryExam} onHistory={() => navigate('#/results')} />}
    {route.name === 'results' && user && <ResultsHistoryScreen results={results} loading={resultsLoading} onRetry={retryExam} onDashboard={() => navigate('#/dashboard')} />}
    {route.name === 'admin' && user && profile?.role === 'admin' && <AdminDashboard username={username} results={results} onBack={() => navigate('#/dashboard')} />}
    {sqlOpen && <SqlModal onClose={() => setSqlOpen(false)} />}
  </div>;
}
