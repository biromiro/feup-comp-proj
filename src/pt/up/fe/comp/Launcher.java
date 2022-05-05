package pt.up.fe.comp;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import pt.up.fe.comp.analysis.Analyser;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.parse.Parser;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsSystem;

public class Launcher {

    public static void main(String[] args) {
        SpecsSystem.programStandardInit();
        SpecsLogs.info("Executing with args: " + Arrays.toString(args));

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
                        throw new RuntimeException("-r requires an argument");
                    }
                    config.put("registerAllocation", split[1]);
                }
                case "-i" -> {
                    if (split.length != 2) {
                        throw new RuntimeException("-i requires an argument");
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

        // Open and read input file
        if (config.get("inputFile").isEmpty()) {
            throw new RuntimeException("Required flag -i not provided");
        }
        File inputFile = new File(config.get("inputFile"));
        if (!inputFile.isFile()) {
            throw new RuntimeException("Expected a path to an existing input file, got '" + config.get("inputFile") + "'.");
        }
        String input = SpecsIo.read(inputFile);

        // Parse stage
        Parser parser = new Parser();
        JmmParserResult parserResult = parser.parse(input, config);
        //System.out.println(parserResult.getRootNode().toTree());
        //System.out.println(parserResult.getRootNode().toJson());
        TestUtils.noErrors(parserResult.getReports());

        // Analysis stage
        Analyser analyser =  new Analyser();
        JmmSemanticsResult analysisResult = analyser.semanticAnalysis(parserResult);
        //System.out.println(analysisResult.getSymbolTable().print());
        //System.out.println(analysisResult.getRootNode().toTree());
        TestUtils.noErrors(analysisResult.getReports());

        // ... add remaining stages
    }

}
