# Deployment and operational notes

## Cloudflare Pages

- Production branch: `main`
- Build command: `npm run build`
- Build output directory: `dist`
- Node.js: `22`
- Required environment variable names:
  - `VITE_SUPABASE_URL`
  - `VITE_SUPABASE_ANON_KEY`

Do not put a Supabase service-role key or any other secret in Vite environment variables.

## Supabase

- Project reference: `arlunvwmuhrknvctjuew`
- The live project already has its own schema and row-level security policies.
- `supabase/schema.sql` is for new installations only. Do not run it against the live project without a reviewed migration.
- The current exam UI imports its 1,142-question bank from `src/data/questionBank.js`. The current frontend does not require rows in `public.questions` to display those practice questions.

## Current application limitations

- The current exam flow chooses questions and calculates the score in the browser, then stores a result summary in Supabase. This is suitable for practice and demonstration, but it is not tamper-resistant enough for official admissions or other high-stakes examinations.
- The current admin route is a starter screen. It does not yet expose user, question, or result management, even though guarded administrative RPC functions exist in the database.
- Before using the application for official admissions, move question delivery, timing enforcement, answer validation, scoring, and result finalization to trusted server-side database functions; then test with non-admin and admin accounts.

## Smoke-test checklist

1. Sign in with a student account and complete the Arabic profile.
2. Open each subject and confirm that questions are visible.
3. Complete a practice attempt and verify that its summary appears in the same user's results history.
4. Sign in with the designated administrator account and confirm the admin route is visible.
5. Verify that a student cannot read another user's profile or results and cannot call administrative RPC functions successfully.
