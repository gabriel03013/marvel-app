disposition: ship

## verdict pass

1. Resolved — pending email operation Back. AuthScreens keeps Back enabled; ArchiveViewModel invalidates abandoned attempts, clears passwords, suppresses pending-task session exposure and signs out late successful authentication. EmailAuthFlowTest clicks Back immediately during valid login, asserts welcome plus no Firebase session after settlement, then verifies the same account can retry. This passes on phone, font-scale and tablet.
2. Resolved — verification gap. Final confirmation logs for phone, font-scale and tablet each report OK (1 test); final font captures include expected wrong-password feedback and reset confirmation. Earlier intermittent failures remain without a definitive root cause; no stronger claim is made.

Remaining: clear for the two scored findings. No regression observed in sampled final error/reset/validation captures. This ship verdict covers the scoped email-auth review and scored fixes, not production provider configuration, reset-email delivery or a new full-app audit. Final evidence: /tmp/archive-auth-confirm-captures, /tmp/archive-auth-confirm-phone.log, /tmp/archive-auth-confirm-font.log and /tmp/archive-auth-confirm-tablet.log. The original review below is retained as history.

## persistence

Pass. PRODUCT.md and SCREENS.md record the explicitly requested email signup, sign-in and recovery extension. DESIGN.md remains the visual authority. Native XML extension; no comp or browser detector applies. Phone/tablet integration passes are reported by the parent; the first font-scale run's wrong-password assertion remains under investigation.

## fidelity

| Element | Assessment |
| --- | --- |
| TYPE | Match: bundled condensed display and readable interface typography. |
| MATERIAL | Match: incumbent collage welcome, quiet readable form surfaces. |
| GROUND | Match: archive black and paper cream from DESIGN.md. |
| Auth choices | Match: Google, email sign-in and account creation are reachable. |
| Validation/recovery | Match: visible English errors, retained inputs, reset link confirmation and return to sign-in. |
| Phone/tablet form | Adaptation: scrollable phone form and centered bounded tablet measure support native sizes and larger text. |
| System Back during request | Contradicted: authLoading consumes Back and disables the form's Back action. |

## ceiling

The extension preserves the established visual world. Fields have labels, appropriate input/autofill types, password visibility controls, scalable text and adequate targets. Native cancellation is the remaining material interaction gap; no unrelated polish is requested.

## material_fixes

1. P2, Android Back promise: ArchiveViewModel.kt:103 and AuthScreens.kt auth_back disable navigation while authentication loads. Allow Back to leave a pending email operation and invalidate its UI continuation, with protection against late Firebase completion navigating back into the flow. Do not rely on coroutine cancellation alone to cancel the underlying Firebase Task. Verify delayed signup/sign-in/reset Back behavior and successful retry.
2. Verification: resolve or explain the transient font-scale wrong-password assertion before claiming the full email integration pass; this review does not establish that failure's cause.

## keep

Keep real Firebase authentication, neutral password-reset confirmation, in-memory retry/rotation input retention, password clearing on departure/success, the existing collage welcome and readable cream form surfaces.

Evidence: reviewed AuthScreens.kt, screen_email_auth.xml, welcome auth actions, ArchiveViewModel email-auth methods, AuthErrors.kt and representative native welcome/signup/validation/error/reset/profile captures across phone, font-scale and tablet. Build and integration results were supplied by the parent, not rerun independently. Production provider configuration and reset-email delivery were not established by emulator captures.
