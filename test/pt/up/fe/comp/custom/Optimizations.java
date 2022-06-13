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
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsStrings;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Optimizations {


    static OllirResult getOllirResult(String filename) {
        return TestUtils.optimize(SpecsIo.getResource("fixtures/public/cpf/5_optimizations/" + filename));
    }

    static JasminResult getJasminResult(String filename) {
        String resource = SpecsIo.getResource("fixtures/public/cpf/5_optimizations/" + filename);
        return TestUtils.backend(resource);
    }

    static JasminResult getJasminResultOpt(String filename) {
        Map<String, String> config = new HashMap<>();
        config.put("optimize", "true");
        return TestUtils.backend(SpecsIo.getResource("fixtures/public/cpf/5_optimizations/" + filename), config);
    }

    static JasminResult getJasminResultReg(String filename, int numReg) {
        Map<String, String> config = new HashMap<>();
        config.put("registerAllocation", String.valueOf(numReg));
        return TestUtils.backend(SpecsIo.getResource("fixtures/public/cpf/5_optimizations/" + filename), config);
    }

    @Test
    public void section1_InstSelection_iconst_0() {
        JasminResult jasminResult = getJasminResult("inst_selection/InstSelection_iconst_0.jmm");
        CpUtils.matches(jasminResult, "iconst_0");

    }
}
