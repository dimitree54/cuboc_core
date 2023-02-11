package cuboc.database

import cuboc.ingredient.Resource
import cuboc.ingredient.UserResource
import cuboc.recipe.UserRecipe
import cuboc.scenario.Scenario
import cuboc.scenario.ScenarioInProgress

interface CUBOCDatabaseAdmin {
    suspend fun launchScenario(request: Resource, scenario: Scenario): ScenarioInProgress?
    suspend fun removeResource(resource: UserResource): Boolean
    suspend fun removeRecipe(recipe: UserRecipe): Boolean
}