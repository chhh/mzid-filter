/*
 * Copyright (c) 2017 Dmitry Avtonomov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package umich.ms.mzid.opts;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Dmitry Avtonomov
 */
public class PathCreatableValidator implements IParameterValidator {
    @Override
    public void validate(String name, String value) throws ParameterException {
        try {
            final Path path = Paths.get(value);
        } catch (InvalidPathException e) {
            throw new ParameterException(String.format("Parameter [%s] - Not a valid path: [%s]", name, value), e);
        }
    }
}
