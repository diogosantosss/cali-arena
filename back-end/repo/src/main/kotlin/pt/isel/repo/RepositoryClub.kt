package pt.isel.repo

import com.caliarena.domain.club.Club

interface RepositoryClub : Repository<Club> {

    fun createClub(name: String, shortName: String?): Club

    fun findByName(name: String): Club?
}