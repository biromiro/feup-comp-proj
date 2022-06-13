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
import org.specs.comp.ollir.*;
import pt.up.fe.comp.CpUtils;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.specs.util.SpecsIo;

import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.hasItem;

public class C3_Ollir {

    static OllirResult getOllirResult(String filename) {
        return TestUtils.optimize(SpecsIo.getResource("fixtures/public/cpf/3_ollir/" + filename));
    }

    /**
     * Test the declaration of the main class
     */
    @Test
    public void section1_Basic_Structure_main() {
        var result = getOllirResult("basic/Structure_class.jmm");

        var method = CpUtils.getMethod(result, "main");

        CpUtils.assertEquals("Is name of the method name?", "main", method.getMethodName(), result);
        CpUtils.assertEquals("Is return type void?", "void", CpUtils.toString(method.getReturnType()), result);
        CpUtils.assertEquals("Is first parameter String[]?", "String[]", CpUtils.toString(method.getParam(0).getType()),
                result);
        CpUtils.assertEquals("Has public access modifier?", "PUBLIC", method.getMethodAccessModifier().toString(),
                result);
        CpUtils.assertTrue("Is static method?", method.isStaticMethod(), result);
    }

}
