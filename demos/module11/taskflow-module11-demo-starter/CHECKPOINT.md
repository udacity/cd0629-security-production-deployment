# Checkpoint: taskflow-module11-demo-starter

Git tag: `module-11-demo-starter`

This is Module 9's final solution state, plus the cors-frontend test pages (index.html, attacker.html) already in place. SecurityConfig.java has NO CORS configuration and NO explicit security headers yet. Important: the attacker.html iframe test will still show as BLOCKED even here — Spring Security ships X-Frame-Options: DENY and X-Content-Type-Options: nosniff as defaults, zero config needed. That's not a bug in this starter, that's real Spring Security behavior. The actual gap this module fixes is CORS (genuinely blocked here) and the headers Spring doesn't provide by default (CSP, Referrer-Policy, COOP/COEP/CORP, Permissions-Policy - genuinely absent here). See the README's Module 11 section for the full corrected recording flow.

This project is tracked with git internally, tagged at every module
boundary (module-3-solution, module-5-demo-starter, module-5-solution,
module-7-demo-starter, module-7-solution, module-9-demo-starter,
module-9-solution). If you need a different module's starter or
solution state later, ask and it can be exported precisely from the
matching tag rather than reconstructed from memory.
