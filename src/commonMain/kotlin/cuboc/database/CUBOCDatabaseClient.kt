package cuboc.database

import cuboc.ingredient.PieceOfUserResource
import cuboc.ingredient.Resource
import cuboc.ingredient.UserResource
import cuboc.recipe.Recipe
import cuboc.recipe.UserRecipe
import cuboc.scenario.Scenario
import cuboc.scenario.ScenarioInProgress
import cuboc_core.cuboc.database.search.SearchRequest
import cuboc_core.cuboc.database.search.SearchResult
import cuboc_core.utility.Report

interface CUBOCDatabaseClient {
    suspend fun search(request: SearchRequest): List<SearchResult>
    suspend fun removeMyResource(resource: UserResource): Boolean
    suspend fun removeMyRecipe(recipe: UserRecipe): Boolean
    suspend fun reportResource(resource: UserResource, report: Report): Boolean
    suspend fun reportRecipe(recipe: UserRecipe, report: Report): Boolean
    suspend fun addResource(resource: Resource): UserResource?
    suspend fun addRecipe(recipe: Recipe): UserRecipe?
    suspend fun getCost(scenario: Scenario): Double?
    suspend fun chooseBestUserResources(resource: Resource): List<PieceOfUserResource>?
    suspend fun getMyResources(): List<PieceOfUserResource>
    suspend fun getMyRecipes(): List<UserRecipe>
    suspend fun getMyScenarios(): List<ScenarioInProgress>
    suspend fun launchScenario(scenario: Scenario): ScenarioInProgress?
}

