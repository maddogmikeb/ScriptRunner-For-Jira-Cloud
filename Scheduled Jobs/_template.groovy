// ********************************
// This groovy script
//
// Created By:
// Last Updated By:
//*********************************

def event_name = "THIS IS NOT SUPPLIED FOR SCHEDULED JOBS"

logger.info("Event -> ${event_name}")

def query = 'JQL'

def searchReq = Unirest.get("/rest/api/2/search")
    .queryString("jql", query)
    .queryString("fields", "key")
    .asObject(Map)
assert searchReq.status == 200
Map searchResult = searchReq.body

searchResult.issues.each { Map issue ->
    logger.info("Commented on ${searchResult.issues.size()} issues")
}

logger.info("Iterated over ${searchResult.issues.size()} issues")

logger.info("Event -> ${event_name} - Completed")