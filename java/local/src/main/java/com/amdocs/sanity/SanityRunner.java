// package com.amdocs.sanity;

// import java.io.IOException;
// import java.nio.file.Files;
// import java.nio.file.Path;
// import java.nio.file.Paths;

// public final class SanityRunner {

//     private static final String PROCESSED_DIR = "processed_tc_data";
//     private static final String FAILED_DIR = "failed_tc_data";

//     private SanityRunner() {
//     }

//     public static void main(String[] args) {
//         int argsForBasicSanity = 4;
//         int argsForExtendedSanity = 10;
//         if (args.length != argsForBasicSanity && args.length != argsForExtendedSanity) {
//             System.err.println(
//                     "Usage: java SanityRunner <input_junit_report_dir> <input_tc_data_dir> <output_dir> <job_name>");
//             System.err.println(
//                     "Usage: java SanityRunner <input_junit_report_dir> <input_tc_data_dir> <output_dir> <job_name> <input_log_file> <flows_separated_by_|> <project (OE/CO)> <DMP> <env> <tester_name>");
//             System.exit(1);
//         }

//         String junitReportDir = args[0];
//         String testCaseDataDir = args[1];
//         String outputDir = args[2];
//         String jobName = args[3];
//         Path processedTestCaseDataDir = Paths.get(outputDir).resolve(PROCESSED_DIR);
//         Path failedTestCaseDataDir = Paths.get(outputDir).resolve(FAILED_DIR);

//         try {
//             Files.createDirectories(processedTestCaseDataDir);
//         } catch (IOException e) {
//             System.err.println("Failed to create output directory: " + e.getMessage());
//         }

//         try {
//             Files.createDirectories(failedTestCaseDataDir);
//         } catch (IOException e) {
//             System.err.println("Failed to create output directory: " + e.getMessage());
//         }

//         int exitCode = 0;

//         try {
//             ReadyAPIReportGenerator.ReportSummary summary = ReadyAPIReportGenerator.generateReport(
//                     junitReportDir,
//                     outputDir,
//                     jobName,
//                     testCaseDataDir,
//                     System.out::println);

//             System.out.println("\nSummary:");
//             System.out.println("  Total Tests: " + summary.totalTests);
//             System.out.println("  Passed: " + summary.totalPassed);
//             System.out.println("  Failed: " + summary.totalFailed);
//             System.out.println("  Total Time: " + String.format("%.3f", summary.totalTime) + "s");

//             ProcessAPIData.processFiles(Paths.get(testCaseDataDir), processedTestCaseDataDir);

//             CopyFailedResponses.copy(processedTestCaseDataDir, failedTestCaseDataDir);
//             System.out.println("Copied Failed Test Case Data!");
//         } catch (Exception e) {
//             exitCode = 1;
//             e.printStackTrace();
//         }

//         if (args.length == argsForExtendedSanity) {
//             Path originalPath = Paths.get(args[4]);
//             String originalFileName = originalPath.getFileName().toString();
//             Path excelPath = Paths.get(outputDir, "Exceptions.xlsx");
//             String[] flows = args[5].split("\\|");

//             int project = -1;
//             if (args[6].equalsIgnoreCase("OE")) {
//                 project = 1;
//             } else if (args[6].equalsIgnoreCase("CO")) {
//                 project = 2;
//             } else {
//                 System.err.println("Project must be OE or CO");
//                 System.exit(1);
//             }

//             String dmp = args[7];
//             String env = args[8];
//             String tester = args[9];

//             try {
//                 for (String flow : flows) {
//                     Path logFilePath = originalPath.resolveSibling(flow.toUpperCase() + originalFileName);
//                     LogsToExcel.log(logFilePath, excelPath, flow, project, dmp, env, tester);
//                 }
//                 System.out.println("Logs processed and saved to Excel!");
//             } catch (IOException e) {
//                 exitCode = 1;
//                 e.printStackTrace();
//             }
//         }

//         System.exit(exitCode);
//     }
// }

package com.amdocs.sanity;

import java.io.FileInputStream;
import java.nio.file.*;
import java.util.*;

public final class SanityRunner {

    private SanityRunner() {
    }

    public static void main(String[] args) {

        Map<String, String> params = parseArgs(args);

        Properties config = new Properties();
        try (FileInputStream fis = new FileInputStream(params.get("config"))) {
            config.load(fis);
        } catch (Exception e) {
            System.err.println("Failed to load config: " + e.getMessage());
            System.exit(1);
        }

        Path buildDir = Paths.get(params.get("buildDir"));

        String junitDirName = config.getProperty("dir.junit");
        String tcDataDirName = config.getProperty("dir.tcdata");
        String processedDirName = config.getProperty("dir.processed");
        String failedDirName = config.getProperty("dir.failed");
        String errorDirName = config.getProperty("dir.errorlogs");

        Path junitDir = buildDir.resolve(junitDirName);
        Path tcDataDir = buildDir.resolve(tcDataDirName);
        Path processedDir = buildDir.resolve(processedDirName);
        Path failedDir = buildDir.resolve(failedDirName);
        Path errorDir = buildDir.resolve(errorDirName);

        int exitCode = 0;

        try {
            Files.createDirectories(processedDir);
            Files.createDirectories(failedDir);

            ReadyAPIReportGenerator.ReportSummary summary = ReadyAPIReportGenerator.generateReport(
                    junitDir.toString(),
                    buildDir.toString(),
                    params.get("jobName"),
                    tcDataDir.toString(),
                    System.out::println);

            System.out.println("\nSummary:");
            System.out.println("  Total Tests: " + summary.totalTests);
            System.out.println("  Passed: " + summary.totalPassed);
            System.out.println("  Failed: " + summary.totalFailed);
            System.out.println("  Total Time: " + String.format("%.3f", summary.totalTime) + "s");

            ProcessAPIData.processFiles(tcDataDir, processedDir);
            System.out.println("Processed Test Case Data!");

            CopyFailedResponses.copy(processedDir, failedDir);
            System.out.println("Copied Failed Test Cases' Data!");
        } catch (Exception e) {
            exitCode = 1;
            e.printStackTrace();
        }

        if ("EXTENDED".equalsIgnoreCase(params.get("type"))) {
            String flows = params.getOrDefault("flows", config.getProperty("flows"));
            String[] flowArray = flows.split("\\|");

            int project = Integer.parseInt(config.getProperty("project." + params.get("project").toLowerCase()));

            Path excelPath = buildDir.resolve(config.getProperty("dir.exceptions"));

            try {
                for (String flow : flowArray) {
                    Path logFile = errorDir.resolve(flow.toUpperCase() + ".err");
                    LogsToExcel.log(logFile, excelPath, flow, project,
                            params.get("dmp"),
                            params.get("env"),
                            params.get("tester"));
                }
                System.out.println("Logs processed and saved to Excel!");
            } catch (Exception e) {
                exitCode = 1;
                e.printStackTrace();
            }
        }

        try {
            ArtifactPackager.zipConfiguredArtifacts(buildDir, config);
            System.out.println("Artifacts packaged successfully!");
        } catch (Exception e) {
            exitCode = 1;
            e.printStackTrace();
        }

        System.exit(exitCode);
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < args.length; i += 2) {
            map.put(args[i].replace("--", ""), args[i + 1]);
        }
        return map;
    }
}