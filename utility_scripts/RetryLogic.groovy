try {
    def runWithRetry = context.testCase.testSuite.project.sharedScripts.runWithRetry

    runWithRetry(testRunner, context)
    testRunner.gotoStep(context.testCase.getTestStepCount())
} catch (Exception e) {
    log.error "Error in 'retryLogic' testStep: ${e.message}"
}
