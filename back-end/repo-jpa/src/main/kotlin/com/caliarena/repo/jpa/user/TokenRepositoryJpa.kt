package com.caliarena.repo.jpa.user

import com.caliarena.repo.entities.user.TokenEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface TokenRepositoryJpa : JpaRepository<TokenEntity, String> {
    /**
     * Deletes the oldest tokens for a user that exceed a specified [offset].
     *
     * Fetches all tokens for the given user ordered by last_used_at (descending) and created_at (descending),
     * skips the first [offset] tokens (most recent), and deletes the remaining older ones.
     *
     * @return The number of tokens deleted
     *
     * @note The @Modifying annotation with:
     * - [clearAutomatically] = true: clears the persistence context after deletion to prevent stale data
     * - [flushAutomatically] = true: ensures changes are immediately flushed to the database
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        value = """
            delete from tokens 
            where user_id = :userId 
                and token_validation in (
                    select token_validation from tokens where user_id = :userId 
                        order by last_used_at desc, created_at desc offset :offset
                )
        """,
        nativeQuery = true,
    )
    fun deleteOldestTokensExceeding(
        @Param("userId") userId: Int,
        @Param("offset") offset: Int,
    ): Int

    /**
     * Deletes a token by its validation string.
     *
     * @return The number of tokens deleted (typically 0 or 1)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from TokenEntity t where t.tokenValidation = :tokenValidation")
    fun deleteByTokenValidation(
        @Param("tokenValidation") tokenValidation: String,
    ): Int
}
