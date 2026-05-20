import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.HashMap;

public class LogSearch {

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java LogSearch <input_log_file> <enable_unique_in_session_file:true/false>");
            return;
        }

        String fileName = args[0].substring(0, args[0].lastIndexOf("."));
        boolean enableUniqueInSession = Boolean.parseBoolean(args[1]);

        try (BufferedReader br = new BufferedReader(new FileReader(args[0]), 32 * 1024);
                BufferedWriter uniqOverall = new BufferedWriter(new FileWriter(fileName + ".err"));) {
            if (enableUniqueInSession) {
                try (BufferedWriter uniqInSesh = new BufferedWriter(
                        new FileWriter(fileName + "_uniq_sesh.err"));) {
                    findAndLogErrors(br, uniqOverall, uniqInSesh);
                }
            } else {
                findAndLogErrors(br, uniqOverall, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    private static void findAndLogErrors(BufferedReader br, BufferedWriter uniqOverall,
            BufferedWriter uniqInSesh) throws IOException {

        HashSet<String> errors = new HashSet<>();
        HashMap<String, HashSet<String>> sessionMap = null;
        if (uniqInSesh != null) {
            sessionMap = new HashMap<>();
        }

        String line1 = br.readLine();
        String line2 = br.readLine();
        String line3 = br.readLine();
        while (line3 != null) {
            if (line1.contains("<Error>")) {
                int idx = line1.indexOf("Session Id=");
                StringBuilder idSb = new StringBuilder();
                for (int i = idx + 11; i < line1.length(); i++) {
                    char ch = line1.charAt(i);
                    if (!Character.isDigit(ch)) {
                        break;
                    }

                    idSb.append(ch);
                }
                String id = idSb.toString();

                // Check if rule error or non-rule error
                if (line2.contains("RULE ERROR: The rule with GROUP ID =")) {
                    if (!errors.contains(line2)) {
                        errors.add(line2);
                        String error = generateErrorLines(br, line1, line2, line3);
                        uniqOverall.write(error);

                        if (uniqInSesh != null) {
                            HashSet<String> err;
                            if (sessionMap.containsKey(id)) {
                                err = sessionMap.get(id);
                            } else {
                                err = new HashSet<>();
                                sessionMap.put(id, err);
                            }
                            err.add(line2);
                            uniqInSesh.write(error);
                        }
                    } else if (uniqInSesh != null) {
                        if (!sessionMap.containsKey(id)) {
                            HashSet<String> err = new HashSet<>();
                            sessionMap.put(id, err);
                            err.add(line2);
                            uniqInSesh.write(generateErrorLines(br, line1, line2, line3));
                        } else {
                            HashSet<String> err = sessionMap.get(id);
                            if (!err.contains(line2)) {
                                err.add(line2);
                                uniqInSesh.write(generateErrorLines(br, line1, line2, line3));
                            }
                        }
                    }
                } else {
                    String errorIdentifier;
                    if (line2.contains("Exception")) {
                        errorIdentifier = line2;
                    } else if (line3.contains("Exception")) {
                        if (line2.trim().length() > 0) {
                            errorIdentifier = line2.split("line")[0].trim();
                        } else {
                            errorIdentifier = line3;
                        }
                    } else {
                        int idxPipe = line1.lastIndexOf("|");
                        errorIdentifier = line1.substring(idxPipe + 1).trim();
                    }

                    if (!errors.contains(errorIdentifier)) {
                        errors.add(errorIdentifier);
                        String error = generateErrorLines(br, line1, line2, line3);
                        uniqOverall.write(error);

                        if (uniqInSesh != null) {
                            HashSet<String> err;
                            if (sessionMap.containsKey(id)) {
                                err = sessionMap.get(id);
                            } else {
                                err = new HashSet<>();
                                sessionMap.put(id, err);
                            }
                            err.add(errorIdentifier);
                            uniqInSesh.write(error);
                        }
                    } else if (uniqInSesh != null) {
                        if (!sessionMap.containsKey(id)) {
                            HashSet<String> err = new HashSet<>();
                            sessionMap.put(id, err);
                            err.add(errorIdentifier);
                            uniqInSesh.write(generateErrorLines(br, line1, line2, line3));
                        } else {
                            HashSet<String> err = sessionMap.get(id);
                            if (!err.contains(errorIdentifier)) {
                                err.add(errorIdentifier);
                                uniqInSesh.write(generateErrorLines(br, line1, line2, line3));
                            }
                        }
                    }
                }

                line2 = br.readLine();
                line3 = br.readLine();
            } else if (uniqInSesh != null && line3.contains("Rule Ended [ Unsuccessfully ]")) {
                uniqInSesh.write(line1);
                uniqInSesh.newLine();
                uniqInSesh.write(line2);
                uniqInSesh.newLine();
                uniqInSesh.write(line3);
                uniqInSesh.newLine();
                while ((line1 = br.readLine()) != null && !line1.isEmpty()) {
                    uniqInSesh.write(line1);
                    uniqInSesh.newLine();
                }

                line2 = br.readLine();
                line3 = br.readLine();
            }

            line1 = line2;
            line2 = line3;
            line3 = br.readLine();
        }
    }

    private static String generateErrorLines(BufferedReader br, String line1, String line2, String line3)
            throws IOException {
        StringBuilder error = new StringBuilder();
        String newLine = System.lineSeparator();

        error.append(line1 + newLine);

        int open = calculateOpenTags(line1);
        if (open > 0) {
            error.append(line2 + newLine);
            open += calculateOpenTags(line2);

            if (open > 0) {
                error.append(line3 + newLine);
                open += calculateOpenTags(line3);
                while (open > 0) {
                    String ln = br.readLine();
                    error.append(ln + newLine);
                    open += calculateOpenTags(ln);
                }
            }
        }
        error.append(newLine);

        return error.toString();
    }

    private static int calculateOpenTags(String line) {
        int open = 0;
        for (byte b : line.getBytes()) {
            if (b == '<') {
                open++;
            } else if (b == '>') {
                open--;
            }
        }

        return open;
    }
}