package pt.isel.repo

/**
 * The implementation of Transaction is responsible for creating the
 * necessary repository instances and managing the transaction lifecycle.
 */
interface Transaction {
    val repoUser: RepositoryUser

    fun rollback()
}