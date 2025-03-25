package top.loryn.database

@JvmRecord
data class LazyLogObject(val getObject: () -> Any?) {
    override fun toString() = getObject().toString()
}
