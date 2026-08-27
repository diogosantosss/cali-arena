package com.caliarena.repo.trx

interface TransactionManager {
    fun <R> run(block: Transaction.() -> R): R
}
