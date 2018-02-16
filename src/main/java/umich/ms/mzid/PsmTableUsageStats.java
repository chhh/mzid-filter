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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Dmitry Avtonomov
 */
class PsmTableUsageStats {
    private static final Logger logger = LoggerFactory.getLogger(PsmTableUsageStats.class);

    private InjectionData injectionData;
    private int psmsTotal;
    private int psmsUnused;
    private int psmsUsedOnce;
    private int psmsUsedMultipleTimes;

    public PsmTableUsageStats(InjectionData injectionData) {
        this.injectionData = injectionData;
    }

    public int getPsmsTotal() {
        return psmsTotal;
    }

    public int getPsmsUnused() {
        return psmsUnused;
    }

    public int getPsmsUsedOnce() {
        return psmsUsedOnce;
    }

    public int getPsmsUsedMultipleTimes() {
        return psmsUsedMultipleTimes;
    }

    public PsmTableUsageStats invoke() {
        psmsTotal = injectionData.psmTable.size();
        psmsUnused = 0;
        psmsUsedOnce = 0;
        psmsUsedMultipleTimes = 0;
        for (PsmCsvEntry psmCsvEntry : injectionData.psmTable.values()) {
            if (psmCsvEntry.numTimesUsed <= 0) {
//                logger.warn("An entry from PSMs table has not been used, id=\"{}\".", psmCsvEntry.spectrumId);
                psmsUnused++;
            } else if (psmCsvEntry.numTimesUsed > 1) {
//                logger.warn("An entry from PSMs table has been used more than once ({} times), id=\"{}\".",
//                        psmCsvEntry.numTimesUsed, psmCsvEntry.spectrumId);
                psmsUsedMultipleTimes++;
            } else {
                psmsUsedOnce++;
            }
        }
        return this;
    }
}
