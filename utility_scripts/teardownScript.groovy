<![CDATA[def project = runner.testSuite.project
def suite = runner.testSuite

def workspace = System.getenv("WORKSPACE")
def buildNumber = System.getenv("BUILD_NUMBER")

def rootOutputDir

if (workspace && buildNumber) {
    rootOutputDir = new File("${workspace}/${buildNumber}/tc_data")
} else {
    def basePath = new File(project.path).parentFile.path
    rootOutputDir = new File("${basePath}/tc_data")
}

rootOutputDir.mkdirs()

def suiteName = suite.name.replaceAll(/[\\\/:*?"<>|]/, "_")

int totalSaved = 0

runner.results.each { caseResult ->

    def testCase = caseResult.testCase
    def caseName = testCase.name.replaceAll(/[\\\/:*?"<>|]/, "_")

    def caseRootDir = new File(rootOutputDir, "${suiteName}/${caseName}")
    def requestDir = new File(caseRootDir, "requests")
    def responseDir = new File(caseRootDir, "responses")

    requestDir.mkdirs()
    responseDir.mkdirs()

    // Get only executed steps for this run
    def executedSteps = caseResult.results.collect { it.testStep }

    executedSteps.each { step ->

        if (step instanceof com.eviware.soapui.impl.wsdl.teststeps.RestTestRequestStep
                && !step.disabled) {

            def safeName = step.name.replaceAll("[^a-zA-Z0-9._-]", "_")

            // REQUEST
            def requestContent = step.testRequest?.requestContent
            if (requestContent?.trim()) {
                new File(requestDir, "${safeName}.json")
                        .write(requestContent, "UTF-8")
            }

            // RESPONSE (read from step, not stepResult)
            def responseContent = step.testRequest?.response?.contentAsString
            if (responseContent?.trim()) {
                new File(responseDir, "${safeName}.json")
                        .write(responseContent, "UTF-8")
            }

            totalSaved++
        }
    }
}

log.info "Total API steps saved (request/response pairs): ${totalSaved}"
]]>