/**
 * Copyright 2022 SPeCS.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. under the License.
 */

package pt.up.fe.comp.custom;

import org.junit.Test;
import pt.up.fe.comp.CpUtils;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsStrings;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class CustomTestsOptimizations {

    static JasminResult getJasminResult(String filename) {
        return TestUtils.backend(SpecsIo.getResource("fixtures/custom/" + filename + ".jmm"));
    }

    static JmmSemanticsResult getSemanticsResult(String filename) {
        return TestUtils.analyse(SpecsIo.getResource("fixtures/custom/" + filename + ".jmm"));
    }

    static String getResults(List<Integer> results) {
        var sb = new StringBuilder();
        for (var r : results) {
            sb.append("Result: ").append(r).append("\n");
        }
        return sb.toString().trim();
    }


    static OllirResult getOllirResult(String filename) {
        return TestUtils.optimize(SpecsIo.getResource("fixtures/custom/" + filename + ".jmm"));
    }

    static JasminResult getJasminResultOpt(String filename) {
        Map<String, String> config = new HashMap<>();
        config.put("optimize", "true");
        return TestUtils.backend(SpecsIo.getResource("fixtures/custom/" + filename + ".jmm"), config);
    }

    static JasminResult getJasminResultReg(String filename, int numReg) {
        Map<String, String> config = new HashMap<>();
        config.put("registerAllocation", String.valueOf(numReg));
        return TestUtils.backend(SpecsIo.getResource("fixtures/custom/" + filename + ".jmm"), config);
    }

    public void eliminationOfUnnecessaryGotosHelper(String filename, int maxIf, int maxGoto, String expected) {
        JasminResult original = getJasminResult(filename);
        JasminResult optimized = getJasminResultOpt(filename);

        CpUtils.runJasmin(optimized, expected);

        CpUtils.assertNotEquals("Expected code to change with -o flag\n\nOriginal code:\n" + original.getJasminCode(),
                original.getJasminCode(), optimized.getJasminCode(),
                optimized);

        var ifOccurOpt = CpUtils.countOccurences(optimized, "if_");
        var gotoOccurOpt = CpUtils.countOccurences(optimized, "goto");

        CpUtils.assertEquals("Expected exactly " + maxIf + " if instruction", maxIf, ifOccurOpt, optimized);
        CpUtils.assertEquals("Expected exactly " + maxGoto + " goto instructions", maxGoto, gotoOccurOpt,
                optimized);
    }

    public void eliminationOfUnnecessaryGotosHelper(String filename, int maxIf, String expected) {
        eliminationOfUnnecessaryGotosHelper(filename, maxIf, 0, expected);
    }

    public void deadCodeHelper(String filename, List<String> words, String expected) {
        JasminResult original = getJasminResult(filename);
        JasminResult optimized = getJasminResultOpt(filename);

        if (expected != null) {
            CpUtils.runJasmin(optimized, expected);
        }

        CpUtils.assertNotEquals("Expected code to change with -o flag\n\nOriginal code:\n" + original.getJasminCode(),
                original.getJasminCode(), optimized.getJasminCode(),
                optimized);

        for (var word : words) {
            var wordOccurOpt = CpUtils.countOccurences(optimized, word);
            CpUtils.assertEquals("Expected exactly 0 " + word, 0, wordOccurOpt, optimized);
        }

    }

    public void deadCodeHelper(String filename, String word, String expected) {
        deadCodeHelper(filename, List.of(word), expected);
    }

    @Test
    public void eliminationOfUnnecessaryGotos1() {
        eliminationOfUnnecessaryGotosHelper("EliminationOfUnnecessaryGotos1", 1, getResults(Arrays.asList(
                1, 2, 3, 4, 5
        )));
    }

    @Test
    public void eliminationOfUnnecessaryGotos2() {
        eliminationOfUnnecessaryGotosHelper("EliminationOfUnnecessaryGotos2", 1, getResults(Arrays.asList(
                1, 2, 3, 4, 5
        )));
    }

    @Test
    public void eliminationOfUnnecessaryGotos3() {
        eliminationOfUnnecessaryGotosHelper("EliminationOfUnnecessaryGotos3", 0, getResults(List.of()));
    }

    @Test
    public void eliminationOfUnnecessaryGotos4() {
        eliminationOfUnnecessaryGotosHelper("EliminationOfUnnecessaryGotos4", 2, getResults(Arrays.asList(
                0, 1
        )));
    }

    @Test
    public void eliminationOfUnnecessaryGotos5() {
        eliminationOfUnnecessaryGotosHelper("EliminationOfUnnecessaryGotos5", 5, getResults(Arrays.asList(
                0, 0, 1, 0, 1, 2
        )));
    }

    @Test
    public void eliminationOfUnnecessaryGotos6() {
        eliminationOfUnnecessaryGotosHelper("EliminationOfUnnecessaryGotos6", 0, getResults(List.of()));
    }

    @Test
    public void deadCode1() {
        deadCodeHelper("DeadCode1", "777501", getResults(List.of(777502)));
    }

    @Test
    public void deadCode2() {
        deadCodeHelper("DeadCode2", "777501", getResults(List.of(777502)));
    }

    @Test
    public void deadCode3() {
        deadCodeHelper("DeadCode3", "777502", getResults(Arrays.asList(
                777501, 777503
        )));
    }

    @Test
    public void deadCode4() {
        deadCodeHelper("DeadCode4", "777501", getResults(Arrays.asList(
                777502, 777503
        )));
    }

    @Test
    public void deadCode5() {
        deadCodeHelper("DeadCode5", "777502", getResults(Arrays.asList(
                777501, 777503
        )));
    }

    @Test
    public void deadCode6() {
        deadCodeHelper("DeadCode6", "777501", getResults(Arrays.asList(
                777502, 777503
        )));
    }

    @Test
    public void deadCode7() {
        deadCodeHelper("DeadCode7", Arrays.asList("777502", "777503"), getResults(Arrays.asList(
                777501, 777504
        )));
    }

    @Test
    public void deadCode8() {
        deadCodeHelper("DeadCode8", Arrays.asList("777501", "777503"), getResults(Arrays.asList(
                777502, 777504
        )));
    }
}
