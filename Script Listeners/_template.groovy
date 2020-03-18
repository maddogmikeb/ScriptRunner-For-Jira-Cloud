// ********************************
// This groovy script
//
// Created By:
// Last Updated By:
//*********************************

String.metaClass.encodeURL = {
    java.net.URLEncoder.encode(delegate, "UTF-8")
}

logger.info("Event -> ${issue_event_type_name}")

def customFields = Unirest.get("/rest/api/2/field")
    .asObject(List)
    .body

def priorityValue = issue.priority?.name
def approversField = customFields.find { (it as Map).name == 'Change Approvers' } as Map

logger.info("Event -> ${issue_event_type_name} - Completed")