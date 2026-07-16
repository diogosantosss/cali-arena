import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

subprojects {
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()

        testLogging {
            events(
                TestLogEvent.PASSED,
                TestLogEvent.FAILED,
                TestLogEvent.STANDARD_OUT,
                TestLogEvent.STANDARD_ERROR,
            )
            exceptionFormat = TestExceptionFormat.SHORT
            showExceptions = true
            showCauses = true
            showStackTraces = true
        }

        addTestListener(object : TestListener {
            override fun beforeSuite(suite: TestDescriptor) {}
            override fun beforeTest(testDescriptor: TestDescriptor) {}
            override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {}

            override fun afterSuite(desc: TestDescriptor, result: TestResult) {
                if (desc.parent == null) {
                    val output = "Results: ${result.resultType} (${result.testCount} tests, ${result.successfulTestCount} passes, ${result.failedTestCount} failures, ${result.skippedTestCount} skipped)"
                    val startItem = "|  "
                    val endItem = "  |"
                    val repeatLength = startItem.length + output.length + endItem.length
                    println("\n" + "-".repeat(repeatLength) + "\n" + startItem + output + endItem + "\n" + "-".repeat(repeatLength) + "\n")
                }
            }
        })
    }
}