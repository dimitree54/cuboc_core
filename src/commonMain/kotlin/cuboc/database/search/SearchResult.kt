package cuboc_core.cuboc.database.search

import cuboc.ingredient.Ingredient
import cuboc_core.cuboc.database.UserRecipe
import cuboc_core.cuboc.database.UserResource

sealed class SearchResult
class RecipeSearchResult(val recipe: UserRecipe) : SearchResult()
class ResourceSearchResult(val resource: UserResource) : SearchResult()
class IngredientSearchResult(val ingredient: Ingredient) : SearchResult()
