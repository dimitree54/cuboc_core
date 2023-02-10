package cuboc.database

import cuboc.ingredient.PieceOfUserResource
import cuboc.ingredient.Resource
import cuboc.ingredient.UserResource
import cuboc.recipe.Recipe
import cuboc_core.cuboc.database.search.SearchRequest
import cuboc_core.cuboc.database.search.SearchResult
import cuboc_core.cuboc.recipe.UserRecipe
import cuboc_core.cuboc.scenario.CraftingScenario

interface CUBOCDatabase {
    suspend fun execute(scenario: CraftingScenario, requesterId: String): PieceOfUserResource?
    suspend fun search(request: SearchRequest): List<SearchResult>
    suspend fun removeResource(resource: UserResource): Boolean
    suspend fun removeRecipe(recipe: UserRecipe): Boolean
    suspend fun addResource(resource: Resource): UserResource?
    suspend fun addRecipe(recipe: Recipe): UserRecipe?
}