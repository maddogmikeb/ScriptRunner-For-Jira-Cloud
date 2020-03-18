def script = "https://raw.githubusercontent.com/SMExDigital/ScriptRunner-For-Jira-Cloud/master/WorkPac/Script%20Listeners/check-for-confluence-link.groovy"

this.metaClass.mixin (new GroovyScriptEngine( '.' ).with {
    logger.info("Loading script from ${script}")
    loadScriptByName(script)
})

def uni = new Expando(get : Unirest.&get, post : Unirest.&post, put : Unirest.&put, patch : Unirest.&patch, delete : Unirest.&delete )
run(uni, logger, issue_event_type_name, issue)