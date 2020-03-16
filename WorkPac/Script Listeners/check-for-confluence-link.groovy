// ********************************
// This groovy script sets the 'Has Confluence Link' if the issue is linked to a wiki page.
//
// Created By: Mike Burns
// Last Updated By: Mike Burns
//*********************************

def run(Unirest, logger, issue_event_type_name, issue) {
    logger.info("Event -> ${issue_event_type_name}")

    def issueKey = issue.key

    def remotelinks = (List) Unirest.get("/rest/api/2/issue/${issueKey}/remotelink")
        .asObject(List)
        .body

    def hasRemotelinks = false;
    if (remotelinks == null || remotelinks == []) {
        logger.info("No remote links")
    }
    else
    {
        remotelinks.each { Map link ->
            if (link.relationship == "Wiki Page") {
                hasRemotelinks = true
                return
            }
        }

        if (hasRemotelinks == false) {
            logger.info("No remote links to wiki pages")
        }
    }

    def customFields = Unirest.get("/rest/api/2/field")
        .asObject(List)
        .body

    def hasConfluenceLinkField = customFields.find { (it as Map).name == 'Has Confluence Link' } as Map
    def hasConfluenceLinkFieldValue = (issue.fields[hasConfluenceLinkField.id] as List)?.get(0)?.value

    if (!hasRemotelinks)  {
        if (hasConfluenceLinkFieldValue == null) {
            logger.info("No Update Required")
            return
        }

        logger.info("Removing Has Confluence Link")

        def result = Unirest.put("/rest/api/2/issue/${issueKey}?notifyUsers=false")
            .header("Content-Type", "application/json")
            .body([
                fields: [
                    (hasConfluenceLinkField.id): null
                ],
            ])
            .asString()
        assert result.status >= 200 && result.status < 300
        if (result.status >= 200 && result.status < 300) {
            logger.info("Updated Has Confluence Link")
        } else {
            logger.error("Failed to change Has Confluence Link")
        }
    }
    else
    {
        if (!(hasConfluenceLinkFieldValue == null || hasConfluenceLinkFieldValue != "Linked")) {
            logger.info("No Update Required")
            return
        }

        logger.info("Updating Has Confluence Link")

        def result = Unirest.put("/rest/api/2/issue/${issueKey}?notifyUsers=false")
            .header("Content-Type", "application/json")
            .body([
                fields: [
                    (hasConfluenceLinkField.id): [
                        [
                            "value" : "Linked"
                        ]
                    ]
                ],
            ])
            .asString()
        assert result.status >= 200 && result.status < 300
        if (result.status >= 200 && result.status < 300) {
            logger.info("Updated Has Confluence Link")
        } else {
            logger.error("Failed to change Has Confluence Link")
        }
    }
}