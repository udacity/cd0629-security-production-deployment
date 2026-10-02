# Checkpoint: config-infrastructure

Two projects for Module 17:

- `config-server/` - a real, separate Spring Boot service running Spring
  Cloud Config Server (@EnableConfigServer), listening on port 8888.
- `taskflow-config-repo/` - a real git repository (not just a folder)
  backing the Config Server, containing `taskflow-api.yml` with
  `partner.request.timeout-seconds`.

**Required layout:** unzip so `config-server/` and `taskflow-config-repo/`
sit as SIBLING folders in the same parent directory - the Config
Server's application.yml expects `file://${user.dir}/../taskflow-config-repo`.

Used by `taskflow-module17-solution` - see that project's README for the
full setup and recording flow.
