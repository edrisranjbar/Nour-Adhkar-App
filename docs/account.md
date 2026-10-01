# Optional account (login / register)

- Sign-in is optional. It is the last onboarding step (page 5); «بعداً، شروع کنیم» skips it and finishes onboarding. Signing in there also finishes onboarding.
- Users can sign in later from the Telegram-style profile header at the top of the drawer (avatar, name, email, streak; «ورود / ثبت‌نام» when signed out). Tapping it opens Profile.
- Email login/register call `POST /api/auth/login` and `POST /api/auth/register` on `https://api.adhkar.ir`.
- The JWT and basic profile are stored in the `account` shared preferences (`AccountRepository`). All app content stays offline and works without an account.

## Google-only sign-in (temporary)

`EmailSignInEnabled` (in `AccountScreen.kt`) is `false`: users can sign in with **Google only**, or continue without an account. The login page shows «ورود با گوگل» and «ادامه بدون حساب» (opens Home) instead of «ورود با ایمیل», and the onboarding account page shows only the Google button. The email login/register, verification-code and password-reset flows below are unchanged and can be re-enabled by setting the switch to `true`. The backend endpoints stay available.

## Profile screen

- Signed out: the Profile destination shows a dedicated login page (`LoginScreen.kt`, app-bar title «ورود به حساب») instead of a form. It has the Nour illustration on a green gradient panel at the top, a short welcome, and two buttons at the bottom: «ورود با گوگل» and «ورود با ایمیل». Achievements stay reachable without an account through the trophy icon in the Home app bar.
- «ورود با ایمیل» opens the email step: login by default, with «حساب کاربری ندارید؟ ثبت‌نام کنید» / «قبلاً حساب ساخته‌اید؟ وارد شوید» to switch. Register adds a name field. Email and password are typed left-to-right; the keyboard's Next/Done moves between fields and submits. Errors appear in a banner above the button. The back arrow (and system back) returns to the welcome page; from the code step it returns to the form.
- Signed in: the account-details card with logout, then the achievements banner last. There is no separate identity card; the drawer header shows the avatar.
- The onboarding account page keeps the compact `AuthForm` (segmented login/register, then Google). Both UIs share `AuthController` for validation, API calls, Google sign-in and the code step.

## Email verification (5-digit code via Resend)

All app requests send `X-Nour-Client: android`. For these requests the API does not issue a token to an unverified email:

1. Register, or log in to an account whose email is not verified yet: the API emails a 5-digit code (branded «اذکار نور» template, sent through Resend) and returns `verification_required: true` (201 on register, 403 on login) with `retry_after`.
2. The app shows five code boxes (paste works; the 5th digit submits automatically). `POST /api/auth/verify-email` with `email`, `password`, `code` returns the token.
3. «ارسال دوباره کد» calls `POST /api/auth/resend-code` (60 s cooldown, shown as a countdown). «تغییر ایمیل» goes back to the form.

Codes expire after 10 minutes, are stored hashed, and lock after 5 wrong attempts (request a new code). The password is required with the code, so a guessed code alone never grants access. Already-verified accounts cannot be signed in through the verify endpoint. The website does not send the header, so web sign-in is unchanged.

## Password reset (5-digit code)

On the email login form, «فراموشی رمز عبور؟» opens the reset flow (`PasswordResetSteps.kt`):

1. Enter the account email → `POST /api/auth/password-reset/request`. The API emails a 5-digit code (same branded template, reset wording) and answers the same way whether or not the email is registered.
2. Enter the code and a new password (at least 6 characters) → `POST /api/auth/password-reset/confirm`. The password is changed, the email is marked verified (the code proves ownership), and the user is signed in with a toast.

Same rules as verification codes: 10-minute expiry, hashed storage, 5 wrong attempts lock the code, 60 s resend cooldown (countdown on «ارسال دوباره کد»). Reset codes live in their own table, so they never overwrite a pending verification code. The website keeps its link-based reset.

## Google sign-in

The login page (and the onboarding form) has a standard "Sign in with Google" button (unmodified four-colour G, white/dark neutral surface, 1dp outline, pill shape). It uses Credential Manager (`GetSignInWithGoogleOption`) to get an ID token and posts it to `POST /api/auth/google`; the API verifies it with Google (`aud` must be in `GOOGLE_CLIENT_IDS`) and signs in or creates the user as already email-verified, so no code step is needed. Closing Google's chooser shows nothing; no Google account on the device, network, or server problems show friendly Persian messages.

### One-time setup (required for the button to work)

1. Google Cloud Console → APIs & Services → Credentials (configure the OAuth consent screen first).
2. Create an OAuth client of type **Web application**. Copy its client id.
3. Create an OAuth client of type **Android**: package `ir.adhkar.app`, SHA-1 of the release signing certificate `A0:2B:BE:E0:EE:1E:EB:A7:7E:3E:6F:07:85:1E:8C:DD:13:D6:02:DB`. (For debug builds add another Android client for `ir.adhkar.app.debug` with the debug keystore SHA-1.) If the app is distributed through a store that re-signs it, also add that store's signing SHA-1.
4. App: add `googleWebClientId=<web client id>` to `local.properties` (or set `GOOGLE_WEB_CLIENT_ID`) and rebuild. Without it the button explains that Google sign-in is not enabled yet.
5. Backend `.env`: `GOOGLE_CLIENT_IDS=<web client id>`, then deploy. Without it the API answers 503 and the app shows a friendly message.

## Backup privacy

Account progress backup/sync runs automatically after sign-in. Profile shows sync status and retry without an enable/disable setting. See [progress-sync.md](progress-sync.md) for stored data, merge behavior, retries and privacy boundaries.

Account credentials and the per-installation inbox identity are excluded from Android cloud backup and device transfer. Restored installations require sign-in again and receive a new inbox installation ID; ordinary app updates preserve both. Progress and other preferences retain their existing backup behavior.
