// Objects available by default: log, project

def envConfig = [
    "8285": [
        hostname: "illnqw8285",
        endpoint: "http://illnqw8285:11111",
        jdbc: "jdbc:oracle:thin:OMS1OMS/OMS1OMS@illnqw8374:1521:CHCDB8285"
    ],
    "8289": [
        hostname: "illnqw8289",
        endpoint: "http://illnqw8289:11111",
        jdbc: "jdbc:oracle:thin:OMS1OMS/OMS1OMS@illnqw8377:1521:CHCDB8289"
    ],
    "8342": [
        hostname: "illnqw8342",
        endpoint: "http://illnqw8342:11111",
        jdbc: "jdbc:oracle:thin:OMS1OMS/OMS1OMS@illnqw8393:1521:CHCDB8342"
    ],
    "8365": [
        hostname: "illnqw8365",
        endpoint: "http://illnqw8365:11111",
        jdbc: "jdbc:oracle:thin:OMS1OMS/OMS1OMS@illnqw8404:1521:CHCDB8365"
    ],
    "8645": [
        hostname: "illnqw8645",
        endpoint: "http://illnqw8645:11111",
        jdbc: "jdbc:oracle:thin:OMS1OMS/OMS1OMS@illnqw8655:1521:CHCDB8645"
    ],
    "8665": [
        hostname: "illnqw8665",
        endpoint: "http://illnqw8665:11111",
        jdbc: "jdbc:oracle:thin:OMS1OMS/OMS1OMS@illnqw8696:1521:CHCDB8665"
    ],
    "8666": [
        hostname: "illnqw8666",
        endpoint: "http://illnqw8666:11111",
        jdbc: "jdbc:oracle:thin:OMS1OMS/OMS1OMS@illnqw8695:1521:CHCDB8666"
    ],
    "8667": [
        hostname: "illnqw8667",
        endpoint: "http://illnqw8667:11111",
        jdbc: "jdbc:oracle:thin:OMS1OMS/OMS1OMS@illnqw8697:1521:CHCDB8667"
    ],
    "8731": [
        hostname: "illnqw8731",
        endpoint: "http://illnqw8731:11111",
        jdbc: "jdbc:oracle:thin:OMS1OMS/OMS1OMS@illnqw8740:1521:CHCDB8731"
    ],
    "8733": [
        hostname: "illnqw8733",
        endpoint: "http://illnqw8733:11111",
        jdbc: "jdbc:oracle:thin:OMS1OMS/OMS1OMS@illnqw8692:1521:CHCDB8733"
    ],
    "SIT1": [
        hostname: "mwhlvchca01",
        endpoint: "https://sit1-oe-npe.amdocs.spectrum.com",
        jdbc: "jdbc:oracle:thin:OMS1OMS/OMS1OMS@mwhlvchcadb01:1521:CHCDB1"
    ],
    "QA1": [
        hostname: "mwhlvchca02",
        endpoint: "https://qa1-oe-npe.amdocs.spectrum.com",
        jdbc: "jdbc:oracle:thin:OMS1OMS/OMS1OMS@mwhlvchcadb02:1521:CHCDB2"
    ],
    "UAT1": [
        hostname: "mwhlvchca03",
        endpoint: "https://uat1-oe-npe.amdocs.spectrum.com",
        jdbc: "jdbc:oracle:thin:OMS1OMS/OMS1OMS@mwhlvchcadb03:1521:CHCDB3"
    ],
    "HF1": [
        hostname: "mwhlvchca04",
        endpoint: "https://hf1-oe-npe.amdocs.spectrum.com",
        jdbc: "jdbc:oracle:thin:OMS1OMS/OMS1OMS@mwhlvchcadb04:1521:CHCDB4"
    ],
    "PLAB": [
        hostname: "mwhlvchcaamc101",
        endpoint: "https://plab-oe-omni.amdocs.spectrum.com",
        jdbc: "jdbc:oracle:thin:CHCOMS/chc_4c0nn@chcplscn1:1521/chcomspl1"
    ]
]

def activeEnv = project.activeEnvironment
def env = System.getProperty("env")

if (env) {
    env = env.trim().toUpperCase()

    def config = envConfig[env]

    if (config) {
        def endpoint = config.endpoint
        def jdbcUrl = config.jdbc
        def username = "omswrk1"
        def hostname = config.hostname

        project.setPropertyValue("ENV", env)
        project.setPropertyValue("MecEndpoint", endpoint)
        project.setPropertyValue("MecDBConnection", jdbcUrl)
        project.setPropertyValue("USER", username)
        project.setPropertyValue("HOST", hostname)

        log.info "ENV: ${env}"
        log.info "Endpoint: ${endpoint}"
        log.info "JDBC URL: ${jdbcUrl}"
        log.info "User: ${username}"
        log.info "Host: ${hostname}"
    }
}


// Custom Test Runner (Retry + API Logging)

def isReadyAPI = Package.getPackages().any { 
    it.name.startsWith("com.smartbear.ready")
}

def runWithRetry = { testRunner, context ->

    def maxAttempts = 3
    def retryDelay = 1000
    def timeoutString = "timed out"

    def passedString = isReadyAPI ? "PASS" : "OK"

    def testCase = context.testCase
    def testSuite = testCase.testSuite
    def testSteps = testCase.getTestStepList()

    def workspace = System.getenv("WORKSPACE")
    def buildNumber = System.getenv("BUILD_NUMBER")

    def rootOutputDir

    if (workspace && buildNumber) {
        rootOutputDir = new File("${workspace}/${buildNumber}/tc_data")
    } else {
        def basePath = new File(project.path).parentFile.path
        rootOutputDir = new File("${basePath}/tc_data")
    }

    for (step in testSteps) {

        if (step == context.currentStep) {
            continue
        }

        def testStepName = step.getName()

        if (step.isDisabled()) {
            log.info "Skipping disabled step: ${testStepName}"
            continue
        }

        def stepType = step.getClass().getSimpleName()
        def isApiStep = stepType in ["RestTestRequestStep", "HttpTestRequestStep"]

        boolean success = false

        def result = step.run(testRunner, context)

        if (result.status.toString() == passedString) {
            success = true
        } else if (isApiStep) {
            def responseContent = result.responseContent ?: ""
            def isTimeout = responseContent.toLowerCase().contains(timeoutString)

            int attempt = 2

            while (isTimeout && attempt <= maxAttempts) {

                log.warn "Timeout on step: ${testStepName}, Attempt: ${attempt}"

                sleep(retryDelay)

                result = step.run(testRunner, context)
                responseContent = result.responseContent ?: ""
                isTimeout = responseContent.toLowerCase().contains(timeoutString)

                if (result.status.toString() == passedString) {
                    success = true
                    break
                }

                attempt++
            }
        }

        if (isApiStep) {
            def requestContent = step.testRequest?.requestContent
            def responseContent = result.responseContent ?: ""

            def safeTestSuiteName = testSuite.name.replaceAll(/[\\\/:*?"<>|]/, "_")
            def safeTestCaseName = testCase.name.replaceAll(/[\\\/:*?"<>|]/, "_")
            def safeTestStepName = testStepName.replaceAll("[^a-zA-Z0-9._-]", "_")

            def requestDir = new File(rootOutputDir, "${safeTestSuiteName}/${safeTestCaseName}/requests")
            def responseDir = new File(rootOutputDir, "${safeTestSuiteName}/${safeTestCaseName}/responses")

            requestDir.mkdirs()
            responseDir.mkdirs()

            if (requestContent?.trim()) {
                new File(requestDir, "${safeTestStepName}.json").write(requestContent, "UTF-8")
            }

            if (responseContent?.trim()) {
                new File(responseDir, "${safeTestStepName}.json").write(responseContent, "UTF-8")
            }
        }

        if (!success) {
            testRunner.fail("Test failed at step: ${testStepName}")
            return
        }
    }
}

project.metaClass.sharedScripts = [:]
project.sharedScripts.runWithRetry = runWithRetry
