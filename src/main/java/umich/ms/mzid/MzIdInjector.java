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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.github.chhh.utils.FileUtils;
import com.github.chhh.utils.data.DelimitedFiles;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import umich.ms.fileio.exceptions.FileParsingException;
import umich.ms.fileio.filetypes.mzidentml.jaxb.standard.MzIdentMLType;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Dmitry Avtonomov
 */
public class MzIdInjector {
    private static final Logger logger = LoggerFactory.getLogger(umich.ms.mzid.MzIdInjector.class);
    public static final String PROG_NAME = "mzid-filter";

    public static void main(String[] args) throws IOException, FileParsingException, JAXBException {
        printIntro();

        // Parse inputs
        InputParams ip = parseInputParams(args);
        if (ip == null) return;

        // validate params deeper and run
        try {
            final InputParams.Validated valid = ip.validate();

            // all parameters validated, print them out and run
            System.out.printf("  - MzIdentML: %s\n", valid.getPathToMzid());
            System.out.printf("  - PSM table: %s\n", valid.getPathToPsms());
            System.out.printf("  - Filter: %b\n", valid.isFilter());
            System.out.printf("  - Prune: %b\n", valid.isPrune());
            if (valid.getMapping().isEmpty()) {
                System.out.printf("  - No mappings\n");
            } else {
                System.out.printf("  - Mappings (PSM table -> mzid):\n");
                for (Map.Entry<String, String> entry : valid.getMapping().entrySet()) {
                    System.out.printf("    - %s -> %s\n", entry.getKey(), entry.getValue());
                }
            }

            run(valid);
        } catch (ParameterException e) {
            System.err.println("Error: " + e.getMessage());
            return;
        }
    }

    static void printIntro() {
        System.out.printf("%s (v%s)\n\n", PROG_NAME, Version.version);
    }

    static InputParams parseInputParams(String[] args) {
        InputParams ip = new InputParams();
        JCommander jcom = new JCommander();
        jcom.addObject(ip);
        jcom.setProgramName("java -jar " + PROG_NAME + ".jar");

        // No inputs
        if (args.length == 0) {
            jcom.usage();
            return null;
        }

        // parse the inputs, check for basic errors
        try {
            jcom.parse(args);
        } catch (ParameterException e) {
            System.err.println("Error: " + e.getMessage());
            return null;
        }

        // is help requested?
        if (ip.help) {
            jcom.usage();
            return null;
        }
        return ip;
    }

    public static void run(InputParams.Validated opts) throws IOException, FileParsingException, JAXBException {
        final Path pathToPsms = opts.getPathToPsms();
        final Path pathToMzid = opts.getPathToMzid();
        final Path pathOut = computeOutputPath(pathToMzid);


        // first read the PSMs file and check if it has all the right columns
        InjectionData injectionData = parseDataToInject(opts.getMapping(), opts.getPsmIdCol(), pathToPsms);


        // modify / filter
        MzIdTool mzidTool = new MzIdTool(pathToMzid, injectionData, opts.isFilter())
                .read()     // reads the whole file in
                .filter()   // filters / injects
                .cleanup(); // removes unused data entries, such as DBSequences and PeptideEvidence

        // check for unused entries
        PsmTableUsageStats psmStats = new PsmTableUsageStats(injectionData).invoke();


        // print out some stats about modification / filtering
        logger.info("MzId entries (PSMs): removed {}, modified {}, left as-is {}, total {}.",
                mzidTool.specIdItemsThrown, mzidTool.specIdItemsModified,
                mzidTool.specIdItemsUnchanged, mzidTool.specIdItemsTotal);
        logger.info("PSM table entries: used once {}, unused {}, used many times {}, total {}.",
                psmStats.getPsmsUsedOnce(), psmStats.getPsmsUnused(), psmStats.getPsmsUsedMultipleTimes(),
                psmStats.getPsmsTotal());


        // All entries have been modified / filtered
        // WRITE the file out
        MzIdentMLType mzid = mzidTool.getMzid();
        writeOutput(pathOut, mzid);


    }

    static void writeOutput(Path pathOut, MzIdentMLType mzid) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(MzIdentMLType.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");

        logger.info("Started writing output ({})", pathOut);
        marshaller.marshal(mzid, pathOut.toFile());
        logger.info("Done writing output");
    }

    static InjectionData parseDataToInject(final Map<String, String> mapping, String idColName, Path pathToPsms) throws IOException {
        final Character delimiter = DelimitedFiles.guessDelimiter(pathToPsms);
        if (delimiter == null) {
            throw new ParameterException("Could not guess delimiter used in [--psms] file.");
        }

        final CSVFormat csvFormat = CSVFormat.newFormat(delimiter).withFirstRecordAsHeader();
        logger.info("Reading PSM table ({} @ {}).", FileUtils.fileSize(pathToPsms), pathToPsms);
        final CSVParser parser = CSVParser.parse(pathToPsms, Charset.forName("UTF-8"), csvFormat); // will parse the first line for header
        final Map<String, Integer> header = parser.getHeaderMap();
        if (header == null || header.isEmpty()) {
            throw new IllegalStateException("Header of PSMs file could not be parsed or the file was empty");
        }

        // check if the header contains all the needed mappings
        for (String key : mapping.keySet()) {
            if (!header.containsKey(key)) {
                throw new ParameterException(String.format("Header of PSMs file [%s] did not contain a required " +
                        "mapping column [%s]", pathToPsms.toString(), key));
            }
        }

        // read the PSM table and store the needed values
        String[] psmColNames = mapping.keySet().toArray(new String[mapping.size()]);
        int[] psmColIndexes = new int[psmColNames.length]; // we need these column indexes for each record
        for (int i = 0; i < psmColNames.length; i++) {
            final Integer index = header.get(psmColNames[i]);
            if (index == null) {
                throw new IllegalStateException(String.format("No header index mapping for column name [%s]", psmColNames[i]));
            }
            psmColIndexes[i] = index;
        }
        String[] mappedColNames = new String[psmColNames.length];
        for (int i = 0; i < psmColNames.length; i++) {
            mappedColNames[i] = mapping.get(psmColNames[i]);
            if (mappedColNames[i] == null) {
                throw new IllegalStateException("Could not map PSM table column name to a mapped name. This should never happen.");
            }
        }
        final Integer idColIndex = header.get(idColName);
        if (idColIndex == null) {
            throw new IllegalStateException(String.format("No header index mapping for ID column [%s]", idColName));
        }

        Map<String, PsmCsvEntry> psms = new HashMap<>();
        for (CSVRecord record : parser) {
            final String id = record.get(idColIndex);
            if (StringUtils.isBlank(id)) {
                logger.warn("An entry with an empty ID found in PSM table file around line {}", record.getRecordNumber());
                continue;
            }
            final String[] colVals = new String[psmColIndexes.length];
            for (int i = 0; i < psmColIndexes.length; i++) {
                String v = record.get(psmColIndexes[i]);
                colVals[i] = v == null ? "" : v;
            }
            if (psms.containsKey(id)) {
                logger.warn("An entry with a duplicate ID [{}] found in PSM table file around line {}", id, record.getRecordNumber());
            }
            psms.put(id, new PsmCsvEntry(id, colVals));
        }
        logger.info("Done reading PSM table.");
        return new InjectionData(psmColNames, psmColIndexes, idColName, idColIndex,
                mappedColNames, psms);
    }

    static Path computeOutputPath(Path pathToMzid) {
        final Path dir = pathToMzid.getParent();
        final String mzidFileName = pathToMzid.getFileName().toString();
        int dot = mzidFileName.lastIndexOf('.');
        String mzidInjectedName;
        if (dot < 0) {
            mzidInjectedName = mzidFileName + ".injected.mzid";
        } else {
            mzidInjectedName = mzidFileName.substring(0, dot) + ".injected" + mzidFileName.substring(dot, mzidFileName.length());
        }
        return dir.resolve(mzidInjectedName);
    }

}
