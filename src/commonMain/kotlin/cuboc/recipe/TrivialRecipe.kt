package cuboc.recipe

import cuboc.ingredient.IngredientRequest
import cuboc.ingredient.RecipeInput
import cuboc.ingredient.RecipeOutput

class TrivialRecipe(request: IngredientRequest) : Recipe(
    null,
    "trivial " + request.ingredient.name,
    setOf(RecipeInput(request.ingredient, request.amount, true)),
    setOf(RecipeOutput(request.ingredient, request.amount, true)),
    Instruction(
        0,
        "Just take ${request.amount} ${request.ingredient.measureUnit.name} of ${request.ingredient.name}"
    )
)