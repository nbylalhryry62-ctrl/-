import { supabase } from '../lib/supabaseClient';
import { ARABIC_NAME_REGEX } from '../data/constants';

export function translateAuthError(message = '') {
  const text = String(message).toLowerCase();
  if (text.includes('invalid login credentials') || text.includes('invalid_credentials')) return 'البريد أو كلمة المرور غير صحيحة';
  if (text.includes('already registered') || text.includes('already exists') || text.includes('duplicate')) return 'البريد مستخدم بالفعل';
  if (text.includes('password should be at least') || text.includes('weak_password')) return 'كلمة المرور قصيرة جدًا (6 أحرف على الأقل)';
  if (text.includes('invalid email')) return 'صيغة البريد الإلكتروني غير صحيحة';
  return message || 'حدث خطأ أثناء المصادقة، يرجى المحاولة مرة أخرى';
}

export async function emailAuth(mode, email, password) {
  if (!supabase) return { error: 'خدمة Supabase غير مهيأة. تحقق من متغيرات البيئة.' };
  const cleanEmail = String(email || '').trim().toLowerCase();
  if (!cleanEmail || !cleanEmail.includes('@')) return { error: translateAuthError('invalid email') };
  if (!password || password.length < 6) return { error: translateAuthError('password should be at least') };
  try {
    const response = mode === 'signup'
      ? await supabase.auth.signUp({ email: cleanEmail, password, options: { data: { full_name: '' } } })
      : await supabase.auth.signInWithPassword({ email: cleanEmail, password });
    if (response.error) return { error: translateAuthError(response.error.message) };
    return { data: response.data, user: response.data?.user || null };
  } catch (error) {
    return { error: translateAuthError(error?.message) };
  }
}

export async function googleAuth() {
  if (!supabase) return { error: 'خدمة Supabase غير مهيأة. تحقق من متغيرات البيئة.' };
  const { error } = await supabase.auth.signInWithOAuth({
    provider: 'google',
    options: { redirectTo: window.location.origin }
  });
  return error ? { error: translateAuthError(error.message) } : { success: true };
}

export async function loadProfile(user) {
  if (!supabase || !user) return null;
  const { data, error } = await supabase
    .from('profiles')
    .select('id,email,full_name,role,created_at,updated_at')
    .eq('id', user.id)
    .maybeSingle();
  if (error) throw error;
  return data || {
    id: user.id,
    email: user.email,
    full_name: user.user_metadata?.full_name || '',
    role: 'student'
  };
}

export async function saveProfile(userId, fullName) {
  const cleanName = String(fullName || '').trim().replace(/\s+/g, ' ');
  if (!ARABIC_NAME_REGEX.test(cleanName)) {
    return { error: 'يرجى إدخال الاسم الكامل بالعربية فقط (3 إلى 100 حرف).' };
  }
  if (!supabase || !userId) return { error: 'انتهت جلسة الدخول. سجّل الدخول مجددًا.' };
  const { data, error } = await supabase
    .from('profiles')
    .update({ full_name: cleanName, updated_at: new Date().toISOString() })
    .eq('id', userId)
    .select('id,email,full_name,role,created_at,updated_at')
    .single();
  return error ? { error: 'تعذر حفظ الملف الشخصي في قاعدة البيانات.' } : { data };
}

export async function signOut() {
  if (!supabase) return { error: 'خدمة Supabase غير مهيأة.' };
  const { error } = await supabase.auth.signOut();
  return error ? { error: translateAuthError(error.message) } : { success: true };
}
