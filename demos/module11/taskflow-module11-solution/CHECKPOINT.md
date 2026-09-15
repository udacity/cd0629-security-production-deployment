# Checkpoint: taskflow-module11-solution

Git tag: `module-11-solution`

This is Module 11's completed solution — an explicit CORS allowlist (localhost:3000 only, never a wildcard) and the security headers Spring Security doesn't already provide by default: Content-Security-Policy, Referrer-Policy, the three cross-origin isolation headers (COOP/COEP/CORP), and Permissions-Policy. Note: X-Frame-Options and X-Content-Type-Options are already Spring Security defaults even before this module - they're written explicitly here so they can't be silently lost, but expect no visible change in DevTools from those two specifically. HSTS won't appear in local HTTP testing at all, it only sends over an actual secure connection.

This project is tracked with git internally, tagged at every module
boundary (module-3-solution, module-5-demo-starter, module-5-solution,
module-7-demo-starter, module-7-solution, module-9-demo-starter,
module-9-solution). If you need a different module's starter or
solution state later, ask and it can be exported precisely from the
matching tag rather than reconstructed from memory.
