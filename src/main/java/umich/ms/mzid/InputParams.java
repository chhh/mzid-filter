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

package umich.ms.mzid;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import umich.ms.mzid.opts.PathConverter;
import umich.ms.mzid.opts.PathCreatableValidator;
import umich.ms.mzid.opts.PathExistsValueValidator;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Dmitry Avtonomov
 */

public class InputParams {
    @Parameter(names = {"--help", "-h"}, help = true, description = "This help message.")
    boolean help;

    @Parameter(names = {"--mzid"}, required = true,
            validateWith = PathCreatableValidator.class, converter = PathConverter.class, validateValueWith = PathExistsValueValidator.class,
            description = "Path to *.mzid file.")
    Path pathToMzid;

    @Parameter(names = {"--psms"}, required = true,
            validateWith = PathCreatableValidator.class, converter = PathConverter.class, validateValueWith = PathExistsValueValidator.class,
            description = "Path to psms.tsv file.")
    Path pathToPsms;

    @Parameter(names = {"--mapf"},
            validateWith = PathCreatableValidator.class, converter = PathConverter.class, validateValueWith = PathExistsValueValidator.class,
            description = "Path to mapping file. One mapping per line, tab delimited, of the form:" +
                    " `<col-name-in-psms-file>\\t<userParam-name>`. `--map` overrides existing entries in this file.")
    Path pathToMapping;

    @Parameter(names = {"--map", "-m"}, arity = 2,
            description = "List of mappings of the form <col-name-in-psms-file> -> <userParam-name>. " +
                    "If you need more than one, specify multiple times. Example: " +
                    "--map \"Observed mz\" \"MAP-NAME:mzexp\" --map \"Theoretical mz\" \"MAP-NAME:mzcalc\". " +
                    "See also `--mapf` (`--map` overrides existing entries in this file).")
    List<String> mappingList;

    @Parameter(names = {"--psmidcol"},
            description = "The name of the column in PSMs file that provides " +
            "the spectrum identifier to be used for matching to mzid file.")
    String psmIdCol = "Spectrum";

    @Parameter(names = {"--filter"}, arity = 1,
            description = "Filter out entries from mzid file that have no corresponding entries in the PSM table. " +
                    "When off will leave all mzid entries as-is, only injecting additional parameters. " +
                    "To turn off: `--filter false`.")
    boolean filter = true;

    @Parameter(names = {"--prune"}, arity = 1,
            description = "")
    boolean prune = true;

    public Validated validate() throws ParameterException {
        if (pathToMapping == null && mappingList == null && !filter) {
            throw new ParameterException("No mappings specified and filtering turned off. See '--mapf', '--map' and '--filter' parameters.");
        }

        HashMap<String, String> map = new HashMap<>();
        // mappings from file
        if (pathToMapping != null) {
            fillMappings(map, pathToMapping);
        }
        // mappings specified on command line
        if (mappingList != null) {
            for (int i = 0; i < mappingList.size(); i+=2) {
                map.put(mappingList.get(i), mappingList.get(i+1));
            }
        }

        if (map.isEmpty() && !filter) {
            throw new ParameterException("The mappings can't be empty when '--filter' is turned off.");
        }

        return new Validated(pathToMzid, pathToPsms, map, psmIdCol, filter, prune);
    }

    /**
     * Fills a mapping hashtable from a file. Entries must be tab delimited. One mapping per line in the form:<br/>
     * <code>KEY\tVALUE</code>
     * @param map Map to put mappings to.
     * @param pathToMapping The file with mappings.
     * @throws ParameterException In case anything goes wrong with reading the file.
     */
    public static void fillMappings(HashMap<String, String> map, Path pathToMapping) {
        try {
            final List<String> lines = Files.readAllLines(pathToMapping, Charset.forName("UTF-8"));
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                line = line.trim();
                if (line.length() <= 0)
                    continue;
                final String[] split = line.split("\t");
                if (split.length != 2) {
                    throw new ParameterException(String.format("Error in mappings file [%s] line [%d]", pathToMapping.toString(), i));
                }
                map.put(split[0], split[1]);
            }
        } catch (IOException e) {
            throw new ParameterException("Can't read mappings file: [" + pathToMapping.toString() + "]", e);
        }
    }

    public static class Validated {
        private Path pathToMzid;
        private Path pathToPsms;
        private Map<String, String> mapping;
        private String psmIdCol;
        private boolean filter;
        private boolean prune;

        public Validated(Path pathToMzid, Path pathToPsms, Map<String, String> mapping, String psmIdCol, boolean filter, boolean prune) {
            this.pathToMzid = pathToMzid;
            this.pathToPsms = pathToPsms;
            this.mapping = mapping;
            this.psmIdCol = psmIdCol;
            this.filter = filter;
            this.prune = prune;
        }

        public Path getPathToMzid() {
            return pathToMzid;
        }

        public Path getPathToPsms() {
            return pathToPsms;
        }

        public Map<String, String> getMapping() {
            return mapping;
        }

        public String getPsmIdCol() {
            return psmIdCol;
        }

        public boolean isFilter() {
            return filter;
        }

        public boolean isPrune() {
            return prune;
        }
    }
}
