package cuboc.database.firebase

import cuboc.ingredient.PieceOfUserResource
import cuboc.scenario.ScenarioInProgress
import cuboc.scenario.ScenarioStageInProgress
import cuboc.scenario.ScenarioStageStatus
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FirebaseFirestore

class ScenariosFirebase(private val db: FirebaseFirestore) {
    private val collectionName = "scenariosInProgress"

    suspend fun put(scenarioInProgress: ScenarioInProgress) {
        db.collection(collectionName).document(scenarioInProgress.id).set(
            "scenario" to scenarioInProgress
        )
    }

    suspend fun getScenario(scenarioId: String): ScenarioInProgress {
        val document = db.collection(collectionName).document(scenarioId).get()
        return document.get("scenario")
    }

    suspend fun getStages(scenarioId: String): List<ScenarioStageInProgress> {
        val document = db.collection(collectionName).document(scenarioId).get()
        val stages = decodeStages(document)
        return stages.values.toList()
    }

    suspend fun getStage(scenarioId: String, stageId: String): ScenarioStageInProgress? {
        val document = db.collection(collectionName).document(scenarioId).get()
        val stages = decodeStages(document)
        return stages[stageId]
    }

    suspend fun getResourcesAfterStage(scenarioId: String, stageId: String): List<PieceOfUserResource>? {
        val document = db.collection(collectionName).document(scenarioId).get()
        val stages = decodeStages(document)
        val stage = stages[stageId] ?: return null
        if (stage.state != ScenarioStageStatus.DONE) {
            return null
        }
        return decodeResourcesAfterStage(document)
    }

    private fun decodeStages(document: DocumentSnapshot): Map<String, ScenarioStageInProgress> {
        if (document.contains("stagesInProgress")) {
            return document.get("stagesInProgress")
        }
        return emptyMap()
    }

    suspend fun startStage(scenarioId: String, stageInProgress: ScenarioStageInProgress): Boolean {
        val documentReference = db.collection(collectionName).document(scenarioId)
        val stages = decodeStages(documentReference.get())
        documentReference.update("stagesInProgress" to stages + (stageInProgress.id to stageInProgress))
        return true
    }

    private fun decodeResourcesAfterStage(document: DocumentSnapshot): List<PieceOfUserResource> {
        if (document.contains("resourcesAfterStage")) {
            return document.get("resourcesAfterStage")
        }
        return emptyList()
    }

    suspend fun finishStage(
        scenarioId: String,
        stageId: String,
        resourcesAfterStage: List<PieceOfUserResource>
    ): Boolean {
        val documentReference = db.collection(collectionName).document(scenarioId)
        val stages = decodeStages(documentReference.get())
        val stageToFinish = stages[stageId] ?: return false
        documentReference.update(
            "stagesInProgress" to stages + (stageId to stageToFinish.getWithUpdatedState(
                ScenarioStageStatus.DONE
            )),
            "resourcesAfterStage" to resourcesAfterStage
        )
        return true
    }

    suspend fun remove(id: String): Boolean {
        db.collection(collectionName).document(id).delete()
        return true
    }

    suspend fun searchByRequester(requesterId: String): List<ScenarioInProgress> {
        TODO()
    }
}
