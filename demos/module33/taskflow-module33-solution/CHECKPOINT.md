# Checkpoint: taskflow-module33-solution

Git tag: `module-33-solution`

This is Module 33's completed solution - a new Transaction entity creates a genuine N+1 query opportunity (Account.getTransactions() lazy-loaded), fixed via @EntityGraph on a new findAllWithTransactions() repository method. Caffeine caching added to AccountService.searchByNameSafe(). HikariCP pool size made explicit. org.hibernate.SQL: DEBUG enabled so query counts are actually visible in console output - this is the real proof for the N+1 demo, not just narration. See README's Module 33 section for the full recording flow, including real jcmd/JFR profiling commands against the running process and a simple curl-loop load test (no Gatling/JMeter needed).
