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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class CustomTestsOptimizations {

    static JasminResult getJasminResult(String filename) {
        return TestUtils.backend(SpecsIo.getResource("fixtures/custom/" + filename + ".jmm"));
    }

    static String getResults(List<Integer> results) {
        var sb = new StringBuilder();
        for (var r : results) {
            sb.append("Result: ").append(r).append("\n");
        }
        return sb.toString().trim();
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

    static JasminResult getJasminResultOptBestReg(String filename) {
        Map<String, String> config = new HashMap<>();
        config.put("optimize", "true");
        config.put("registerAllocation", "0");
        return TestUtils.backend(SpecsIo.getResource("fixtures/custom/" + filename + ".jmm"), config);
    }

    public void eliminationOfUnnecessaryGotosHelper(String filename, int maxIf, int maxGoto, String expected, boolean optDif) {
        JasminResult original = getJasminResult(filename);
        JasminResult optimized = getJasminResultOpt(filename);

        CpUtils.runJasmin(optimized, expected);

        if (optDif) {
            CpUtils.assertNotEquals("Expected code to change with -o flag\n\nOriginal code:\n" + original.getJasminCode(),
                    original.getJasminCode(), optimized.getJasminCode(),
                    optimized);
        }

        var ifOccurOpt = CpUtils.countOccurences(optimized, "if");
        var gotoOccurOpt = CpUtils.countOccurences(optimized, "goto");

        CpUtils.assertEquals("Expected exactly " + maxIf + " if instruction", maxIf, ifOccurOpt, optimized);
        CpUtils.assertEquals("Expected exactly " + maxGoto + " goto instructions", maxGoto, gotoOccurOpt,
                optimized);
    }

    public void eliminationOfUnnecessaryGotosHelper(String filename, int maxIf, String expected) {
        eliminationOfUnnecessaryGotosHelper(filename, maxIf, 0, expected, true);
    }

    public void eliminationOfUnnecessaryGotosHelper(String filename, int maxIf, String expected, boolean optDiff) {
        eliminationOfUnnecessaryGotosHelper(filename, maxIf, 0, expected, optDiff);
    }

    public void deadCodeHelper(String filename, List<String> words, String expected) {
        JasminResult original = getJasminResult(filename);
        JasminResult optimized = getJasminResultOptBestReg(filename);

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

    public void constFoldAndPropHelper(String filename, String codeExpected, String expected, boolean optDiff) {
        JasminResult original = getJasminResult(filename);
        JasminResult optimized = getJasminResultOptBestReg(filename);

        CpUtils.runJasmin(optimized, expected);

        if (optDiff) {
            CpUtils.assertNotEquals("Expected code to change with -o flag\n\nOriginal code:\n" + original.getJasminCode(),
                    original.getJasminCode(), optimized.getJasminCode(),
                    optimized);
        }

        CpUtils.matches(optimized, codeExpected);
    }

    public void constFoldAndPropHelper(String filename, String codeExpected, String expected) {
        constFoldAndPropHelper(filename, codeExpected, expected, true);
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
        eliminationOfUnnecessaryGotosHelper("EliminationOfUnnecessaryGotos4.noDiff", 2, getResults(Arrays.asList(
                0, 1
        )), false);
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
    public void eliminationOfUnnecessaryGotos7() {
        eliminationOfUnnecessaryGotosHelper("EliminationOfUnnecessaryGotos7.noDiff", 2, getResults(List.of()), false);
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

    @Test
    public void deadCode9() {
        deadCodeHelper("DeadCode9", Arrays.asList("777501", "777503"), getResults(Arrays.asList(
                777502, 777504
        )));
    }

    @Test
    public void deadCode10() {
        deadCodeHelper("DeadCode10", "777501", getResults(List.of(777502)));
    }

    @Test
    public void deadCode11() {
        deadCodeHelper("DeadCode11", Arrays.asList("777501", "777503", "777504", "777505"), getResults(Arrays.asList(
                777502, 777506
        )));
    }

    @Test
    public void unusedVariable1() {
        deadCodeHelper("UnusedVariable1", "777501", getResults(List.of()));
    }

    @Test
    public void unusedVariable2() {
        deadCodeHelper("UnusedVariable2", "777501", getResults(List.of(777502)));
    }

    @Test
    public void unusedVariable3() {
        deadCodeHelper("UnusedVariable3", "777502", getResults(List.of(777501)));
    }

    @Test
    public void unusedVariable4() {
        deadCodeHelper("UnusedVariable4", "777501", getResults(List.of(777502)));
    }

    @Test
    public void unusedVariable5() {
        deadCodeHelper("UnusedVariable5", "777502", getResults(List.of(777501)));
    }

    @Test
    public void unusedVariable6() {
        deadCodeHelper("UnusedVariable6", "777501", getResults(List.of(777502)));
    }

    @Test
    public void unusedVariable7() {
        deadCodeHelper("UnusedVariable7", "777505", getResults(Arrays.asList(
                777504, 777506)));
    }

    @Test
    public void unusedVariable8() {
        deadCodeHelper("UnusedVariable8", "777505", getResults(List.of(777506)));
    }

    @Test
    public void constProp1() {
        constFoldAndPropHelper("ConstProp1", "(bipush|sipush|ldc) 10\\s+invokevirtual ConstProp/foo\\(I\\)I", getResults(List.of()));
    }

    @Test
    public void constProp2() {
        constFoldAndPropHelper("ConstProp2",
                "(?s)((iconst_5\\s+iload_\\d+)|(iload_\\d+\\s+iconst_5))\\s+if_icmplt\\s+.*((iconst_5\\s+iload_\\d+)|(iload_\\d+\\s+iconst_5))\\s+if_icmplt\\s+.*iconst_5\\s+invokestatic\\s+ioPlus/printResult\\(I\\)V\\s+.*iconst_5\\s+invokestatic\\s+ioPlus/printResult\\(I\\)V",
                getResults(List.of(5)));
    }

    @Test
    public void constProp3() {
        constFoldAndPropHelper("ConstProp3", "iconst_5\\s+invokestatic ioPlus/printResult\\(I\\)V\\s+(bipush|sipush|ldc) 6\\s+invokestatic ioPlus/printResult\\(I\\)V", getResults(Arrays.asList(
                5, 6
        )));
    }

    @Test
    public void constProp4() {
        constFoldAndPropHelper("ConstProp4.noDiff",
                "(?s)iconst_5\\s+istore_\\d+.*(bipush|sipush|ldc) 6\\s+istore_\\d+.*iload_\\d+\\s+ireturn",
                getResults(List.of(5)), false);
    }

    @Test
    public void constProp5() {
        constFoldAndPropHelper("ConstProp5",
                "iconst_5\\s+ireturn",
                getResults(List.of(5)));
    }

    @Test
    public void constProp6() {
        constFoldAndPropHelper("ConstProp6.noDiff",
                "(bipush|sipush|ldc) 50\\s+istore_\\d+",
                getResults(Arrays.asList(50, 1, 2, 4)), false);
    }

    @Test
    public void constProp7() {
        constFoldAndPropHelper("ConstProp7",
                "(bipush|sipush|ldc) 500\\s+invokestatic ioPlus/printResult\\(I\\)V",
                getResults(Arrays.asList(50, 50, 50, 500, 500)));
    }

    @Test
    public void constProp8() {
        constFoldAndPropHelper("ConstProp8.noDiff",
                "(bipush|sipush|ldc) 50\\s+istore(_| )\\d+",
                getResults(Arrays.asList(50, 50, 50, 4)));
    }


    @Test
    public void constProp9() {
        constFoldAndPropHelper("ConstProp9",
                "(?s)(bipush|sipush|ldc) 50\\s+invokestatic ioPlus/printResult\\(I\\)V.*(bipush|sipush|ldc) 50\\s+invokestatic ioPlus/printResult\\(I\\)V",
                getResults(Arrays.asList(50)));
    }

    @Test
    public void constFold1() {
        constFoldAndPropHelper("ConstFold1",
                "(bipush|sipush|ldc) 17\\s+ireturn",
                getResults(List.of(17)));
    }

    @Test
    public void constFold2() {
        constFoldAndPropHelper("ConstFold2",
                "iconst_1\\s+ireturn",
                getResults(List.of(1)));
    }

    @Test
    public void constFold3() {
        constFoldAndPropHelper("ConstFold3",
                "(?s)(bipush|sipush|ldc) 120.*(bipush|sipush|ldc) 40.*(bipush|sipush|ldc) 13.*",
                getResults(List.of(0)));
    }

    @Test
    public void constFold4() {
        constFoldAndPropHelper("ConstFold4",
                "(?s)iconst_0\\s+iload_\\d+\\s+imul\\s+.*iload_\\d+\\s+imul",
                getResults(List.of(0)));
    }

    @Test
    public void constFold5() {
        constFoldAndPropHelper("ConstFold5",
                "(bipush|sipush|ldc) 15",
                getResults(List.of(16)));
    }

    @Test
    public void constFoldAndProp1() {
        constFoldAndPropHelper("ConstFoldAndProp1",
                ".limit stack \\d+\\s+.limit locals \\d+\\s+iconst_3\\s+ireturn",
                getResults(List.of(3)));
    }

    @Test
    public void constFoldAndProp2() {
        constFoldAndPropHelper("ConstFoldAndProp2",
                ".limit stack \\d+\\s+.limit locals \\d+\\s+iconst_0\\s+ireturn",
                getResults(List.of(0)));
    }
}
