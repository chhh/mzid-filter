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

import com.github.chhh.utils.ResourceUtils;
import org.junit.Assert;
import org.junit.Test;
import umich.ms.fileio.filetypes.mzidentml.MzIdentMLParser;
import umich.ms.fileio.filetypes.mzidentml.jaxb.standard.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;

/**
 * @author Dmitry Avtonomov
 */
public class MzIdInjectorTest {
    private static final boolean DELETE_ON_EXIT = true;

    @Test
    public void testInjectionNoFilter() throws Exception {
        String mzIdName = "Sequest_example_ver1.1_small.mzid";
        String psmTableName = "psm_3-lines.tsv";
        String mappingsFileNname = "mappings.txt";
        final Path pathMzId = ResourceUtils.getResource(MzIdInjectorTest.class, "", mzIdName);
        final Path pathPsmTable = ResourceUtils.getResource(MzIdInjectorTest.class, "", psmTableName);
        final Path pathMappings = ResourceUtils.getResource(MzIdInjectorTest.class, "", mappingsFileNname);


        final HashMap<String, String> mappings = new HashMap<>();
        InputParams.fillMappings(mappings, pathMappings);
        mappings.put("PeptideProphet Probability", "NESVI:PPPROB");
        final String idPsmColName = "Spectrum";
        final InputParams.Validated opts = new InputParams.Validated(pathMzId, pathPsmTable, mappings, idPsmColName, false, false);

        MzIdInjector.run(opts);

        final Path pathOut = MzIdInjector.computeOutputPath(pathMzId);
        if (!Files.exists(pathOut))
            throw new IllegalStateException("Output file was not created");
        if (DELETE_ON_EXIT)
            pathOut.toFile().deleteOnExit();
        final MzIdentMLType mzid = MzIdentMLParser.parse(pathOut);

        final List<SpectrumIdentificationListType> sidLists = mzid.getDataCollection().getAnalysisData().getSpectrumIdentificationList();
        Assert.assertNotNull(sidLists);
        Assert.assertEquals(1, sidLists.size());
        for (SpectrumIdentificationListType sidList : sidLists) {
            final List<SpectrumIdentificationResultType> sidResults = sidList.getSpectrumIdentificationResult();
            Assert.assertNotNull(sidResults);
            for (SpectrumIdentificationResultType sidResult : sidResults) {
                Assert.assertEquals(2, sidResult.getSpectrumIdentificationItem().size());
            }
        }
        Assert.assertEquals(2, mzid.getSequenceCollection().getDBSequence().size());
        Assert.assertEquals(2, mzid.getSequenceCollection().getPeptide().size());
        Assert.assertEquals(2, mzid.getSequenceCollection().getPeptideEvidence().size());
    }

    @Test
    public void testInjectionWithFilter() throws Exception {
        String mzIdName = "Sequest_example_ver1.1_small.mzid";
        String psmTableName = "psm_3-lines.tsv";
        String mappingsFileNname = "mappings.txt";
        final Path pathMzId = ResourceUtils.getResource(MzIdInjectorTest.class, "", mzIdName);
        final Path pathPsmTable = ResourceUtils.getResource(MzIdInjectorTest.class, "", psmTableName);
        final Path pathMappings = ResourceUtils.getResource(MzIdInjectorTest.class, "", mappingsFileNname);


        final HashMap<String, String> mappings = new HashMap<>();
        InputParams.fillMappings(mappings, pathMappings);
        final String injectedExtraParamName = "NESVI:PPPROB";
        mappings.put("PeptideProphet Probability", injectedExtraParamName);
        final String idPsmColName = "Spectrum";
        final InputParams.Validated opts = new InputParams.Validated(pathMzId, pathPsmTable, mappings, idPsmColName, true, true);

        MzIdInjector.run(opts);

        final Path pathOut = MzIdInjector.computeOutputPath(pathMzId);
        if (!Files.exists(pathOut))
            throw new IllegalStateException("Output file was not created");
        if (DELETE_ON_EXIT)
            pathOut.toFile().deleteOnExit();
        final MzIdentMLType mzid = MzIdentMLParser.parse(pathOut);

        final List<SpectrumIdentificationListType> sidLists = mzid.getDataCollection().getAnalysisData().getSpectrumIdentificationList();
        Assert.assertNotNull(sidLists);
        Assert.assertEquals(1, sidLists.size());
        for (SpectrumIdentificationListType sidList : sidLists) {
            final List<SpectrumIdentificationResultType> sidResults = sidList.getSpectrumIdentificationResult();
            Assert.assertNotNull(sidResults);
            for (SpectrumIdentificationResultType sidResult : sidResults) {
                Assert.assertEquals(1, sidResult.getSpectrumIdentificationItem().size());
                for (SpectrumIdentificationItemType specId : sidResult.getSpectrumIdentificationItem()) {
                    boolean hasAMappedField = specId.getParamGroup().stream()
                            .anyMatch(p -> p instanceof UserParamType && injectedExtraParamName.equals(p.getName()));
                    Assert.assertTrue("Mapped field not found in output", hasAMappedField);
                }
            }
        }
        Assert.assertEquals(1, mzid.getSequenceCollection().getDBSequence().size());
        Assert.assertEquals(1, mzid.getSequenceCollection().getPeptide().size());
        Assert.assertEquals(1, mzid.getSequenceCollection().getPeptideEvidence().size());
    }

    @Test
    public void testOnlyFilter() throws Exception {
        String mzIdName = "Sequest_example_ver1.1_small.mzid";
        String psmTableName = "psm_3-lines.tsv";
        String mappingsFileNname = "mappings.txt";
        final Path pathMzId = ResourceUtils.getResource(MzIdInjectorTest.class, "", mzIdName);
        final Path pathPsmTable = ResourceUtils.getResource(MzIdInjectorTest.class, "", psmTableName);
        final Path pathMappings = ResourceUtils.getResource(MzIdInjectorTest.class, "", mappingsFileNname);


        final HashMap<String, String> mappings = new HashMap<>();
        InputParams.fillMappings(mappings, pathMappings);
        final String idPsmColName = "Spectrum";
        final InputParams.Validated opts = new InputParams.Validated(pathMzId, pathPsmTable, mappings, idPsmColName, true, false);

        MzIdInjector.run(opts);

        final Path pathOut = MzIdInjector.computeOutputPath(pathMzId);
        if (!Files.exists(pathOut))
            throw new IllegalStateException("Output file was not created");
        if (DELETE_ON_EXIT)
            pathOut.toFile().deleteOnExit();
        final MzIdentMLType mzid = MzIdentMLParser.parse(pathOut);

        final List<SpectrumIdentificationListType> sidLists = mzid.getDataCollection().getAnalysisData().getSpectrumIdentificationList();
        Assert.assertNotNull(sidLists);
        Assert.assertEquals(1, sidLists.size());
        for (SpectrumIdentificationListType sidList : sidLists) {
            final List<SpectrumIdentificationResultType> sidResults = sidList.getSpectrumIdentificationResult();
            Assert.assertNotNull(sidResults);
            for (SpectrumIdentificationResultType sidResult : sidResults) {
                Assert.assertEquals(1, sidResult.getSpectrumIdentificationItem().size());
            }
        }
        Assert.assertEquals(1, mzid.getSequenceCollection().getDBSequence().size());
        Assert.assertEquals(1, mzid.getSequenceCollection().getPeptide().size());
        Assert.assertEquals(1, mzid.getSequenceCollection().getPeptideEvidence().size());
    }
}
