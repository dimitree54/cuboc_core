package cuboc_core.cuboc.database

import cuboc.recipe.Recipe

class UserRecipe(
    val id: String,
    recipe: Recipe
) : Recipe(
    recipe.name,
    recipe.inputs,
    recipe.outputs,
    recipe.instruction
)