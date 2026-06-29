package com.caliarena.app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.caliarena"])
class App {

}

fun main() {
    runApplication<App>()
}
