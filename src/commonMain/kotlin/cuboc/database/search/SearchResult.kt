package cuboc_core.cuboc.database.search

import cuboc.ingredient.Ingredient
import cuboc.ingredient.PieceOfUserResource
import cuboc.recipe.UserRecipe

sealed class SearchResult
class RecipeSearchResult(val userRecipe: UserRecipe) : SearchResult()
class ResourceSearchResult(val pieceOfUserResource: PieceOfUserResource) : SearchResult()
class IngredientSearchResult(val ingredient: Ingredient) : SearchResult()
