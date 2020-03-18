// ********************************
// This groovy script adds change managers as watchers.
//
// Created By: Mike Burns
// Last Updated By: Mike Burns
//*********************************

String.metaClass.encodeURL = {
    java.net.URLEncoder.encode(delegate, "UTF-8")
}

def issueKey = issue.key

def currentWatchers = (Unirest.get("/rest/api/latest/issue/${issueKey}/watchers")
    .header('Content-Type', 'application/json')
    .asObject(Map)
    .body).watchers?.collect({ it.accountId })

def watchers = []
def changeManagementGroup = "Change Management".encodeURL()
def changeManagers = Unirest.get("/rest/api/latest/group?expand=users&groupname=${changeManagementGroup}")
    .asObject(Map)
    .body
changeManagers?.users.items.each { Map u ->
    if (!currentWatchers.contains(u.accountId)) {
        logger.info("Adding Watcher -> ${u.displayName} (${u.accountId})")
        watchers.push(u.accountId)
    }
}

watchers.unique().each { String watcher ->
    try {
        def result = Unirest.post("/rest/api/latest/issue/${issueKey}/watchers")
            .header('Content-Type', 'application/json')
            .body("\"${watcher}\"")
            .asString()
        if (result.status >= 200 && result.status < 300) {
            logger.info("Watch added -> ${watcher}")
        } else {
            logger.warn("Unable to add watch -> ${watcher}")
        }
    } catch (Exception ex) {
        logger.warn("Unable to add watch -> ${watcher}")
    }
}