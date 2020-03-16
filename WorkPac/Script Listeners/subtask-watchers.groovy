// ********************************
// This groovy script allocates the reporter of a sub-task as a watcher to the parent story
//
// Created By: Mike Burns
// Last Updated By: Mike Burns
//*********************************

def run(Unirest, logger, issue_event_type_name, issue) {
    // check should be done via conditional evaluation - this is a double check
    // condition = issue.issueType.name == "Sub-task"
    if (!issue.fields.issuetype.subtask) {
        return
    }

    def watchers = [user.accountId]

    if (issue.fields.reporter) {
        watchers.push(issue.fields.reporter.accountId)
    }
    if (issue.fields.assignee) {
        watchers.push(issue.fields.assignee.accountId)
    }

    def parentKey = issue.fields.parent.key

    watchers.unique().each { String watcher ->
        try {
            def result = Unirest.post("/rest/api/2/issue/${parentKey}/watchers")
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
}