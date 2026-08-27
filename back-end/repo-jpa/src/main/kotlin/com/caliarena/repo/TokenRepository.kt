package com.caliarena.repo

import com.caliarena.repo.entities.user.TokenEntity
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

interface TokenRepository : CrudRepository<TokenEntity, String> {
    /**
     * Removes the least-recently-used tokens for a user, keeping only the newest [offset] entries.
     *
     * Tokens are ranked by:
     * 1. `last_used_at` descending
     * 2. `created_at` descending
     *
     * Any tokens beyond that window are deleted in a single bulk operation.
     *
     * @return number of deleted tokens
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        value = """
            delete from tokens 
            where user_id = :userId 
                and token_validation in (
                    select token_validation 
                    from tokens 
                    where user_id = :userId 
                    order by last_used_at desc, created_at desc 
                    offset :offset
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
