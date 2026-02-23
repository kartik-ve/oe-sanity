package com.amdocs.sanity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class SanityRunner {

    private SanityRunner() {
    }

    public static void main(String[] args) {
        int argsForBasicSanity = 5;
        int argsForExtendedSanity = 11;
        if (args.length != argsForBasicSanity && args.length != argsForExtendedSanity) {
            System.err.println(
                    "Usage: java SanityRunner <input_junit_report_dir> <input_tc_data_dir> <output_dir> <job_name>");
            System.err.println(
                    "Usage: java SanityRunner <input_junit_report_dir> <input_tc_data_dir> <output_dir> <job_name> <input_log_file> <flows_separated_by_|> <project (OE/CO)> <DMP> <env> <tester_name>");
            System.exit(1);
        }

        String junitReportDir = args[0];
        String testCaseDataDir = args[1];
        String outputDir = args[2];
        String jobName = args[3];
        Path processedTestCaseDataDir = Paths.get(outputDir).resolve("processed_tc_data");
        Path failedTestCaseDataDir = Paths.get(outputDir).resolve("failed_tc_data");
        
        try {
            Files.createDirectories(processedTestCaseDataDir);
        } catch (IOException e) {
            System.err.println("Failed to create output directory: " + e.getMessage());
        }

        try {
            Files.createDirectories(failedTestCaseDataDir);
        } catch (IOException e) {
            System.err.println("Failed to create output directory: " + e.getMessage());
        }

        int exitCode = 0;

        try {
            ReadyAPIReportGenerator.ReportSummary summary = ReadyAPIReportGenerator.generateReport(
                    junitReportDir,
                    outputDir,
                    jobName,
                    testCaseDataDir,
                    System.out::println);

            System.out.println("\nSummary:");
            System.out.println("  Total Tests: " + summary.totalTests);
            System.out.println("  Passed: " + summary.totalPassed);
            System.out.println("  Failed: " + summary.totalFailed);
            System.out.println("  Total Time: " + String.format("%.3f", summary.totalTime) + "s");

            ProcessAPIData.processFiles(Paths.get(testCaseDataDir), processedTestCaseDataDir);

            CopyFailedResponses.copy(processedTestCaseDataDir, failedTestCaseDataDir);
            System.out.println("Copied Failed Test Case Data!");
        } catch (Exception e) {
            exitCode = 1;
            e.printStackTrace();
        }

        if (args.length == argsForExtendedSanity) {
            Path originalPath = Paths.get(args[4]);
            String originalFileName = originalPath.getFileName().toString();
            Path excelPath = Paths.get(outputDir, "Exceptions.xlsx");
            String[] flows = args[5].split("\\|");
            
            int project = -1;
            if (args[6].equalsIgnoreCase("OE")) {
                project = 1;
            } else if (args[6].equalsIgnoreCase("CO")) {
                project = 2;
            } else {
                System.err.println("Project must be OE or CO");
                System.exit(1);
            }

            String dmp = args[7];
            String env = args[8];
            String tester = args[9];

            try {
                for (String flow : flows) {
                    Path logFilePath = originalPath.resolveSibling(flow.toUpperCase() + originalFileName);
                    LogsToExcel.log(logFilePath, excelPath, flow, project, dmp, env, tester);
                }
                System.out.println("Logs processed and saved to Excel!");
            } catch (IOException e) {
                exitCode = 1;
                e.printStackTrace();
            }
        }

        System.exit(exitCode);
    }
}
