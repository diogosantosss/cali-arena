package pt.isel.repo

/**
 * The implementation of Transaction is responsible for creating the
 * necessary repository instances and managing the transaction lifecycle.
 */
interface Transaction {
    val repoUser: RepositoryUser
//    val repoAthlete: RepositoryAthlete
//    val repoClub: RepositoryClub
//    val repoTournament: RepositoryTournament
//    val repoBracket: RepositoryBracket
//    val repoMatch: RepositoryMatch

    fun rollback()
}