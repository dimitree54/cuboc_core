package cuboc.scenario

import cuboc.recipe.Recipe
import kotlinx.serialization.Serializable

@Serializable
data class ScenarioStage(
    val id: String,
    val recipe: Recipe,
)