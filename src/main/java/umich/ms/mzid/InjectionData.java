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

import java.util.Map;

/**
 * @author Dmitry Avtonomov
 */
public class InjectionData {
    final String[] psmColNames;
    final int[] psmColIndexes;
    final String idColName;
    final Integer idColIndex;

    final String[] mappedColNames;
    final Map<String, PsmCsvEntry> psmTable;

    public InjectionData(String[] psmColNames, int[] psmColIndexes, String idColName, Integer idColIndex, String[] mappedColNames, Map<String, PsmCsvEntry> psmTable) {
        this.psmColNames = psmColNames;
        this.psmColIndexes = psmColIndexes;
        this.idColName = idColName;
        this.idColIndex = idColIndex;
        this.mappedColNames = mappedColNames;
        this.psmTable = psmTable;
    }
}
