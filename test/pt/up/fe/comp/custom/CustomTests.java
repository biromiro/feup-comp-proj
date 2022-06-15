/**
 * Copyright 2022 SPeCS.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
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
import pt.up.fe.specs.util.SpecsIo;

import java.util.Arrays;
import java.util.List;

public class CustomTests {

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

    @Test
    public void overloading1() {
        CpUtils.runJasmin(getJasminResult("Overloading1"), getResults(Arrays.asList(
                101, 10201, 1030301, -5, 5, 10303010, 103030177, -10303010, 810
        )));
    }

    @Test
    public void overloading2() {
        CpUtils.runJasmin(getJasminResult("Overloading2"), getResults(Arrays.asList(
                5705, -57
        )));
    }

    @Test
    public void overloading3() {
        TestUtils.mustFail(getSemanticsResult("Overloading3.fail").getReports());
    }

    @Test
    public void recursion1() {
        CpUtils.runJasmin(getJasminResult("Recursion1"), getResults(List.of(120)));
    }

    @Test
    public void recursion2() {
        CpUtils.runJasmin(getJasminResult("Recursion2"), getResults(List.of(55)));
    }

    @Test
    public void recursion3() {
        CpUtils.runJasmin(getJasminResult("Recursion3"), getResults(Arrays.asList(0, 1, 1, 0)));
    }

    @Test
    public void variablesAndFields1() {
        CpUtils.runJasmin(getJasminResult("VariablesAndFields1"), getResults(Arrays.asList(
                100, 101, 102, 103, 104, 100, 2, 3
        )));
    }

    @Test
    public void variablesAndFields2() {
        CpUtils.runJasmin(getJasminResult("VariablesAndFields2"), getResults(Arrays.asList(
                2, 2, -2
        )));
    }

    @Test
    public void variablesAndFields3() {
        CpUtils.runJasmin(getJasminResult("VariablesAndFields3"), getResults(Arrays.asList(
                3, 2
        )));
    }

    @Test
    public void complexExpressions1() {
        CpUtils.runJasmin(getJasminResult("ComplexExpressions1"), getResults(Arrays.asList(
                6, 0
        )));
    }

    @Test
    public void complexExpressions2() {
        CpUtils.runJasmin(getJasminResult("ComplexExpressions2"), getResults(Arrays.asList(
                6, 0
        )));
    }

    @Test
    public void complexExpressions3() {
        CpUtils.runJasmin(getJasminResult("ComplexExpressions3"), getResults(Arrays.asList(
                27, 33, 94, 62, 0, 1, 5, 12, 2, 12, 1, 1, 5, 3, 2, 12
        )));
    }

    @Test
    public void complexExpressions4() {
        CpUtils.runJasmin(getJasminResult("ComplexExpressions4"), getResults(Arrays.asList(
                1, 2, 5, 5
        )));
    }

    @Test
    public void complexExpressions5() {
        CpUtils.runJasmin(getJasminResult("ComplexExpressions5"), getResults(Arrays.asList(
                0, 3, 5
        )));
    }

    @Test
    public void complexExpressions6() {
        CpUtils.runJasmin(getJasminResult("ComplexExpressions6"), getResults(List.of(
                4
        )));
    }

    @Test
    public void complexExpressions7() {
        CpUtils.runJasmin(getJasminResult("ComplexExpressions7"), getResults(List.of(
                7
        )));
    }

    @Test
    public void complexExpressions8() {
        CpUtils.runJasmin(getJasminResult("ComplexExpressions8"), getResults(List.of(
                43
        )));
    }

    @Test
    public void inheritance1() {
        CpUtils.runJasmin(getJasminResult("Inheritance1"), getResults(Arrays.asList(
                2, 2
        )));
    }

    @Test
    public void inheritance2() {
        CpUtils.runJasmin(getJasminResult("Inheritance2"), "Hello, World!");
    }

    @Test
    public void inheritance3() {
        TestUtils.mustFail(getSemanticsResult("Inheritance3.fail").getReports());
    }

    @Test
    public void complexArrayAccess1() {
        CpUtils.runJasmin(getJasminResult("ComplexArrayAccess1"), getResults(Arrays.asList(
                1, 2, 3, 4, 5
        )));
    }

    @Test
    public void ifElse1() {
        CpUtils.runJasmin(getJasminResult("IfElse1"), getResults(Arrays.asList(
                1, 0, 1, 0, 2, 0, 2, 12, 2
        )));
    }

    @Test
    public void ifElse2() {
        CpUtils.runJasmin(getJasminResult("IfElse2"), getResults(Arrays.asList(
                0, 1, 1, 1, 0, 0
        )));
    }

    @Test
    public void while1() {
        CpUtils.runJasmin(getJasminResult("While1"), getResults(Arrays.asList(
                0, 0, 1, 0, 1, 2, 0, 1, 2, 3
        )));
    }

    @Test
    public void while2() {
        CpUtils.runJasmin(getJasminResult("While2"), getResults(Arrays.asList(
                100, 200, 300, 400
        )));
    }

    @Test
    public void while3() {
        CpUtils.runJasmin(getJasminResult("While3"), getResults(List.of()));
    }

    @Test
    public void while4() {
        CpUtils.runJasmin(getJasminResult("While4"), getResults(Arrays.asList(
                0, 1, 2, 3, 4, 50, 5, 16, 8, 4, 2
        )));
    }
}
