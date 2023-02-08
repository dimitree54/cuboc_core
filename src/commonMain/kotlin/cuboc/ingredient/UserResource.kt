package cuboc_core.cuboc.ingredient

import cuboc.ingredient.Resource

class UserResource(val id: String, resource: Resource) : Resource(resource.ingredient, resource.amount)

