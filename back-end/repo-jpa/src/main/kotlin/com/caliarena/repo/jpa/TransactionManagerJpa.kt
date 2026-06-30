package com.caliarena.repo.jpa

import com.caliarena.repo.UserRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pt.isel.repo.Transaction
import pt.isel.repo.TransactionManager

@Component
class TransactionManagerJpa(
    private val userRepositoryJpa: UserRepositoryJpa,
    private val tokenRepositoryJpa: TokenRepositoryJpa,
) : TransactionManager {
    @Transactional
    override fun <R> run(block: Transaction.() -> R): R {
        val repoUser = UserRepository(userRepositoryJpa, tokenRepositoryJpa)

        return TransactionJpa(repoUser).block()
    }
}
