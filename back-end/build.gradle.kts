import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestListener
import org.gradle.api.tasks.testing.TestResult
import java.io.File
import java.nio.file.Files

subprojects {
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        testLogging { events() }

        finalizedBy(rootProject.tasks.named("aggregateTestSummary"))

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

val aggregateTestSummary = tasks.register("aggregateTestSummary") {
    val reportDirs = subprojects.flatMap { project ->
        project.tasks.withType<Test>().mapNotNull { task ->
            task.reports.junitXml.outputLocation.asFile.orNull
        }
    }.distinct()

    doLast {
        var total = 0
        var passed = 0
        var failed = 0
        var skipped = 0

        reportDirs.forEach { dir ->
            if (dir.exists()) {
                Files.walk(dir.toPath()).use { paths ->
                    paths.filter { it.toString().endsWith(".xml") }.forEach { path ->
                        val suite = Regex("""<testsuite[^>]*>""").find(path.toFile().readText())?.value ?: return@forEach
                        val attr = { name: String ->
                            Regex("""$name="(\d+)"""").find(suite)?.groupValues?.get(1)?.toInt() ?: 0
                        }

                        total += attr("tests")
                        passed += attr("tests") - attr("skipped") - attr("failures") - attr("errors")
                        failed += attr("failures") + attr("errors")
                        skipped += attr("skipped")
                    }
                }
            }
        }

        val output =
            "Total Gradle Test Run -> SUCCESS ($total tests, $passed passed, $failed failed, $skipped skipped)"

        val border = "-".repeat(output.length + 4)
        println("\n$border\n| $output |\n$border\n")
    }
}