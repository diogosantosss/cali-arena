package com.caliarena.repo.jpa

import org.springframework.transaction.interceptor.TransactionAspectSupport
import pt.isel.repo.RepositoryUser
import pt.isel.repo.Transaction

class TransactionJpa(
    override val repoUser: RepositoryUser,
//    override val repoAthlete: RepositoryAthlete,
//    override val repoClub: RepositoryClub,
//    override val repoTournament: RepositoryTournament,
//    override val repoBracket: RepositoryBracket,
//    override val repoMatch: RepositoryMatch,
) : Transaction {
    override fun rollback() {
        TransactionAspectSupport
            .currentTransactionStatus()
            .setRollbackOnly()
    }
}
