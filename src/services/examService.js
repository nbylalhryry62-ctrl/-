import { supabase } from '../lib/supabaseClient';

const RESULT_COLUMNS = 'id,user_id,subject,level,score,total,percentage,created_at';

export async function fetchResults(userId) {
  if (!supabase || !userId) return { data: [], error: null };
  const { data, error } = await supabase
    .from('results')
    .select(RESULT_COLUMNS)
    .eq('user_id', userId)
    .order('created_at', { ascending: false });
  return { data: data || [], error };
}

export async function saveExamResult({ userId, subject, level, score, total }) {
  if (!supabase || !userId) return { data: null, error: new Error('لا توجد جلسة مستخدم صالحة.') };
  const percentage = total ? Math.round((score / total) * 100) : 0;
  const { data, error } = await supabase
    .from('results')
    .insert({ user_id: userId, subject, level: String(level || '1'), score, total, percentage })
    .select(RESULT_COLUMNS)
    .single();
  return { data, error };
}
