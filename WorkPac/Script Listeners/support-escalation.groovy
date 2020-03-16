// ********************************
// This groovy script allows for escaltion to other teams based on adding a
// comment when the field changes (via workflow)
//
// Created By: Mike Burns
// Last Updated By: Mike Burns
//*********************************

def run(Unirest, logger, issue_event_type_name, issue) {
    String.metaClass.encodeURL = {
        java.net.URLEncoder.encode(delegate, "UTF-8")
    }

    logger.info("Event -> ${issue_event_type_name}")

    def issueKey = issue.key
    def defaultSupportTeam = "Level 1"

    def msteams = [
        //"Level 3 - Apps": "https://outlook.office.com/webhook/b7778a23-a9ba-46aa-bc8f-6dbff68a7651@c250ce71-f5b5-49c7-b52d-051f8ebf0cfa/IncomingWebhook/46bce73da6704014a2e65827115d6743/4faf1181-d8d4-44f0-b0ea-0e036088bfd9",
    ]

    def customFields = Unirest.get("/rest/api/2/field")
        .asObject(List)
        .body

    def customField = customFields.find { (it as Map).name == 'Support Team' } as Map

    if (customField == null) {
        logger.debug("Custom field not found")
        return
    }

    if (issue_event_type_name != "issue_created") {
        def teamChanged = changelog?.items.find { it['fieldId'] == customField.id }
        logger.info("Change item -> ${teamChanged}")
        if (teamChanged == null) {
            logger.info("Support team was not updated")
            return
        }
        if (teamChanged.fromString == null && teamChanged.toString == defaultSupportTeam) {
            logger.info("Support team was defaulting to ${defaultSupportTeam} no update required")
            return
        }
    }

    def supportTeamValue = (issue.fields[customField.id] as Map)?.value

    if (supportTeamValue == null) {
        logger.info("Support team has no value")
        return
    }

    if (issue_event_type_name == "issue_created" && supportTeamValue == defaultSupportTeam) {
        logger.info("Default support team selected")
        return
    }

    def webhookUrl = msteams[supportTeamValue]

    if (webhookUrl != null) {
        def hookresult = Unirest.post(webhookUrl)
            .header("Content-Type", "application/json")
            .body([
                "@context": "https://schema.org/extensions",
                "@type": "MessageCard",
                "themeColor": "066862",
                "title": "${item.key} has been escalted to " + supportTeamValue,
                "text": "${item.key} - ${item.fields.summary}\nReported by ${item.fields.reporter?.displayName}",
                "potentialAction": [
                    [
                        "@type": "OpenUri",
                        "name": "Open item in Jira",
                        "targets": [
                            [ "os": "default", "uri": "https://workpactech.atlassian.net/browse/${item.key}" ]
                        ]
                    ]
                ]
            ])
            .asString()
        if (hookresult.status >= 200 && hookresult.status < 300) {
            logger.info("Notification sent to ms teams")
        }
    }
    else
    {
        def comment = "'*** Automated message ***  \r\n This issue has been escalated to ${supportTeamValue}."

        try {
            def notificationGroup = "supportlevel-${supportTeamValue}"
            def groupDetails = Unirest.get("/rest/api/2/group?expand=users&groupname=${notificationGroup.encodeURL()}")
                .asObject(Map)
                .body
            if (groupDetails?.users.items.size() > 0) {
                comment += " FYI"
                groupDetails?.users.items.each { Map user ->
                    comment += " [${user.displayName}|~accountid:${user.accountId}] "
                }
            }
        } catch (Exception ex) {
            logger.warn("Unable to get support-level group details. ${ex}")
        }

        try {
            def result = Unirest.post("/rest/servicedeskapi/request/${issueKey}/comment?notifyUsers=false")
                .header("Content-Type", "application/json")
                .body([
                    public: false,
                    body: comment
                ])
                .asString()
            if (result.status >= 200 && result.status < 300) {
                logger.info("Created comment -> ${comment}")
            }
        } catch (Exception ex) {
            logger.warn("Cannot create - most likely the item is still being updated. ${ex}")
        }
    }

    logger.info("Event -> ${issue_event_type_name} - Completed")
}