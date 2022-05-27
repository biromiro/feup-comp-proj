package pt.up.fe.comp;

import java.io.File;
import java.util.*;

import pt.up.fe.comp.jasmin.MyJasminBackend;
import pt.up.fe.comp.analysis.Analyser;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.ollir.Optimizer;
import pt.up.fe.comp.parse.Parser;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsSystem;

public class Launcher {

    private static Map<String, String> parseCommandArgs(String[] args) {
        // Create config
        Map<String, String> config = new HashMap<>();
        config.put("inputFile", "");
        config.put("optimize", "false");
        config.put("registerAllocation", "-1");
        config.put("debug", "false");

        // Parse command args
        for (String arg: args) {
            String[] split = arg.split("=");

            if (split.length < 1) {
                throw new RuntimeException("Unknown argument: " + arg);
            }

            switch (split[0]) {
                case "-r" -> {
                    if (split.length != 2) {
                        throw new RuntimeException("-r requires an argument (integer >= -1)");
                    }
                    try {
                        if (Integer.parseInt(split[1]) < -1) {
                            throw new RuntimeException("-r requires an integer >= -1");
                        }
                    } catch (NumberFormatException e) {
                        throw new RuntimeException("-r requires an integer >= -1");
                    }
                    config.put("registerAllocation", split[1]);
                }
                case "-i" -> {
                    if (split.length != 2) {
                        throw new RuntimeException("-i requires an argument (input file)");
                    }
                    config.put("inputFile", split[1]);
                }
                case "-o" -> {
                    if (split.length != 1) {
                        throw new RuntimeException("-o does not require an argument");
                    }
                    config.put("optimize", "true");
                }
                case "-d" -> {
                    if (split.length != 1) {
                        throw new RuntimeException("-d does not require an argument");
                    }
                    config.put("debug", "true");
                }
                default -> throw new RuntimeException("Unknown argument: " + arg);
            }
        }

        return config;
    }

    private static String readFile(String path) {
        if (path.isEmpty()) {
            throw new RuntimeException("Required flag -i not provided");
        }
        File inputFile = new File(path);
        if (!inputFile.isFile()) {
            throw new RuntimeException("Expected a path to an existing input file, got '" + path + "'.");
        }

        return SpecsIo.read(inputFile);
    }

    private static void checkErrors(List<Report> reports) {
        Optional<String> errors = reports.stream()
                .filter(report -> report.getType() == ReportType.ERROR)
                .map(report -> {
                    String message = report.getType() + "@" + report.getStage();
                    if (report.getLine() != -1) message += ", line " + report.getLine();
                    if (report.getColumn() != -1) message += ", col " + report.getColumn();
                    message += ": " + report.getMessage();
                    if (report.getException().isPresent()) {
                        message += "\n" + report.getException().get().getMessage();
                    }
                    return message;
                })
                .reduce((a, b) -> a + "\n" + b);

        if (errors.isPresent()) {
            throw new RuntimeException("Error while compiling:\n" + errors.get());
        }
    }

    public static void main(String[] args) {
        SpecsSystem.programStandardInit();
        SpecsLogs.info("Executing with args: " + Arrays.toString(args));

        Map<String, String> config = parseCommandArgs(args);
        if (config.get("debug").equals("true")) {
            System.out.println("Debug mode enabled\n\n");
        }
        String input = readFile(config.get("inputFile"));

        // Parse stage
        JmmParserResult parserResult = new Parser().parse(input, config);
        checkErrors(parserResult.getReports());

        // Analysis stage
        JmmSemanticsResult analysisResult = new Analyser().semanticAnalysis(parserResult);
        checkErrors(analysisResult.getReports());

        // Optimization stage
        Optimizer optimizer = new Optimizer();
        JmmSemanticsResult highLevelOptimizationResult = optimizer.optimize(analysisResult);
        checkErrors(highLevelOptimizationResult.getReports());
        OllirResult ollirResult = optimizer.toOllir(highLevelOptimizationResult);
        checkErrors(ollirResult.getReports());
        OllirResult lowLevelOptimizationResult = optimizer.optimize(ollirResult);
        checkErrors(lowLevelOptimizationResult.getReports());

        // Backend stage
        JasminResult jasminResult = new MyJasminBackend().toJasmin(lowLevelOptimizationResult);
        checkErrors(jasminResult.getReports());
        //jasminResult.compile();

        // Running
        String runResult = jasminResult.run();
    }

}
