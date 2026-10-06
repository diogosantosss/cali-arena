import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestListener
import org.gradle.api.tasks.testing.TestResult
import java.util.concurrent.ConcurrentHashMap

object TestRuns {
    val results = ConcurrentHashMap<String, LongArray>()
}

subprojects {
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        testLogging { events() }

        finalizedBy(rootProject.tasks.named("finalTestSummary"))

        addTestListener(object : TestListener {
            override fun beforeSuite(suite: TestDescriptor) {}
            override fun beforeTest(testDescriptor: TestDescriptor) {}
            override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {}

            override fun afterSuite(desc: TestDescriptor, result: TestResult) {
                if (desc.parent == null && desc.displayName.contains(":test")) {
                    val module = desc.displayName.trim()

                    TestRuns.results[module] =
                        longArrayOf(result.testCount, result.successfulTestCount, result.failedTestCount, result.skippedTestCount)

                    val output =
                        "Module $module -> ${result.resultType} " +
                                "(${result.testCount} tests, " +
                                "${result.successfulTestCount} passed, " +
                                "${result.failedTestCount} failed, " +
                                "${result.skippedTestCount} skipped)"

                    val border = "-".repeat(output.length + 4)
                    println("\n$border\n| $output |\n$border\n")
                }
            }
        })
    }
}

val finalTestSummary = tasks.register("finalTestSummary") {
    doLast {
        val totals = longArrayOf(0, 0, 0, 0)
        TestRuns.results.values.forEach { v -> (0..3).forEach { i -> totals[i] += v[i] } }

        if (totals[0] > 0) {
            val resultType = if (totals[2] > 0) "FAILURE" else "SUCCESS"
            val output =
                "Total Gradle Test Run -> $resultType (${totals[0]} tests, " +
                        "${totals[1]} passed, ${totals[2]} failed, ${totals[3]} skipped)"

            val border = "-".repeat(output.length + 4)
            println("\n$border\n| $output |\n$border\n")
            TestRuns.results.clear()
        }
    }
}