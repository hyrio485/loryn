package top.loryn.expression

interface EntityCreator<E> {
    fun createEntity(): E = throw UnsupportedOperationException()
}
