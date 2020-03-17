// ********************************
// This groovy script keeps releases from the master project in sync with the tech projects.
// Note: It has a dependency on the Release Board plugin.
//
// Created By: Mike Burns
// Last Updated By: Mike Burns
//*********************************

logger.info("Event info: Scheduled Job - Release Board Sync")

def formatDate(d) {
    if (d == null) return null
    def dp = d.toString().substring(0, d.toString().length()-2) + "20" + d.toString().reverse().take(2).reverse()
    def date = new Date().parse("dd/MMM/yyyy", dp)
    return date.format("dd/MM/yyyy")
}

def projectCategories = ((List<Map>) Unirest.get("/rest/api/2/projectCategory")
    .header('Content-Type', 'application/json')
    .asObject(List)
    .body)
    .collect {
        [
            "id": it.id.toString(),
            "name": it.name.toString()?: "",
        ]}

def masterCategoryId = (projectCategories.find { (it as Map).name == 'Technology Portfolio' } as Map).id

def masterProjectKey = ((Map)get("/rest/api/2/project/search?orderBy=category&expand=projectKeys&categoryId=${masterCategoryId}")
    .header('Content-Type', 'application/json')
    .asObject(Map)
    .body)
    .values
    .collect { it.key.toString() }[0]

logger.info("masterProjectKey=${masterProjectKey}")

def squadCategoryId = (projectCategories.find { (it as Map).name == 'Technology Squad' } as Map).id

def targetProjectCodes = ((Map)get("/rest/api/2/project/search?orderBy=category&expand=projectKeys&categoryId=${squadCategoryId}")
    .header('Content-Type', 'application/json')
    .asObject(Map)
    .body)
    .values
    .collect { it.key.toString() }

logger.info("targetProjectCodes=${targetProjectCodes}")

def sourceVersions = (List<Map<String, Map>>) Unirest.get("/rest/api/3/project/${masterProjectKey}/versions")
    .asObject(List)
    .body

def sourceRBConfig = Unirest.get("/rest/api/2/project/${masterProjectKey}/properties/release-board-config")
    .asObject(Map)
    .body

if (sourceRBConfig == null) {
    sourceRBConfig = [:]
}

def sourceRBVersions = Unirest.get("/rest/api/2/project/${masterProjectKey}/properties/release-board-versions")
    .asObject(Map)
    .body

if (sourceRBVersions == null) {
    sourceRBVersions = [:]
}

def sourceMappedVersions = []

sourceRBVersions.value.each { version ->
    def releaseBoardColumn = (Map) sourceRBConfig.value.columns.find { it.id == version.value.columnId }
    def realVersion = (Map) sourceVersions.find { it.id == version.value.id }
    if (realVersion != null) {
        sourceMappedVersions.push([
            id: realVersion.id,
            name: realVersion.name,
            releaseBoardColumnName: releaseBoardColumn.name
        ])
    } else {
        releaseBoardColumn = (Map) sourceRBConfig.value.columns.find { it.sequence == 1 }
        sourceMappedVersions.push([
            id: version.value.id,
            name: "", // the name doesn't come through if there hasn't been any changes to the board
            releaseBoardColumnName: releaseBoardColumn.name
        ])
    }
}

targetProjectCodes.each { targetProjectKey ->
    def targetVersions = (List<Map<String, Map>>) Unirest.get("/rest/api/3/project/${targetProjectKey}/versions")
        .asObject(List)
        .body

    logger.info("targetVersions: ${targetVersions}")
    logger.info("sourceVersions: ${sourceVersions}")

    def syncedVersions = sourceVersions.find { v ->
        return targetVersions.find { it.name == v.name } != null
    }

    if (syncedVersions == null) {
        throw new Error("Versions in ${targetProjectKey} are not in sync with ${masterProjectKey}. Cannot sync board configs.")
    }

    def targetRBConfig = Unirest.get("/rest/api/2/project/${targetProjectKey}/properties/release-board-config")
        .asObject(Map)
        .body

    if (targetRBConfig == null) {
        targetRBConfig = [:]
    }

    def targetRBVersions = ((Map<String, Map>) Unirest.get("/rest/api/2/project/${targetProjectKey}/properties/release-board-versions")
        .asObject(Map)
        .body)
        .value

    if (targetRBVersions == null) {
        targetRBVersions = [:]
    }

    def updatedBoard = [:]

    targetVersions.each { v ->
        def sourceVersion = (Map) sourceMappedVersions.find { it.name == v.name }
        if (sourceVersion != null) {
            def targetColumn = (Map) targetRBConfig.value.columns.find { it.name == sourceVersion.releaseBoardColumnName }
            updatedBoard.put ("${v.id}", [
                id: v.id,
                columnId: targetColumn.id
            ])
        }
    }

    def boardsAreEqual = updatedBoard.every { k, v ->
        return v == targetRBVersions[k]
    }

    if (boardsAreEqual) {
        logger.info("Boards ${masterProjectKey} & ${targetProjectKey} are in sync.")
    } else {
        logger.info("Updated ${targetProjectKey} board details: ${updatedBoard}")

        def result = Unirest.put("/rest/api/3/project/${targetProjectKey}/properties/release-board-versions")
            .header("Content-Type", "application/json")
            .body(updatedBoard)
            .asString()
        assert result.status >= 200 && result.status < 300
        logger.info("Updated.")
    }
}

logger.info("Event info: Scheduled Job - Release Board Sync - Completed")