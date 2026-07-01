package pt.isel.repo

import com.caliarena.domain.athlete.Athlete
import com.caliarena.domain.athlete.GenderType

interface RepositoryAthlete : Repository<Athlete> {

    fun createAthlete(name: String, gender: GenderType, clubId: Int): Athlete

    fun findByClubId(clubId: Int): List<Athlete>

    fun findByGender(gender: GenderType): List<Athlete>
}
