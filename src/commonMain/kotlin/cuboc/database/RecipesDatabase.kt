package cuboc.database

import cuboc.ingredient.Ingredient
import cuboc.recipe.Recipe

interface RecipesDatabase {
    fun search(ingredients: List<Ingredient>): List<Recipe>
    fun add(recipe: Recipe)
}