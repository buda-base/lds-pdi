package io.bdrc.formatters;

/*******************************************************************************
 * Copyright (c) 2018 Buddhist Digital Resource Center (BDRC)
 *
 * If this file is a derivation of another work the license header will appear below;
 * otherwise, this work is licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

import java.util.List;
import java.util.SortedMap;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFWriter;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.sparql.util.Symbol;
import org.apache.jena.vocabulary.SKOS;

import io.bdrc.jena.sttl.CompareComplex;
import io.bdrc.jena.sttl.ComparePredicates;
import io.bdrc.jena.sttl.STTLWriter;

public class TTLRDFWriter {

    static public final Lang sttl = STTLWriter.registerWriter();
    static public final String strLangSttl = "STTL";
    static final SortedMap<String, Integer> nsPrio = ComparePredicates.getDefaultNSPriorities();
    static final List<String> predicatesPrio = CompareComplex.getDefaultPropUris();
    static final Context ctx = new Context();

    static {
        nsPrio.put(SKOS.getURI(), 1);
        nsPrio.put("http://purl.bdrc.io/ontology/admin/", 5);
        nsPrio.put("http://purl.bdrc.io/ontology/toberemoved/", 6);
        predicatesPrio.add("http://purl.bdrc.io/ontology/admin/logDate");
        predicatesPrio.add("http://purl.bdrc.io/ontology/core/seqNum");
        predicatesPrio.add("http://purl.bdrc.io/ontology/core/onYear");
        predicatesPrio.add("http://purl.bdrc.io/ontology/core/notBefore");
        predicatesPrio.add("http://purl.bdrc.io/ontology/core/notAfter");
        predicatesPrio.add("http://purl.bdrc.io/ontology/core/noteText");
        predicatesPrio.add("http://purl.bdrc.io/ontology/core/noteWork");
        predicatesPrio.add("http://purl.bdrc.io/ontology/core/noteLocationStatement");
        predicatesPrio.add("http://purl.bdrc.io/ontology/core/volumeNumber");
        predicatesPrio.add("http://purl.bdrc.io/ontology/core/eventWhere");
        predicatesPrio.add("http://purl.bdrc.io/ontology/core/eventWho");
        predicatesPrio.add("http://purl.bdrc.io/ontology/core/lineageWho");
        ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "nsPriorities"), nsPrio);
        ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "nsDefaultPriority"), 2);
        ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "complexPredicatesPriorities"), predicatesPrio);
        ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "indentBase"), 3);
        ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "predicateBaseWidth"), 12);
    }

    public static RDFWriter getSTTLRDFWriter(Model m,String baseURI) {
        return RDFWriter.create().source(m.getGraph()).base(baseURI).context(ctx).lang(sttl).build();
    }

}
