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

import com.github.chhh.utils.FileUtils;
import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import umich.ms.fileio.exceptions.FileParsingException;
import umich.ms.fileio.filetypes.mzidentml.MzIdentMLParser;
import umich.ms.fileio.filetypes.mzidentml.jaxb.standard.*;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * @author Dmitry Avtonomov
 */
class MzIdTool {
    private static final Logger logger = LoggerFactory.getLogger(MzIdTool.class);
    
    private Path pathToMzid;
    private InjectionData injectionData;
    private boolean filter;
    private MzIdentMLType mzid;
    int specIdItemsThrown = 0;
    List<SpectrumIdentificationItemType> specIdItemsKept;
    int specIdItemsModified = 0;
    int specIdItemsUnchanged = 0;
    int specIdItemsTotal = 0;

    public MzIdTool(Path pathToMzid, InjectionData injectionData, boolean filter) {
        this.pathToMzid = pathToMzid;
        this.injectionData = injectionData;
        this.filter = filter;
    }

    public MzIdentMLType getMzid() {
        return mzid;
    }



    public MzIdTool read() throws FileParsingException {
        // slurp the mzid file, that's our only option anyway
        logger.info("Reading mzid, might take a while.. ({} @ {})", FileUtils.fileSize(pathToMzid), pathToMzid);
        mzid = MzIdentMLParser.parse(pathToMzid);
        logger.info("Done reading mzid.");

        return this;
    }

    public MzIdTool filter() {
        logger.info("Started filtering");
        Preconditions.checkNotNull(mzid, "You must call read() before calling filter()");
        final DataCollectionType dataCollection = mzid.getDataCollection();
        Preconditions.checkNotNull(dataCollection, "DataCollection was not present/could not be parsed");
        final AnalysisDataType analysisData = dataCollection.getAnalysisData();
        Preconditions.checkNotNull(analysisData, "AnalysisData in DataCollection was not present/could not be parsed.");
        final List<SpectrumIdentificationListType> sils = analysisData.getSpectrumIdentificationList();
        Preconditions.checkNotNull(sils, "SpectrumIdentificationList was not present/could not be parsed.");

        // find the total number of SpecIdItems
        final int totalSpecIdItems = mzid.getDataCollection().getAnalysisData().getSpectrumIdentificationList().stream()
                .flatMap(sil -> sil.getSpectrumIdentificationResult().stream())
                .mapToInt(sir -> sir.getSpectrumIdentificationItem().size())
                .sum();
        logger.debug("Total SpecIdItems: {}", totalSpecIdItems);
        specIdItemsKept = new ArrayList<>(totalSpecIdItems);
        specIdItemsModified = 0;
        specIdItemsThrown = 0;
        specIdItemsUnchanged = 0;


        int silProgress = 0;
        int sirProgress = 0;

        for (SpectrumIdentificationListType sil : sils) {
            silProgress++;
            logger.debug("Processing SpectrumIdentificationList {}/{}", silProgress, sils.size());
            final List<SpectrumIdentificationResultType> sirs = sil.getSpectrumIdentificationResult();

            // a SpecIdResult doesn't stay, if it has no SpecIdItems left in it.
            final ArrayList<SpectrumIdentificationResultType> sirsStaying = new ArrayList<>(sirs.size());

            for (SpectrumIdentificationResultType sir : sirs) {
                if (sirProgress % 1000 == 0) {
                    logger.debug("Processing SpectrumIdentificationResult {}/{}", sirProgress, sirs.size());
                }
                sirProgress++;
                final String specResultId = sir.getId();
                final String specResultName = sir.getName(); // this is the thing that links to PSM table
                Preconditions.checkNotNull(specResultId, "SpectrumIdentificationResult ID was null.");
                Preconditions.checkNotNull(specResultName, "SpectrumIdentificationResult Name was null.");

                final List<SpectrumIdentificationItemType> siis = sir.getSpectrumIdentificationItem();

                // a SpecIdItem doesn't stay if it doesn't pass criteria (ID present in PSM table).
                final ArrayList<SpectrumIdentificationItemType> siisStaying = new ArrayList<>(siis.size());

                for (SpectrumIdentificationItemType specIdItem : siis) {
                    specIdItemsTotal++;
                    final String itemId = specIdItem.getId();
                    if (itemId == null) {
                        throw new IllegalStateException("SpectrumIdentificationItem ID was null");
                    }

                    // we need to add ".<charge-state>" to the Result ID to get PSM table ID
                    final int charge = specIdItem.getChargeState();
                    final String psmTableId = specResultName + "." + Integer.toString(charge);
                    final PsmCsvEntry psmTableEntry = injectionData.psmTable.get(psmTableId);

                    // Treating the current SpectrumIdentificationItem
                    if (psmTableEntry == null) {
                        // No PSM table entry
                        if (filter) {
                            // Filtering ON, No PSM table entry, the peptide is thrown
                            specIdItemsThrown++;
                        } else {
                            // Filtering OFF, No PSM table entry, the peptide stays unmodified
//                            logger.warn("No PSM table entry found for SpectrumIdentificationResult id=\"{}\", name=\"{}, " +
//                                            "SpectrumIdentificationItem id={}, charge={}\", skipping",
//                                    specResultId, specResultName, specIdItem.getId(), specIdItem.getChargeState());
                            siisStaying.add(specIdItem);
                            specIdItemsKept.add(specIdItem);
                            specIdItemsUnchanged++;
                        }
                        continue;
                    }

                    // PSM table entry found, peptide stays and gets modified
                    siisStaying.add(specIdItem);
                    specIdItemsKept.add(specIdItem);
                    psmTableEntry.incrementTimesUsed();

                    if (injectionData.mappedColNames.length > 0) {
                        specIdItemsModified++;

                        // INJECT
                        final List<AbstractParamType> paramGroup = specIdItem.getParamGroup();
                        for (int i = 0; i < injectionData.mappedColNames.length; i++) {
                            final UserParamType u = new UserParamType();
                            u.setName(injectionData.mappedColNames[i]);
                            u.setValue(psmTableEntry.mappedFilds[i]);
                            paramGroup.add(u);
                        }
                    } else {
                        specIdItemsUnchanged++;
                    }
                }
                // END for (SpecIdItem : SpecIdResult)

                // has any item been removed? if so we'll have to reinsert the remaining ones back
                if (siisStaying.size() == siis.size()) {
                    // all good, nothing got thrown away, SpecIdResult also stays
                    sirsStaying.add(sir);
                } else if (siisStaying.size() > 0) {
                    // Some SpecIdItems have been thrown away, SpecIdResult still stays
                    sirsStaying.add(sir);
                    // Its SpecIdItems list needs to be repopulated.
                    // There's no way to reassign the list, so we have to clear the existing one and re-insert
                    // elements that are staying.
                    final List<SpectrumIdentificationItemType> l = sir.getSpectrumIdentificationItem();
                    l.clear();
                    l.addAll(siisStaying);
                } else {
                    // specIdItemsStaying.size() == 0, specIdResult gets thrown
                }
            }

            // Possibly repopulate SpectrumIdentificationList with SpectrumIdentificationResults
            if (sirsStaying.size() != sirs.size()) {
                sirs.clear();
                sirs.addAll(sirsStaying);
            }
        }
        logger.info("Done filtering");
        return this;
    }

    /**
     * After {@link #filter()} has been called, there might be unused entities in mzid - the things that were
     * previously referenced by SpectrumIdentificationItems, such as Peptides and PeptideEvidences.
     * Calling this method attempts to remove entities that are not referenced anymore.
     *
     * <b>Operates on the same instance of MzId that the filter was called on.</b>
     *
     * @return
     */
    public MzIdTool cleanup() {
        // Only `Peptide`, `DBSequence` and `PeptideEvidence` need to be cleaned up.
        // They are all located in `SequenceCollection`.
        //
        // we might have removed `SpectrumIdentificationResult`s
        //      those contain `spectraData_ref`, we will leave spectraData untouched, as those spectra have actually
        //      been processed, but, sort of, produced no results
        // we might have removed `SpectrumIdentificationItem`s
        //      they contain `peptide_ref` and `peptideEvidence_ref`
        // `PeptideEvidence` maps a pure DB sequence to a `Peptide`, which is a sequence with all localized modifications
        //
        // Procedure:
        //    - Gather all `peptideEvidence_ref`s from the remaining `SpectrumIdentificationItem`s
        //    - Remove all `SequenceCollection`.`PeptideEvidence` that are not used
        //    - Gather all `peptide_ref`s from remaining `SpectrumIdentificationItems`s and `PeptideEvidence`s
        //    - Remove all `SequenceCollection`.`Peptide` that are not used
        //    - Gather all `dbsequence_ref`s from remaining `PeptideEvidence`
        //    - Remove all `SequenceCollection`.`DBSequence` that are not used
        logger.debug("Start cleanup()");
        final HashSet<String> usedPepEvRefs = new HashSet<>();
        final SequenceCollectionType seqCol = mzid.getSequenceCollection();

        // - Gather all `peptideEvidence_ref`s from the remaining `SpectrumIdentificationItem`s
        logger.debug("START: Gather all `peptideEvidence_ref`s from the remaining `SpectrumIdentificationItem`s");
        specIdItemsKept.stream()
                .flatMap(item -> item.getPeptideEvidenceRef().stream())
                .map(PeptideEvidenceRefType::getPeptideEvidenceRef)
                .forEach(usedPepEvRefs::add);
        logger.debug("END: Gather all `peptideEvidence_ref`s from the remaining `SpectrumIdentificationItem`s");

        // - Remove all `SequenceCollection`.`PeptideEvidence` that are not used
        logger.debug("START: ");
        final List<PeptideEvidenceType> pepEvs = seqCol.getPeptideEvidence();
        final ArrayList<PeptideEvidenceType> pepEvsOrig = new ArrayList<>(pepEvs);
        pepEvs.clear();
        pepEvsOrig.stream().filter(pepEv -> usedPepEvRefs.contains(pepEv.getId())).forEach(pepEvs::add);
        logger.debug("END: ");

        // - Gather all `peptide_ref`s from remaining `SpectrumIdentificationItems`s and `PeptideEvidence`s
        logger.debug("START: Gather all `peptide_ref`s from remaining `SpectrumIdentificationItems`s and `PeptideEvidence`s");
        final HashSet<String> usedPepRefs = new HashSet<>();
        pepEvs.stream().map(PeptideEvidenceType::getPeptideRef).forEach(usedPepRefs::add);
        specIdItemsKept.stream().map(SpectrumIdentificationItemType::getPeptideRef).forEach(usedPepRefs::add);
        logger.debug("END: Gather all `peptide_ref`s from remaining `SpectrumIdentificationItems`s and `PeptideEvidence`s");

        // - Remove all `SequenceCollection`.`Peptide` that are not used
        logger.debug("START: Remove all `SequenceCollection`.`Peptide` that are not used");
        final List<PeptideType> peps = seqCol.getPeptide();
        final ArrayList<PeptideType> pepsOrig = new ArrayList<>(peps);
        peps.clear();
        pepsOrig.stream().filter(pep -> usedPepRefs.contains(pep.getId())).forEach(peps::add);
        logger.debug("END: Remove all `SequenceCollection`.`Peptide` that are not used");

        // - Gather all `dbsequence_ref`s from remaining `PeptideEvidence`
        logger.debug("START: Gather all `dbsequence_ref`s from remaining `PeptideEvidence`");
        final HashSet<String> usedDbSeqRefs = new HashSet<>();
        pepEvs.stream().map(PeptideEvidenceType::getDBSequenceRef).forEach(usedDbSeqRefs::add);
        logger.debug("END: Gather all `dbsequence_ref`s from remaining `PeptideEvidence`");

        // - Remove all `SequenceCollection`.`DBSequence` that are not used
        logger.debug("START: Remove all `SequenceCollection`.`DBSequence` that are not used");
        final List<DBSequenceType> dbSeqs = seqCol.getDBSequence();
        final ArrayList<DBSequenceType> dbSeqsOrig = new ArrayList<>(dbSeqs);
        dbSeqs.clear();
        dbSeqsOrig.stream().filter(dbSeq -> usedDbSeqRefs.contains(dbSeq.getId())).forEach(dbSeqs::add);
        logger.debug("END: Remove all `SequenceCollection`.`DBSequence` that are not used");

        logger.debug("Done cleanup()");
        return this;
    }
}
