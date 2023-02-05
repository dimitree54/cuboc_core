package cuboc_core.cuboc.database

import cuboc.ingredient.Resource

class UserResource(val id: String, resource: Resource) : Resource(resource.ingredient, resource.amount)

