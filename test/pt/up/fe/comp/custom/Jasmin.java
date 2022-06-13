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
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsStrings;
import pt.up.fe.specs.util.utilities.LineStream;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class Jasmin {

    static JasminResult getJasminResult(String filename) {
        return TestUtils.backend(SpecsIo.getResource("fixtures/custom/" + filename + ".jmm"));
    }

    static String getResults(List<Integer> results) {
        var sb = new StringBuilder();
        for (var r : results) {
            sb.append("Result: ").append(r).append("\n");
        }
        return sb.toString();
    }

    @Test
    public void overloading1() {
        CpUtils.runJasmin(getJasminResult("Overloading1"), getResults(Arrays.asList(
                101, 10201, 1030301, -5, 5, 10303010, 103030177, -10303010, 810
        )));
    }
}
