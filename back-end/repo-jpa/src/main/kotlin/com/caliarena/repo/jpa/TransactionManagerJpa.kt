package com.caliarena.repo.jpa

import com.caliarena.Transaction
import com.caliarena.TransactionManager
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class TransactionManagerJpa(
    private val transaction: TransactionJpa,
) : TransactionManager {
    @Transactional
    override fun <R> run(block: Transaction.() -> R): R = transaction.block()
}
