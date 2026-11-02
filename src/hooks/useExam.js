import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { EXAM_DURATION_SECONDS, PASSING_PERCENTAGE, SUBJECTS } from '../data/constants';
import { QUESTION_BANK, QUESTIONS_PER_LEVEL, getSubjectLevels, shuffleQuestionsArray } from '../data/questionBank';
import { fetchResults, saveExamResult } from '../services/examService';

export function useExamHistory(userId) {
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const requestId = useRef(0);
  const refresh = useCallback(async () => {
    const currentRequest = ++requestId.current;
    if (!userId) { setResults([]); setLoading(false); return []; }
    setLoading(true);
    try {
      const { data, error } = await fetchResults(userId);
      if (error) console.error('تعذر تحميل النتائج:', error.message);
      if (currentRequest === requestId.current) setResults(data || []);
      return data || [];
    } finally {
      if (currentRequest === requestId.current) setLoading(false);
    }
  }, [userId]);
  useEffect(() => { void refresh(); }, [refresh]);
  return { results, setResults, loading, refresh };
}

export function useExam({ subjectId, levelParam = '1', user, onFinish }) {
  const subject = SUBJECTS[subjectId] ? subjectId : 'biology';
  const level = String(levelParam || '1');
  const questions = useMemo(() => {
    const pool = QUESTION_BANK[subject] || [];
    let selected = pool;
    if (level === 'random') selected = shuffleQuestionsArray(pool).slice(0, QUESTIONS_PER_LEVEL);
    else if (level !== 'all') selected = getSubjectLevels(subject).find((item) => item.id === level)?.questionsPool || [];
    return shuffleQuestionsArray(selected).map((question) => ({ ...question, options: shuffleQuestionsArray(question.options || []) }));
  }, [subject, level]);

  const [currentIndex, setCurrentIndex] = useState(0);
  const [answers, setAnswers] = useState({});
  const [timeLeft, setTimeLeft] = useState(EXAM_DURATION_SECONDS);
  const [status, setStatus] = useState('ready');
  const [result, setResult] = useState(null);
  const timerRef = useRef(null);
  const startTimeRef = useRef(null);
  const answersRef = useRef(answers);
  const submittedRef = useRef(false);
  const submitRef = useRef(null);

  useEffect(() => { answersRef.current = answers; }, [answers]);
  useEffect(() => () => clearInterval(timerRef.current), []);

  const submitExam = useCallback(async () => {
    if (submittedRef.current || !questions.length) return null;
    submittedRef.current = true;
    clearInterval(timerRef.current);
    const currentAnswers = answersRef.current;
    const details = questions.map((question) => {
      const userAnswer = currentAnswers[question.id] || null;
      return { questionId: question.id, question: question.question, userAnswer, correctAnswer: question.correctKey, isCorrect: userAnswer === question.correctKey, options: question.options };
    });
    const score = details.filter((answer) => answer.isCorrect).length;
    const total = questions.length;
    const examResult = { subject, level, score, total, percentage: Math.round((score / total) * 100), passed: (score / total) * 100 >= PASSING_PERCENTAGE, timeTaken: startTimeRef.current ? Math.min(EXAM_DURATION_SECONDS, Math.round((Date.now() - startTimeRef.current) / 1000)) : 0, answers: details, completedAt: new Date().toISOString() };
    setResult(examResult);
    setStatus('finished');
    if (user?.id) {
      const saved = await saveExamResult({ userId: user.id, subject, level, score, total });
      if (saved.error) examResult.saveError = saved.error.message;
      else examResult.id = saved.data?.id;
    }
    setResult({ ...examResult });
    await onFinish?.(examResult);
    return examResult;
  }, [questions, subject, level, user?.id, onFinish]);
  submitRef.current = submitExam;

  const startExam = useCallback(() => {
    if (!questions.length) return;
    submittedRef.current = false;
    startTimeRef.current = Date.now();
    setTimeLeft(EXAM_DURATION_SECONDS);
    setStatus('in_progress');
  }, [questions.length]);

  useEffect(() => {
    if (status !== 'in_progress') return undefined;
    const deadline = Date.now() + timeLeft * 1000;
    timerRef.current = setInterval(() => {
      const remaining = Math.max(0, Math.ceil((deadline - Date.now()) / 1000));
      setTimeLeft(remaining);
      if (remaining === 0) void submitRef.current?.();
    }, 250);
    return () => clearInterval(timerRef.current);
  }, [status]);

  const answerQuestion = useCallback((questionId, selectedKey) => setAnswers((current) => ({ ...current, [questionId]: selectedKey })), []);
  const nextQuestion = useCallback(() => setCurrentIndex((index) => Math.min(index + 1, questions.length - 1)), [questions.length]);
  const prevQuestion = useCallback(() => setCurrentIndex((index) => Math.max(0, index - 1)), []);
  const currentQuestion = questions[currentIndex] || null;
  const answeredCount = Object.keys(answers).length;
  const formatTime = `${String(Math.floor(timeLeft / 60)).padStart(2, '0')}:${String(timeLeft % 60).padStart(2, '0')}`;
  const progress = questions.length ? ((currentIndex + 1) / questions.length) * 100 : 0;

  return { questions, currentIndex, currentQuestion, answers, timeLeft, formatTime, status, result, startExam, answerQuestion, nextQuestion, prevQuestion, submitExam, progress, answeredCount };
}
