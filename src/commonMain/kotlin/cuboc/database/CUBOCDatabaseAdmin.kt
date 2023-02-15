package cuboc.database

import cuboc.ingredient.Resource
import cuboc.ingredient.UserResource
import cuboc.recipe.UserRecipe
import cuboc.scenario.Scenario
import cuboc.scenario.ScenarioInProgress
import cuboc.scenario.ScenarioStageInProgress

interface CUBOCDatabaseAdmin {
    suspend fun launchScenario(request: List<Resource>, scenario: Scenario): ScenarioInProgress
    suspend fun finishStage(scenarioInProgress: ScenarioInProgress, stageInProgress: ScenarioStageInProgress)
    suspend fun cancelScenario(scenarioInProgress: ScenarioInProgress)
    suspend fun cancelStage(scenarioStageInProgress: ScenarioStageInProgress)
    suspend fun removeResource(resource: UserResource)
    suspend fun removeRecipe(recipe: UserRecipe)
}