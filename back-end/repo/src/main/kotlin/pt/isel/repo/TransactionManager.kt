package pt.isel.repo

interface TransactionManager {
    fun <R> run(block: Transaction.() -> R): R
}