package com.amdocs.sanity;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

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
        String failedDirName = config.getProperty("dir.failed");
        String errorDirName = config.getProperty("dir.errorlogs");

        Path junitDir = buildDir.resolve(junitDirName);
        Path tcDataDir = buildDir.resolve(tcDataDirName);
        Path failedDir = buildDir.resolve(failedDirName);
        Path errorDir = buildDir.resolve(errorDirName);

        int exitCode = 0;

        try {
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

            Files.createDirectories(failedDir);
            CopyFailedTestCaseData.copy(tcDataDir, failedDir);

            System.out.println("Copied Failed Test Cases' Data!");
        } catch (Exception e) {
            exitCode = 1;
            e.printStackTrace();
        } finally {
            try {
                if (Files.exists(failedDir) && isDirectoryEmpty(failedDir)) {
                    Files.delete(failedDir);
                }
            } catch (IOException cleanupEx) {
                System.err.println("Failed to clean up failedDir: " + cleanupEx.getMessage());
            }
        }

        if ("EXTENDED".equalsIgnoreCase(params.get("type"))) {
            String[] flows = params.getOrDefault("flows", config.getProperty("flows")).split("\\|");

            int project = Integer.parseInt(config.getProperty("project." + params.get("project").toLowerCase()));

            Path excelPath = buildDir.resolve(config.getProperty("dir.exceptions"));

            for (String flow : flows) {
                Path logFile = errorDir.resolve(flow.toUpperCase() + ".err");

                if (!Files.exists(logFile)) {
                    continue;
                }

                try {
                    LogsToExcel.log(logFile, excelPath, flow, project,
                            params.get("dmp"),
                            params.get("env"),
                            params.get("tester"));
                } catch (IllegalArgumentException e) {
                    System.err.println("Invalid argument provided for flow: " + flow);
                } catch (IOException e) {
                    exitCode = 1;
                    e.printStackTrace();
                }
            }
            System.out.println("Logs processed and saved to Excel!");
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

    private static boolean isDirectoryEmpty(Path dir) throws IOException {
        try (Stream<Path> stream = Files.list(dir)) {
            return !stream.findFirst().isPresent();
        }
    }
}