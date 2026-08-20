package com.caliarena.repo.jpa

import com.caliarena.RepoScreenRoutine
import com.caliarena.RepositoryAthlete
import com.caliarena.RepositoryClub
import com.caliarena.RepositoryEnduranceRoutine
import com.caliarena.RepositoryMatch
import com.caliarena.RepositoryTournament
import com.caliarena.RepositoryUser
import com.caliarena.Transaction
import org.springframework.stereotype.Component
import org.springframework.transaction.interceptor.TransactionAspectSupport

@Component
class TransactionJpa(
    override val repoAthlete: RepositoryAthlete,
    override val repoClub: RepositoryClub,
    override val repoMatch: RepositoryMatch,
    override val repoEnduranceRoutine: RepositoryEnduranceRoutine,
    override val repoTournament: RepositoryTournament,
    override val repoUser: RepositoryUser,
    override val repoScreenRoutine: RepoScreenRoutine,
) : Transaction {
    override fun rollback() {
        TransactionAspectSupport
            .currentTransactionStatus()
            .setRollbackOnly()
    }
}
