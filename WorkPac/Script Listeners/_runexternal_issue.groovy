def script = "https://raw.githubusercontent.com/maddogmikeb/ScriptrunnerForJiraScripts/master/msteamsnotification.groovy"

this.metaClass.mixin (new GroovyScriptEngine( '.' ).with {
    logger.info("Loading script from ${script}")
    loadScriptByName(script)
})

def uni = new Expando(get : Unirest.&get, post : Unirest.&post, put : Unirest.&put, patch : Unirest.&patch, delete : Unirest.&delete )
run(uni, logger, issue_event_type_name, issue)