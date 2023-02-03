package cuboc.database

import cuboc.ingredient.Ingredient
import cuboc.ingredient.PieceOfResource
import cuboc.ingredient.Resource
import cuboc.recipe.Recipe
import cuboc.recipe.Scenario
import cuboc_core.cuboc.database.search.SearchRequest
import cuboc_core.cuboc.database.search.SearchResult

interface CUBOCDatabase {
    suspend fun execute(scenario: Scenario): PieceOfResource?
    suspend fun search(request: SearchRequest): List<SearchResult>
    suspend fun removeResource(resource: Resource): Boolean
    suspend fun removeRecipe(recipe: Recipe): Boolean
    suspend fun addResource(ingredient: Ingredient, amount: Double): Resource?
    suspend fun addRecipe(recipe: Recipe): Boolean
}