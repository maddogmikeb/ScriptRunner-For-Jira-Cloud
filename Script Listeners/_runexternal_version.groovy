def script = "https://raw.githubusercontent.com/SMExDigital/ScriptRunner-For-Jira-Cloud/master/WorkPac/Script%20Listeners/version_sync.groovy"

this.metaClass.mixin (new GroovyScriptEngine( '.' ).with {
    logger.info("Loading script from ${script}")
    loadScriptByName(script)
})

def uni = new Expando(get : Unirest.&get, post : Unirest.&post, put : Unirest.&put, patch : Unirest.&patch, delete : Unirest.&delete )
run(uni, logger, webhookEvent, version)