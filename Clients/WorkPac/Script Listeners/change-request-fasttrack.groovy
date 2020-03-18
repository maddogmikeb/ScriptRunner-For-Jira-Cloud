// ********************************
// This groovy script fast tracks low risk standard change requests.
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

def riskLevelField = customFields.find { (it as Map).name == 'Risk' } as Map
def requesttypeField = customFields.find { (it as Map).name == 'Customer Request Type' } as Map

if (riskLevelField != null && requesttypeField != null) {

    def riskLevelValue = (issue.fields[riskLevelField.id] as Map)?.value
    def requesttypeValue = (issue.fields[requesttypeField.id] as Map)?.requestType?.name
    def changeRequired =
                    !(
                        requesttypeValue == "Standard Change Request"
                        && ["Backlog", "To Do"].contains(issue.fields.status.name)
                        && ["Low", "Medium"].contains(riskLevelValue)
                    )

    if (changeRequired) {
        logger.info("No transition required -> ${requesttypeValue}, ${issue.fields.status.name}, ${riskLevelValue}")
        logger.info("Event -> ${issue_event_type_name}")
        return
    }

    def trans = (Unirest.get("/rest/api/2/issue/${issueKey}/transitions")
        .asObject(Map)
        .body).transitions.find { (issue.fields.status.name == "Backlog" && it.to.name == "To Do") || (issue.fields.status.name == "To Do" && it.to.name == "Awaiting approval") }

    if (trans != null) {
        logger.info("Transition issue to -> ${trans}")
        def result = Unirest.post("/rest/api/2/issue/${issueKey}/transitions")
            .header("Content-Type", "application/json")
            .body([
                "transition": [
                    "id": trans.id
                ]
            ])
            .asString()
        assert result.status >= 200 && result.status < 300
        if (result.status >= 200 && result.status < 300) {
            logger.info("Transitioned to -> ${trans}")
        } else {
            logger.error("Failed to transition to ${trans}")
        }

        try {
            if (issue.fields.status.name == "Backlog") {
                def comment = Unirest.post("/rest/servicedeskapi/request/${issueKey}/comment?notifyUsers=false")
                    .header("Content-Type", "application/json")
                    .body([
                        public: true,
                        body: "Fast tracked low risk standard change request. Please ensure that all relevant documentation is attached."
                    ])
                    .asString()
                if (comment.status >= 200 && comment.status < 300) {
                    logger.info("Created comment")
                }
            }
        } catch (Exception ex) {
            logger.info("Unable to create comment")
        }
    }
}