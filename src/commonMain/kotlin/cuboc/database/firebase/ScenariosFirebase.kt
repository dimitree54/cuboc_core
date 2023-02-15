package cuboc.database.firebase

import cuboc.ingredient.PieceOfUserResource
import cuboc.scenario.ScenarioInProgress
import cuboc.scenario.ScenarioStageInProgress
import cuboc.scenario.ScenarioStageStatus
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.FirebaseFirestore

class ScenariosFirebase(private val db: FirebaseFirestore) {
    private val collectionName = "scenariosInProgress"
    private val scenarioField = "scenario"
    private val stagesField = "stagesInProgress"
    private val resourcesField = "reservedResources"

    suspend fun put(scenarioInProgress: ScenarioInProgress) {
        db.collection(collectionName).document(scenarioInProgress.id).set(
            mapOf(
                scenarioField to scenarioInProgress,
                stagesField to emptyList<ScenarioStageInProgress>(),
                resourcesField to emptyList<Map<String, List<PieceOfUserResource>>>()
            )
        )
    }

    suspend fun getScenario(scenarioId: String): ScenarioInProgress {
        val document = db.collection(collectionName).document(scenarioId).get()
        return document.get(scenarioField)
    }

    private fun decodeStages(document: DocumentSnapshot): Map<String, ScenarioStageInProgress> {
        return document.get<List<ScenarioStageInProgress>>(stagesField).associateBy { it.id }
    }

    suspend fun getStages(scenarioId: String): Map<String, ScenarioStageInProgress> {
        val document = db.collection(collectionName).document(scenarioId).get()
        return decodeStages(document)
    }

    suspend fun startStage(scenarioId: String, stageInProgress: ScenarioStageInProgress) {
        val documentReference = db.collection(collectionName).document(scenarioId)
        documentReference.update(
            stagesField to FieldValue.arrayUnion(stageInProgress)
        )
    }

    private fun decodeResources(document: DocumentSnapshot): Map<String, List<PieceOfUserResource>> {
        val resourcesList = document.get<List<Map<String, List<PieceOfUserResource>>>>(resourcesField)
        val resourcesMap = mutableMapOf<String, MutableList<PieceOfUserResource>>()
        for (resourcesPerStage in resourcesList) {
            for ((stageId, resources) in resourcesPerStage) {
                resourcesMap.getOrPut(stageId) { mutableListOf() }.addAll(resources)
            }
        }
        return resourcesMap
    }

    suspend fun getResourcesForStage(
        scenarioId: String,
        stageId: String
    ): List<PieceOfUserResource> {
        val document = db.collection(collectionName).document(scenarioId).get()
        return decodeResources(document)[stageId] ?: emptyList()
    }

    suspend fun finishStage(
        scenarioId: String,
        finishedStage: ScenarioStageInProgress,
        resourcesForLaterStages: Map<String, List<PieceOfUserResource>>
    ) {
        require(finishedStage.state == ScenarioStageStatus.DONE) { "Invalid status of finished stage" }
        val documentReference = db.collection(collectionName).document(scenarioId)
        val document = documentReference.get()
        val previousStageState = decodeStages(document)[finishedStage.id]!!
        documentReference.update(
            mapOf(
                stagesField to FieldValue.arrayRemove(previousStageState),
                stagesField to FieldValue.arrayUnion(finishedStage),
                resourcesField to FieldValue.arrayUnion(resourcesForLaterStages)
            )
        )
    }

    suspend fun remove(id: String): Boolean {
        db.collection(collectionName).document(id).delete()
        return true
    }
}
