package cuboc_core.cuboc.database.search

import cuboc.ingredient.Ingredient
import cuboc.ingredient.Resource
import cuboc.recipe.Recipe

sealed class SearchResult
class RecipeSearchResult(val recipe: Recipe) : SearchResult()
class ResourceSearchResult(val resource: Resource) : SearchResult()
class IngredientSearchResult(val ingredient: Ingredient) : SearchResult()
