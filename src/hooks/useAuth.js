import { useCallback, useEffect, useState } from 'react';
import { supabase } from '../lib/supabaseClient';
import { emailAuth, googleAuth, loadProfile, saveProfile as persistProfile, signOut as endSession } from '../services/authService';

export function useAuth() {
  const [user, setUser] = useState(null);
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);

  const refreshProfile = useCallback(async (currentUser = user) => {
    if (!currentUser) { setProfile(null); return null; }
    try {
      const value = await loadProfile(currentUser);
      setProfile(value);
      return value;
    } catch (error) {
      console.error('تعذر تحميل الملف الشخصي:', error?.message || error);
      const fallback = { id: currentUser.id, email: currentUser.email, full_name: currentUser.user_metadata?.full_name || '', role: 'student' };
      setProfile(fallback);
      return fallback;
    }
  }, [user]);

  useEffect(() => {
    if (!supabase) { setLoading(false); return undefined; }
    let active = true;
    const applySession = async (session) => {
      const nextUser = session?.user || null;
      if (!active) return;
      setUser(nextUser);
      if (!nextUser) {
        setProfile(null);
        setLoading(false);
        return;
      }
      try {
        const nextProfile = await loadProfile(nextUser);
        if (active) setProfile(nextProfile);
      } catch (error) {
        console.error('تعذر تحميل الملف الشخصي:', error?.message || error);
        if (active) setProfile({ id: nextUser.id, email: nextUser.email, full_name: nextUser.user_metadata?.full_name || '', role: 'student' });
      } finally {
        if (active) setLoading(false);
      }
    };
    supabase.auth.getSession().then(({ data, error }) => {
      if (error) console.error('تعذر استعادة الجلسة:', error.message);
      return applySession(data?.session);
    }).catch((error) => {
      console.error('تعذر استعادة الجلسة:', error);
      if (active) setLoading(false);
    });
    const { data: { subscription } } = supabase.auth.onAuthStateChange((_event, session) => {
      setTimeout(() => { if (active) void applySession(session); }, 0);
    });
    return () => { active = false; subscription.unsubscribe(); };
  }, []);

  const login = useCallback(async (mode, email, password) => {
    const response = await emailAuth(mode, email, password);
    const nextUser = response.data?.session?.user;
    if (!response.error && nextUser) {
      setUser(nextUser);
      try {
        setProfile(await loadProfile(nextUser));
      } catch {
        setProfile({ id: nextUser.id, email: nextUser.email, full_name: nextUser.user_metadata?.full_name || '', role: 'student' });
      }
      setLoading(false);
    }
    return response;
  }, []);
  const loginWithGoogle = useCallback(() => googleAuth(), []);
  const updateProfile = useCallback(async (fullName) => {
    if (!user) return { error: 'انتهت جلسة الدخول. سجّل الدخول مجددًا.' };
    const result = await persistProfile(user.id, fullName);
    if (result.data) setProfile(result.data);
    return result;
  }, [user]);
  const logout = useCallback(() => endSession(), []);

  return { user, profile, loading, login, loginWithGoogle, updateProfile, logout, refreshProfile };
}
