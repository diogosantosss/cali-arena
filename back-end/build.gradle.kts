import org.gradle.api.tasks.testing.Test

subprojects {
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        testLogging { events() }

        addTestListener(object : TestListener {
            override fun beforeSuite(suite: TestDescriptor) {}
            override fun beforeTest(testDescriptor: TestDescriptor) {}
            override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {}

            override fun afterSuite(desc: TestDescriptor, result: TestResult) {
                val isModule = desc.parent == null && desc.displayName.contains(":test")

                if (isModule) {

                    val output =
                        "Module ${desc.displayName.trim()} -> ${result.resultType} " +
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