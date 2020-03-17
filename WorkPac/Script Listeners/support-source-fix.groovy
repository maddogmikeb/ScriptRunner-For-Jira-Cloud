// ********************************
// This groovy script updates the source
// based on channel - email, jira, api
//
// Created By: Mike Burns
// Last Updated By: Mike Burns
//*********************************

logger.info("Event -> ${issue_event_type_name}")

def issueKey = issue.key

def customFields = Unirest.get("/rest/api/2/field")
    .asObject(List)
    .body

def sourceField = customFields.find { (it as Map).name == 'Source' } as Map

if (sourceField != null) {
    def sourceValue = (issue.fields[sourceField.id] as Map)?.value
    if (sourceValue == null || sourceValue == "None") {
        sourceValue = "None";
        def tempIssue = Unirest.get("/rest/api/2/issue/${issueKey}?properties=*all")
                .header('Content-Type', 'application/json')
                .asObject(Map)
        if (tempIssue.status == 200) {
            def channel = tempIssue.body.properties["request.channel.type"]?.value;
            def source = "None"
            switch (channel) {
                case "email": source = "Email";  break;
                case "jira": source = "Phone";  break;
                case "portal": source = "Portal";  break;
            }
            logger.info("source = ${source}")
            if (source.toLowerCase() != sourceValue.toLowerCase()) {
                logger.info("Updating source -> ${sourceValue} to ${source}")
                def result = Unirest.put("/rest/api/2/issue/${issueKey}?notifyUsers=false")
                    .header("Content-Type", "application/json")
                    .body([
                        fields: [
                            (sourceField.id): [ value : source ]
                        ],
                    ])
                    .asString()
                if (result.status >= 200 && result.status < 300) {
                    logger.info("Updated source to -> ${source}")
                } else {
                    logger.info("Failed to change source to ${source}")
                }
            }
        }
    }
}

logger.info("Event -> ${issue_event_type_name} - Completed")