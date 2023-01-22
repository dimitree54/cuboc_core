package cuboc_core

class IOSPlatform: Platform {
    override val name: String = "JS"
}

actual fun getPlatform(): Platform = IOSPlatform()