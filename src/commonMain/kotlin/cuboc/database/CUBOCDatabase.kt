package cuboc.database

import cuboc.ingredient.PieceOfResource
import cuboc.ingredient.Resource
import cuboc.recipe.Recipe
import cuboc.recipe.Scenario
import cuboc_core.cuboc.database.search.SearchRequest
import cuboc_core.cuboc.database.search.SearchResult
import cuboc_core.cuboc.ingredient.UserResource
import cuboc_core.cuboc.recipe.UserRecipe

interface CUBOCDatabase {
    suspend fun execute(scenario: Scenario, requesterId: String): PieceOfResource?
    suspend fun search(request: SearchRequest): List<SearchResult>
    suspend fun removeResource(resource: UserResource): Boolean
    suspend fun removeRecipe(recipe: UserRecipe): Boolean
    suspend fun addResource(resource: Resource): UserResource?
    suspend fun addRecipe(recipe: Recipe): UserRecipe?
}