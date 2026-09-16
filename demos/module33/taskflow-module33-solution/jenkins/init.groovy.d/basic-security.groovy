#!groovy
// Module 29: matches the same "dev-mode convenience" pattern as
// Keycloak (start-dev, admin/admin) and Vault (dev-mode, a fixed root
// token) elsewhere in this project — a known, fixed admin/admin login,
// zero interactive setup wizard. Never do this for a real Jenkins
// instance; fine for a local course demo that gets torn down and
// rebuilt.
import jenkins.model.*
import hudson.security.*
import jenkins.install.*

def instance = Jenkins.getInstance()

def hudsonRealm = new HudsonPrivateSecurityRealm(false)
hudsonRealm.createAccount("admin", "admin")
instance.setSecurityRealm(hudsonRealm)

def strategy = new FullControlOnceLoggedInAuthorizationStrategy()
strategy.setAllowAnonymousRead(false)
instance.setAuthorizationStrategy(strategy)

instance.setInstallState(InstallState.INITIAL_SETUP_COMPLETED)
instance.save()
