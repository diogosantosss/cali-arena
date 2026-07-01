package pt.isel.repo

import com.caliarena.domain.athlete.GenderType
import com.caliarena.domain.bracket.Bracket
import com.caliarena.domain.bracket.BracketStage

interface RepositoryBracket : Repository<Bracket> {

    fun createBracket(tournamentId: Int, gender: GenderType, stage: BracketStage): Bracket

    fun findByTournamentId(tournamentId: Int): List<Bracket>

    fun findByTournamentIdAndGender(tournamentId: Int, gender: GenderType): List<Bracket>
}