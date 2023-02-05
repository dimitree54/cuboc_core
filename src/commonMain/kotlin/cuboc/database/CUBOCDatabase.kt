package cuboc.database

import cuboc.ingredient.PieceOfResource
import cuboc.ingredient.Resource
import cuboc.recipe.Recipe
import cuboc.recipe.Scenario
import cuboc_core.cuboc.database.UserRecipe
import cuboc_core.cuboc.database.UserResource
import cuboc_core.cuboc.database.search.SearchRequest
import cuboc_core.cuboc.database.search.SearchResult

interface CUBOCDatabase {
    suspend fun execute(scenario: Scenario): PieceOfResource?
    suspend fun search(request: SearchRequest): List<SearchResult>
    suspend fun removeResource(resource: UserResource): Boolean
    suspend fun removeRecipe(recipe: UserRecipe): Boolean
    suspend fun addResource(resource: Resource): UserResource?
    suspend fun addRecipe(recipe: Recipe): UserRecipe?
}