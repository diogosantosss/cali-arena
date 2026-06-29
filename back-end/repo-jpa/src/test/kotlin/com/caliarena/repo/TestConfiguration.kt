package com.caliarena.repo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories


@SpringBootApplication
@EntityScan("com.caliarena.repo.entities")
@EnableJpaRepositories("com.caliarena.repo")
open class TestConfiguration