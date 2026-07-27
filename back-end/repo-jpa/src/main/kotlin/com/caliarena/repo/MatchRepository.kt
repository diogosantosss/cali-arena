package com.caliarena.repo

import com.caliarena.RepositoryMatch
import com.caliarena.domain.match.Match
import com.caliarena.domain.match.MatchProgress
import com.caliarena.domain.match.MatchStatus
import com.caliarena.repo.entities.match.MatchEntity
import com.caliarena.repo.entities.match.MatchEntity.Companion.fromDomain
import com.caliarena.repo.entities.match.MatchProgressEntity
import com.caliarena.repo.entities.match.MatchProgressEntity.Companion.fromDomain
import com.caliarena.repo.jpa.match.MatchProgRepositoryJpa
import com.caliarena.repo.jpa.match.MatchRepositoryJpa
import com.caliarena.repo.jpa.routine.ExerciseRepositoryJpa
import com.caliarena.repo.jpa.tournament.BracketRepositoryJpa
import com.caliarena.repo.jpa.user.UserRepositoryJpa
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class MatchRepository(
    private val matchProgJpa: MatchProgRepositoryJpa,
    private val matchJpa: MatchRepositoryJpa,
    private val bracketJpa: BracketRepositoryJpa,
    private val userJpa: UserRepositoryJpa,
    private val exerciseJpa: ExerciseRepositoryJpa,
) : RepositoryMatch {
    override fun createMatch(
        bracketId: Int,
        routineId: Int,
        judgeId: Int,
        redFromMatchId: Int?,
        blueFromMatchId: Int?,
        createdAt: Instant,
    ): Match? {
        val bracket = bracketJpa.findByIdOrNull(bracketId) ?: return null
        val judge = userJpa.findByIdOrNull(judgeId) ?: return null
        return matchJpa
            .save(
                MatchEntity(
                    bracket = bracket,
                    routineId = routineId,
                    judge = judge,
                    redFromMatchId = redFromMatchId,
                    blueFromMatchId = blueFromMatchId,
                    status = MatchStatus.PENDING,
                    createdAt = createdAt.epochSecond,
                ),
            ).toDomain()
    }

    override fun findByBracketId(bracketId: Int): List<Match> = matchJpa.findByBracketId(bracketId).map(MatchEntity::toDomain)

    override fun findByStatus(status: MatchStatus): List<Match> = matchJpa.findByStatus(status).map(MatchEntity::toDomain)

    override fun createMatchProgress(
        matchId: Int,
        updatedAt: Instant,
    ): MatchProgress? {
        val match = matchJpa.findByIdOrNull(matchId) ?: return null
        return matchProgJpa
            .save(
                MatchProgressEntity(
                    match = match,
                    redCurrentReps = 0,
                    blueCurrentReps = 0,
                    updatedAt = updatedAt.epochSecond,
                ),
            ).toDomain()
    }

    override fun findProgressByMatchId(matchId: Int): MatchProgress? = matchProgJpa.findByMatchId(matchId)?.toDomain()

    override fun updateMatchProgress(
        progress: MatchProgress,
        redCurrentExerciseId: Int?,
        blueCurrentExerciseId: Int?,
    ): MatchProgress? {
        val match = matchJpa.findByIdOrNull(progress.matchId) ?: return null
        matchProgJpa.findByIdOrNull(progress.id) ?: return null

        if (redCurrentExerciseId != null || blueCurrentExerciseId != null) {
            val redCurrentExercise = redCurrentExerciseId?.let { exerciseJpa.findByIdOrNull(it) }
            val blueCurrentExercise = blueCurrentExerciseId?.let { exerciseJpa.findByIdOrNull(it) }

            return matchProgJpa.save(progress.fromDomain(match, redCurrentExercise, blueCurrentExercise)).toDomain()
        }

        return matchProgJpa.save(progress.fromDomain(match, null, null)).toDomain()
    }

    override fun findById(id: Int): Match? = matchJpa.findByIdOrNull(id)?.toDomain()

    override fun findAll(): List<Match> = matchJpa.findAll().map(MatchEntity::toDomain)

    override fun save(entity: Match): Match? {
        val bracket = bracketJpa.findByIdOrNull(entity.bracketId) ?: return null
        val judge = userJpa.findByIdOrNull(entity.judgeId) ?: return null
        return matchJpa.save(entity.fromDomain(bracket, null, null, judge, null)).toDomain()
    }

    override fun deleteById(id: Int) = matchJpa.deleteById(id)

    override fun clear() {
        matchProgJpa.deleteAll()
        matchJpa.clearFromMatchReferences()
        matchJpa.deleteAll()
    }
}
