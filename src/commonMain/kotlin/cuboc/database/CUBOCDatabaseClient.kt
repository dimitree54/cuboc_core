package cuboc.database

import cuboc.ingredient.PieceOfUserResource
import cuboc.ingredient.Resource
import cuboc.ingredient.UserResource
import cuboc.recipe.Recipe
import cuboc.recipe.UserRecipe
import cuboc.scenario.Scenario
import cuboc.scenario.ScenarioInProgress
import cuboc.scenario.ScenarioStageInProgress
import cuboc_core.cuboc.database.search.SearchRequest
import cuboc_core.cuboc.database.search.SearchResult

interface CUBOCDatabaseClient {
    suspend fun search(request: SearchRequest): List<SearchResult>
    suspend fun addResource(resource: Resource): UserResource  // todo what with security?
    suspend fun addRecipe(recipe: Recipe): UserRecipe  // todo what with security?
    suspend fun getMyResources(): List<PieceOfUserResource>
    suspend fun getMyRecipes(): List<UserRecipe>
    suspend fun getMyScenariosInProgress(): List<ScenarioInProgress>
    suspend fun getMyStagesInProgress(): List<ScenarioStageInProgress>
    suspend fun removeMyResource(resource: UserResource)  // todo what with security?
    suspend fun removeMyRecipe(recipe: UserRecipe)  // todo what with security?
    suspend fun getCost(scenario: Scenario): Double?
    suspend fun chooseBestUserResources(resource: Resource): List<PieceOfUserResource>?
}
