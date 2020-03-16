// ********************************
// This groovy script adds watches from value stream leads groups
//
// Created By: Mike Burns
// Last Updated By: Mike Burns
//*********************************

def run(Unirest, logger, issue_event_type_name, issue) {
    def issueKey = issue.key

    def customField = Unirest.get("/rest/api/2/field")
        .asObject(List)
        .body
        .find { (it as Map).name == 'Value Stream' } as Map

    if (customField == null) {
        logger.error("Field not found")
        return
    }

    logger.info("Event -> ${issue_event_type_name}")

    if (issue_event_type_name != "issue_created") {
        def changed = changelog?.items.find { it['fieldId'] == customField.id }
        logger.info("Changed item -> ${changed}")
        if (changed == null) {
            logger.info("Field not updated")
            return
        }
    }

    def customfieldValues = (issue.fields[customField.id] as Map)

    if (customfieldValues == null) {
        logger.info("Field has no value")
        return
    }

    customfieldValues.flatten().each { Map item ->

        def customfieldValue = item.value

        if (customfieldValue == "Unsure" || customfieldValue == null) {
            return
        }

        def notificationGroup = "tech-valuestream-${customfieldValue.toLowerCase().replace(" ", "_").replace(",", "").replace("&", "")}".replace("__", "_")

        def groupDetails = Unirest.get("/rest/api/2/group?expand=users&groupname=${notificationGroup}")
            .asObject(Map)
            .body

        groupDetails?.users.items.each { Map user ->

            def result = Unirest.post("/rest/api/2/issue/${issueKey}/watchers")
                .header('Content-Type', 'application/json')
                .body("\"${user.accountId}\"")
                .asString()
            if (result.status >= 200 && result.status < 300) {
                logger.info("Watch added -> ${user.displayName}")
            } else {
                logger.error("Unable to add watch -> ${user.displayName}")
            }
        }
    }
}