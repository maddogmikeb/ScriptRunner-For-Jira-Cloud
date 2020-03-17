// ********************************
// This groovy script adds approvers based on priority.
//
// Created By: Mike Burns
// Last Updated By: Mike Burns
//*********************************

String.metaClass.encodeURL = {
    java.net.URLEncoder.encode(delegate, "UTF-8")
}

logger.info("Event -> ${issue_event_type_name}")

def issueKey = issue.key

def customFields = Unirest.get("/rest/api/2/field")
    .asObject(List)
    .body

def priorityValue = issue.priority?.name
def approversField = customFields.find { (it as Map).name == 'Change Approvers' } as Map

if (approversField != null && priorityValue != null && priorityValue != "Standard") {

    def newApprovers = []
    def changeGroup = "changegroup-${priorityValue}".encodeURL()
    def groupDetails = Unirest.get("/rest/api/2/group?expand=users&groupname=${changeGroup}")
        .asObject(Map)
        .body
    groupDetails?.users.items.each { Map grpuser ->
        if (user.accountId != grpuser.accountId) { // can't approve your own
            logger.info("Adding Approver -> ${grpuser.displayName} (${grpuser.accountId})")
            newApprovers.push(["id": grpuser.accountId])
        }
    }

    if (newApprovers.size() > 0) {
        logger.info("Updating approvers")
        def result = Unirest.put("/rest/api/2/issue/${issueKey}?notifyUsers=false")
            .header("Content-Type", "application/json")
            .body([
                fields: [
                    (approversField.id): newApprovers
                ],
            ])
            .asString()
        assert result.status >= 200 && result.status < 300
        if (result.status >= 200 && result.status < 300) {
            logger.info("Updated approvers to -> ${newApprovers}")
        } else {
            logger.error("Failed to change approvers to ${newApprovers}")
        }
    }
}

logger.info("Event -> ${issue_event_type_name} - Completed")