package cuboc.ingredient

import kotlinx.serialization.Serializable
import utility.MeasureUnit
import utility.Name

@Serializable
data class Ingredient(val name: Name, val measureUnit: MeasureUnit)