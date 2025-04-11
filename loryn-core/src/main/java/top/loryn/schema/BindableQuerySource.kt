package top.loryn.schema

interface BindableQuerySource<E> : QuerySource {
    fun createEntity(): E
}
