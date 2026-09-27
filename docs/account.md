# Optional account (login / register)

- Sign-in is optional. It is the last onboarding step (page 5); «بعداً، شروع کنیم» skips it and finishes onboarding. Signing in there also finishes onboarding.
- Users can sign in later from the Telegram-style profile header at the top of the drawer (avatar, name, email, streak; «ورود / ثبت‌نام» when signed out). Tapping it opens Profile.
- Email login/register call `POST /api/auth/login` and `POST /api/auth/register` on `https://api.adhkar.ir`.
- The JWT and basic profile are stored in the `account` shared preferences (`AccountRepository`). All app content stays offline and works without an account.

## Profile screen

The email login/register card (or, when signed in, the account-details card with logout), then the achievements banner last. There is no separate identity card; the drawer header shows the avatar.

## Email verification (5-digit code via Resend)

All app requests send `X-Nour-Client: android`. For these requests the API does not issue a token to an unverified email:

1. Register, or log in to an account whose email is not verified yet: the API emails a 5-digit code (branded «اذکار نور» template, sent through Resend) and returns `verification_required: true` (201 on register, 403 on login) with `retry_after`.
2. The app shows five code boxes (paste works; the 5th digit submits automatically). `POST /api/auth/verify-email` with `email`, `password`, `code` returns the token.
3. «ارسال دوباره کد» calls `POST /api/auth/resend-code` (60 s cooldown, shown as a countdown). «تغییر ایمیل» goes back to the form.

Codes expire after 10 minutes, are stored hashed, and lock after 5 wrong attempts (request a new code). The password is required with the code, so a guessed code alone never grants access. Already-verified accounts cannot be signed in through the verify endpoint. The website does not send the header, so web sign-in is unchanged.

## Google sign-in (removed from the app for now)

The backend keeps `POST /api/auth/google`, which verifies a Google ID token against `GOOGLE_CLIENT_IDS`. To bring it back in the app: add `androidx.credentials`, `credentials-play-services-auth`, and `googleid` (plus `androidx.fragment` >= 1.3.0, which release lint requires once play-services-auth is on the classpath), create OAuth Web and Android client ids for `ir.adhkar.app`, and call the endpoint with the ID token from Credential Manager.
