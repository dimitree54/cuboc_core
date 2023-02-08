package cuboc.recipe

import cuboc.ingredient.IngredientRequest
import cuboc.ingredient.RecipeInput
import cuboc.ingredient.RecipeOutput
import utility.Text

class TrivialRecipe(request: IngredientRequest) : Recipe(
    "trivial " + request.ingredient.name,
    setOf(RecipeInput(request.ingredient, request.amount, true)),
    setOf(RecipeOutput(request.ingredient, request.amount, true)),
    Instruction(
        0,
        Text("Just take ${request.amount} ${request.ingredient.measureUnit} of ${request.ingredient.name}")
    )
)