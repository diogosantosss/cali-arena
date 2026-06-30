package com.caliarena.repo.jpa

import com.caliarena.repo.entities.TokenEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface TokenRepositoryJpa : JpaRepository<TokenEntity, String> {
    @Modifying
    @Query(
        value = """
            delete from tokens 
            where user_id = :userId 
                and token_validation in (
                    select token_validation from tokens where user_id = :userId 
                        order by last_used_at desc offset :offset
                )
        """,
        nativeQuery = true,
    )
    fun deleteOldestTokensExceeding(
        @Param("userId") userId: Int,
        @Param("offset") offset: Int,
    ): Int

    @Modifying
    @Query("delete from TokenEntity t where t.tokenValidation = :tokenValidation")
    fun deleteByTokenValidation(
        @Param("tokenValidation") tokenValidation: String,
    ): Int
}
