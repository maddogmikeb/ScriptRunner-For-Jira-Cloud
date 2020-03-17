// ********************************
// This groovy script keeps releases from the master project in sync with the tech projects.
// Note: It has a dependency on the Release Board plugin.
//
// Created By: Mike Burns
// Last Updated By: Mike Burns
//*********************************

def mappedVersion = (Map) version

logger.info("Event Version info: ${mappedVersion.toMapString()}")
logger.info("Event info: ${webhookEvent}")

def formatDate(d) {
    if (d == null) return null
    def dp = d.toString().substring(0, d.toString().length()-2) + "20" + d.toString().reverse().take(2).reverse()
    def date = new Date().parse("dd/MMM/yyyy", dp)
    return date.format("dd/MM/yyyy")
}

if (webhookEvent == "jira:version_updated") {
    // the update event does not include the latest information!!
    mappedVersion = (Map) Unirest.get("/rest/api/3/version/${mappedVersion.id}")
        .asObject(Map)
        .body

    logger.info("Source Version info: ${mappedVersion.toMapString()}")
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

def squadCategoryId = (projectCategories.find { (it as Map).name == 'Technology Squad' } as Map).id

def targetProjectCodes = ((Map)Unirest.get("/rest/api/2/project/search?orderBy=category&expand=projectKeys&categoryId=${squadCategoryId}")
    .header('Content-Type', 'application/json')
    .asObject(Map)
    .body)
    .values
    .collect { it.key.toString() }

logger.info("targetProjectCodes=${targetProjectCodes}")

def sourceVersions = (List<Map<String, Object>>) Unirest.get("/rest/api/3/project/${mappedVersion.projectId}/versions")
    .asObject(List)
    .body

def sourceRBConfig = (Map) Unirest.get("/rest/api/2/project/${mappedVersion.projectId}/properties/release-board-config")
    .asObject(Map)
    .body

if (sourceRBConfig == null) {
    sourceRBConfig = [:]
}

def sourceRBVersions = (Map) Unirest.get("/rest/api/2/project/${mappedVersion.projectId}/properties/release-board-versions")
    .asObject(Map)
    .body

logger.info("sourceRBVersions: ${sourceRBVersions}")

if (sourceRBVersions == null) {
    sourceRBVersions = [:]
} else {
    sourceRBVersions = sourceRBVersions.value
}

def sourceMappedVersions = []

sourceRBVersions.each { v ->
    def realVersion = (Map) sourceVersions.find { it.id == ((Map) v.value).id }
    if (realVersion == null) {
        return
    }
    def releaseBoardColumn = (Map) sourceRBConfig.value.columns.find { it.id == ((Map) v.value).columnId }
    if (releaseBoardColumn == null) {
        return
    }

    sourceMappedVersions.push([
        id: realVersion.id,
        name: realVersion.name,
        releaseBoardColumnName: releaseBoardColumn.name
    ])
}

targetProjectCodes.each { String projectCode ->

    def targetProject = Unirest.get("/rest/api/3/project/${projectCode}")
        .asObject(Map)
        .body

    logger.info("Target project: ${targetProject.key}")

    def targetVersions = (List<Map<String, Map>>) Unirest.get("/rest/api/3/project/${targetProject.key}/versions")
        .asObject(List)
        .body

    def v = targetVersions.find { it["description"] == "Links to Version Id: " + version["id"] || it["name"] == version["name"] }

    if (webhookEvent == "jira:version_created") {
        if (v != null) {
            logger.error("Version already exists.")
        } else {
            logger.info("New version will be created.")
            def newVersion = [
                description: "Links to Version Id: " + version.id,
                name: version.name,
                archived: version.archived.toBoolean(),
                released: version.released.toBoolean(),
                userStartDate: formatDate(version.userStartDate),
                userReleaseDate: formatDate(version.userReleaseDate),
                projectId: targetProject.id
            ]
            logger.info("Creating: ${newVersion.toMapString()}")
            def result = Unirest.post("/rest/api/3/version/")
                .header("Content-Type", "application/json")
                .body(newVersion)
                .asString()
            assert result.status >= 200 && result.status < 300
            logger.info("Created.")
        }
    } else if (webhookEvent == "jira:version_deleted") {
        if (v != null) {
            logger.info("Version will be deleted: ${v.toMapString()}")
            def result = Unirest.post("/rest/api/3/version/${v.id}/removeAndSwap")
                .header("Content-Type", "application/json")
                .body(
                [
                    customFieldReplacementList: [],
                    moveAffectedIssuesTo: null,
                    moveFixIssuesTo: null
                ])
                .asString()
            assert result.status >= 200 && result.status < 300
            logger.info("Deleted.")
        } else {
            logger.error("No version found.")
            logger.info("New version will be created.")
            def newVersion = [
                description: "Links to Version Id: " + version.id,
                name: version.name,
                archived: version.archived.toBoolean(),
                released: version.released.toBoolean(),
                userStartDate: formatDate(version.userStartDate),
                userReleaseDate: formatDate(version.userReleaseDate),
                projectId: targetProject.id
            ]
            logger.info("Creating: ${newVersion.toMapString()}")
            def result = Unirest.post("/rest/api/3/version/")
                .header("Content-Type", "application/json")
                .body(newVersion)
                .asString()
            assert result.status >= 200 && result.status < 300
            logger.info("Created.")
        }
    } else {
        if (v != null) {
            logger.info("Found version: ${v.toMapString()}")
            def updatedVersion = [
                description: "Links to Version Id: " + version.id,
                name: version.name,
                archived: version.archived.toBoolean(),
                released: version.released.toBoolean(),
                userStartDate: formatDate(version.userStartDate),
                userReleaseDate: formatDate(version.userReleaseDate),
                projectId: targetProject.id
            ]
            logger.info("Updating to: ${updatedVersion.toMapString()}")
            def result = Unirest.put("/rest/api/3/version/${v.id}")
                .header("Content-Type", "application/json")
                .body(updatedVersion)
                .asString()
            assert result.status >= 200 && result.status < 300
            logger.info("Updated.")
        } else {
            logger.error("Version not found.")
            logger.info("New version will be created.")
            def newVersion = [
                description: "Links to Version Id: " + version.id,
                name: version.name,
                archived: version.archived.toBoolean(),
                released: version.released.toBoolean(),
                userStartDate: formatDate(version.userStartDate),
                userReleaseDate: formatDate(version.userReleaseDate),
                projectId: targetProject.id
            ]
            logger.info("Creating: ${newVersion.toMapString()}")
            def result = Unirest.post("/rest/api/3/version/")
                .header("Content-Type", "application/json")
                .body(newVersion)
                .asString()
            assert result.status >= 200 && result.status < 300
            logger.info("Created.")
        }
    }

    def targetRBConfig = Unirest.get("/rest/api/2/project/${targetProject.key}/properties/release-board-config")
        .asObject(Map)
        .body

    def updatedBoard = [:]

    targetVersions.each { Map va ->
        def targetColumn = (Map) sourceMappedVersions.find { it.name == va.name }
        if (targetColumn == null) {
            return
        }
        def releaseBoardColumn = targetRBConfig.value.columns.find { it.name == targetColumn.releaseBoardColumnName }
        if (releaseBoardColumn == null) {
            return
        }
        updatedBoard.put ("${va.id}", [
            id: va.id,
            columnId: releaseBoardColumn.id
        ])
    }

    logger.info("${updatedBoard}")

    def result = Unirest.put("/rest/api/3/project/${targetProject.key}/properties/release-board-versions")
        .header("Content-Type", "application/json")
        .body(updatedBoard)
        .asString()
    assert result.status >= 200 && result.status < 300
    logger.info("Updated.")
}