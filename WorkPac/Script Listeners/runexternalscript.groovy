def script = "https://raw.githubusercontent.com/maddogmikeb/ScriptrunnerForJiraScripts/master/msteamsnotification.groovy"

this.metaClass.mixin (new GroovyScriptEngine( '.' ).with {
    logger.info("Loading script from ${script}")
    loadScriptByName(script)
})

run(new Expando(get : Unirest.&get, post : Unirest.&post, put : Unirest.&put, patch : Unirest.&patch, delete : Unirest.&delete ), logger, issue_event_type_name, issue)