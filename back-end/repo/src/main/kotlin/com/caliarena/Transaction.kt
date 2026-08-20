package com.caliarena

/**
 * The implementation of Transaction is responsible for creating the
 * necessary repository instances and managing the transaction lifecycle.
 */
interface Transaction {
    val repoAthlete: RepositoryAthlete
    val repoClub: RepositoryClub
    val repoEnduranceRoutine: RepositoryEnduranceRoutine
    val repoMatch: RepositoryMatch
    val repoTournament: RepositoryTournament
    val repoUser: RepositoryUser
    val repoScreenRoutine: RepoScreenRoutine
    fun rollback()
}