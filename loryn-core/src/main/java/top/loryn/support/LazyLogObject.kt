package top.loryn.support

@JvmRecord
data class LazyLogObject(val getObject: () -> Any?) {
    override fun toString() = getObject().toString()
}
