package com.caliarena.repo.jpa

import org.springframework.transaction.interceptor.TransactionAspectSupport
import pt.isel.repo.RepositoryUser
import pt.isel.repo.Transaction

class TransactionJpa(
    override val repoUser: RepositoryUser,
) : Transaction {
    override fun rollback() {
        TransactionAspectSupport
            .currentTransactionStatus()
            .setRollbackOnly()
    }
}
