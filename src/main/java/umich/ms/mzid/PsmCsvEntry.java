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

import java.util.Arrays;

/**
 * @author Dmitry Avtonomov
 */
public class PsmCsvEntry {
    final String spectrumId;
    final String[] mappedFilds;
    volatile int numTimesUsed;

    public PsmCsvEntry(String spectrumId, String[] mappedFilds) {
        this.spectrumId = spectrumId;
        this.mappedFilds = mappedFilds;
        numTimesUsed = 0;
    }

    public String getSpectrumId() {
        return spectrumId;
    }

    public String[] getMappedFilds() {
        return mappedFilds;
    }

    public synchronized void setNumTimesUsed(int numTimesUsed) {
        this.numTimesUsed = numTimesUsed;
    }

    public synchronized void incrementTimesUsed() {
        numTimesUsed++;
    }

    @Override
    public String toString() {
        return "PsmCsvEntry{" +
                "spectrumId='" + spectrumId + '\'' +
                ", mappedFilds=" + Arrays.toString(mappedFilds) +
                ", numTimesUsed=" + numTimesUsed +
                '}';
    }
}
