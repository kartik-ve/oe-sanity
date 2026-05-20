// Object available by default: virtRunner, log, testSuite, context, runner

import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

def workspace   = System.getenv("WORKSPACE")
def buildNumber = System.getenv("BUILD_NUMBER")

def rootOutputDir

if (workspace && buildNumber) {
    rootOutputDir = new File("${workspace}/${buildNumber}/error_logs")
} else {
    def basePath  = new File(testSuite.project.path).parentFile.path
    rootOutputDir = new File("${basePath}/tc_data")
}

def file = new File(rootOutputDir, "session_ids.txt")
def path = Paths.get(file.getAbsolutePath())

StringBuilder sb = new StringBuilder()

if (!Files.exists(path)) {
    Files.createDirectories(path.getParent())
    Files.createFile(path)
} else {
    sb.append("\n")
}

sb.append(testSuite.name).append("\n")

testSuite.testCaseList.each { tc ->

    if (!tc.isDisabled()) {

        try {
            def propsStep = tc.getTestStepByName("SessionAndTransaction_ids")

            if (propsStep != null) {

                def sessionId = propsStep.getPropertyValue("Session_ID")

                if (sessionId != null && sessionId.trim()) {
                    sb.append(sessionId + ": " + tc.name + "\n")
                }
            }

        } catch (Exception e) {
            log.warn("Error processing testcase '${tc.name}': ${e.message}")
        }
    }
}

Files.write(
    path,
    sb.toString().getBytes("UTF-8"),
    StandardOpenOption.APPEND
)

log.info("Session IDs written to: " + file.getAbsolutePath())
