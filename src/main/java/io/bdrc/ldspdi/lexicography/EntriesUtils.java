package io.bdrc.ldspdi.lexicography;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.ibm.icu.text.Collator;

import io.bdrc.ldspdi.exceptions.RestException;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.lucene.bo.TibetanAnalyzer;

public class EntriesUtils {
    
    public final static Logger log = LoggerFactory.getLogger(EntriesUtils.class);
    public final static Collator tibUniCollator = Collator.getInstance(Locale.forLanguageTag("bo"));
    
    public static final class Token {
        public final int start;
        public final int end;
        public final String charTerm;
        
        public Token(final int start, final int end, final String charTerm) {
            this.start = start;
            this.end = end;
            this.charTerm = charTerm;
        }
        @Override
        public String toString() {
             return this.charTerm+" ("+this.start+"-"+this.end+")";
        }
    }
    
    static public TibetanAnalyzer uniAnalyzer = null;
    static public TibetanAnalyzer ewtsAnalyzer = null;
    static public TibetanAnalyzer uniAnalyzer_noStops = null;
    static public TibetanAnalyzer ewtsAnalyzer_noStops = null;
    static {
        try {
            uniAnalyzer = new TibetanAnalyzer(false, true, true, "unicode", "");
            ewtsAnalyzer = new TibetanAnalyzer(false, true, true, "ewts", "");
            uniAnalyzer_noStops = new TibetanAnalyzer(false, true, true, "unicode", null);
            ewtsAnalyzer_noStops = new TibetanAnalyzer(false, true, true, "ewts", null);
        } catch (IOException e) {
            log.error("can't initialize Tibetan analyzer", e);
        }
    }
    
    static public List<Token> getTokens(String inputStr, final String inputStr_lang, final boolean ignore_stop_words) throws IOException {
        TibetanAnalyzer ta = inputStr_lang.equals("bo-x-ewts") ? ewtsAnalyzer : uniAnalyzer;
        if (ignore_stop_words)
            ta = inputStr_lang.equals("bo-x-ewts") ? ewtsAnalyzer_noStops : uniAnalyzer_noStops;
        final TokenStream ts =  ta.tokenStream("", inputStr);
        ts.reset();
        final List<Token> res = new ArrayList<>();
        try {
            final CharTermAttribute charTermAttr = ts.addAttribute(CharTermAttribute.class);
            final OffsetAttribute offsetAttr = ts.addAttribute(OffsetAttribute.class);
            while (ts.incrementToken()) {
                res.add(new Token(offsetAttr.startOffset(), offsetAttr.endOffset(), charTermAttr.toString()));
            }
        } catch (IOException e) {
            log.error("error while tokenizing {}", inputStr, e);
            return null;
        } finally {
            ts.close();
        }
        return res;
    }
    
    public static int[] getTokensRange(final List<Token> tokens, final int offset_start, final int offset_end) {
        log.debug("get tokens in the range {}:{}", offset_start, offset_end);
        final int[] res = { -1, -1 };
        boolean first_seen = false;
        int t_i = 0;
        for (final Token t : tokens) {
            log.debug("look at token at range {}:{}", t.start, t.end);
            if (t.start < offset_end && t.end > offset_start) {
                if (!first_seen) {
                    res[0] = t_i;
                    first_seen = true;
                }
                log.debug("token matches");
                res[1] = t_i;
            }
            if (t.start > offset_end)
                break;
            t_i += 1;
        }
        return res;
    }
    
    public static class SimpleLiteralSerializer extends JsonSerializer<Literal> {
        @Override
        public void serialize(Literal value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            final String lang = value.getLanguage();
            if (lang != null && !lang.isEmpty()) {
                gen.writeStringField("lang", value.getLanguage());
            }
            gen.writeStringField("value", value.getLexicalForm());
            gen.writeEndObject();
        }
    }
    
    public static class Entry {
        @JsonProperty("word")
        public final Literal word;
        @JsonIgnore
        public final Literal normalized;
        @JsonProperty("def")
        public final Literal def;
        @JsonProperty("uri")
        public final String res_uri;
        @JsonProperty("nb_tokens")
        public int nb_tokens = 0;
        @JsonProperty("chunk_offset_start")
        public int chunk_offset_start = 0;
        @JsonProperty("chunk_offset_end")
        public int chunk_offset_end = 0;
        @JsonProperty("cursor_in_entry_start")
        public int cursor_in_entry_start = 0;
        @JsonProperty("cursor_in_entry_end")
        public int cursor_in_entry_end = 0;
        @JsonProperty("type")
        public String type;
        
        public Entry(final Literal word, final Literal normalized, final Literal def, final Resource res, final Literal type) {
            this.word = word;
            this.def = def;
            this.normalized = normalized;
            this.res_uri = res.getURI();
            this.type = type.getString();
        }
        
        @Override
        public String toString() {
            return this.word.getLexicalForm();
        }
    }
    
    public static List<Entry> searchInFuseki(final String cursor_string, final String raw_cursor_string, final String cursor_string_left, final String cursor_string_right, final String lang) {
        final List<Entry> res = new ArrayList<>();
        // type can be:
        // - "e" for exact
        // - "c" for context
        // - "k" for match in keyword
        // - "d" for match in definition
        String sparql = "select distinct ?res ?word ?normalized ?def ?type where {";
        // first exact matches, either the entry or its normalized form
        sparql +=       " {  ?res <https://www.w3.org/ns/lemon/ontolex#writtenForm> \""+cursor_string.replace("\"", "")+"\"@bo . BIND(\""+cursor_string.replace("\"", "")+"\"@bo as ?word) ?res <http://purl.bdrc.io/ontology/core/definitionGMD> ?def . BIND(\"e\" as ?type) }";
        sparql +=       " union {  ?res <https://www.w3.org/ns/lemon/ontolex#writtenForm> \""+raw_cursor_string.replace("\"", "")+"\"@bo . BIND(\""+raw_cursor_string.replace("\"", "")+"\"@bo as ?word) ?res <http://purl.bdrc.io/ontology/core/definitionGMD> ?def . BIND(\"e\" as ?type) }";
        sparql +=       " union {  ?res <http://purl.bdrc.io/ontology/core/normalizedForm> \""+cursor_string.replace("\"", "")+"\"@bo ; <https://www.w3.org/ns/lemon/ontolex#writtenForm> ?word ; <http://purl.bdrc.io/ontology/core/definitionGMD> ?def . BIND(\""+cursor_string.replace("\"", "")+"\"@bo as ?normalized) BIND(\"e\" as ?type) }";
        // then left / right context if relevant
        if (cursor_string_left != null) {
            sparql +=   " union { (?res ?sc ?word) <http://jena.apache.org/text#query> (<https://www.w3.org/ns/lemon/ontolex#writtenForm> \"\\\""+cursor_string_left.replace("\"", "")+"\\\"\"@"+lang+") . ?res <http://purl.bdrc.io/ontology/core/definitionGMD> ?def . BIND(\"c\" as ?type) }";
        }
        if (cursor_string_right != null) {
            sparql +=   " union { (?res ?sc ?word) <http://jena.apache.org/text#query> (<https://www.w3.org/ns/lemon/ontolex#writtenForm> \"\\\""+cursor_string_right.replace("\"", "")+"\\\"\"@"+lang+") . ?res <http://purl.bdrc.io/ontology/core/definitionGMD> ?def . BIND(\"c\" as ?type) }";
        }
        // then matches in the keyword
        sparql +=   " union { (?res ?sc ?word) <http://jena.apache.org/text#query> (<https://www.w3.org/ns/lemon/ontolex#writtenForm> \"\\\""+cursor_string.replace("\"", "")+"\\\"\"@"+lang+" \"highlight:\") . ?res <http://purl.bdrc.io/ontology/core/definitionGMD> ?def . BIND(\"k\" as ?type) }";
        // matches in the definition
        sparql +=   " union { (?res ?sc ?def) <http://jena.apache.org/text#query> (<http://purl.bdrc.io/ontology/core/definitionGMD> \"\\\""+cursor_string.replace("\"", "")+"\\\"\"@"+lang+" \"highlight:\") . ?res <https://www.w3.org/ns/lemon/ontolex#writtenForm> ?word . BIND(\"d\" as ?type) }";
        sparql +=       "} limit 200";
        log.debug(sparql);
        final String fusekiUrl = ServiceConfig.getProperty("fusekiLexiconsUrl");
        final Query q = QueryFactory.create(sparql);
        final QueryExecution qe = QueryExecution.service(fusekiUrl).query(q).build();
        final ResultSet rs = qe.execSelect();
        final List<Entry> res_k = new ArrayList<>();
        final List<Entry> res_d = new ArrayList<>();
        while (rs.hasNext()) {
            final QuerySolution qs = rs.next();
            final Resource lx = qs.getResource("res");
            final Literal word = qs.getLiteral("word");
            final Literal def = qs.getLiteral("def");
            final Literal type = qs.getLiteral("type");
            Literal normalized = word;
            if (qs.contains("normalized"))
                normalized = qs.getLiteral("normalized");
            final Entry e = new Entry(word, normalized, def, lx, type);
            if (e.type.equals("d"))
                res_d.add(e);
            else if (e.type.equals("k"))
                res_k.add(e);
            else
                res.add(e);
        }
        Collections.sort(res_k, (d1, d2) -> { return tibUniCollator.compare(d1.word.getString(), d2.word.getString()); });
        Collections.sort(res_d, (d1, d2) -> { return tibUniCollator.compare(d1.word.getString(), d2.word.getString()); });
        res.addAll(res_k);
        res.addAll(res_d);
        return res;
    }
    
    public static int get_first_match(final List<Token> to_match, final List<Token> tokens, final int start) {
        final String firstCharTerm = to_match.get(0).charTerm;
        for (int i = start ; i < tokens.size(); i ++) {
            if (!tokens.get(i).charTerm.equals(firstCharTerm))
                continue;
            boolean found = true;
            for (int j = 1 ; j < to_match.size(); j ++) {
                if (!tokens.get(i+j).charTerm.equals(to_match.get(j).charTerm)) {
                    found = false;
                    break;
                }
            }
            if (found)
                return i;
        }
        return -1;
    }
    
    public static List<Entry> selectAndFillEntries(final List<Entry> entries, final List<Token> chunk_tokens, final List<Token> cursor_tokens, final int cursor_start_ti, final int cursor_end_ti, final boolean ignore_stop_words) throws IOException {
        final List<Entry> res = new ArrayList<>();
        final Map<String,Boolean> seenEntries = new HashMap<>();
        for (final Entry entry : entries) {
            if (entry.type.equals("d") || entry.type.equals("k")) {
                res.add(entry);
                continue;
            }
            // don't show the same entry twice (can happen in the first two types)
            if (seenEntries.containsKey(entry.res_uri))
                continue;
            seenEntries.put(entry.res_uri, true);
            log.debug("looking at {}/{}", entry.word, entry.normalized);
            final List<Token> word_tokens = getTokens(entry.normalized.getLexicalForm(), entry.normalized.getLanguage(), ignore_stop_words);
            final int match_start_ti = get_first_match(cursor_tokens, word_tokens, 0);
            if (match_start_ti == -1) {
                log.error("couldn't find {} in {}", cursor_tokens.toString(), word_tokens.toString());
                continue;
            }
            // we sort out entries that cannot possibly match fully
            if (match_start_ti > cursor_start_ti)
                continue;
            if (word_tokens.size() - match_start_ti > chunk_tokens.size() - cursor_start_ti)
                continue;
            entry.cursor_in_entry_start = word_tokens.get(match_start_ti).start;
            entry.cursor_in_entry_end = word_tokens.get(match_start_ti+cursor_tokens.size()-1).end;
            final int diff = cursor_start_ti - match_start_ti;
            boolean matches = true;
            entry.chunk_offset_end = chunk_tokens.get(cursor_end_ti).end;
            // checking if entry matches forward
            for (int i = cursor_end_ti + 1 ; i < chunk_tokens.size() ; i ++) {
                if (i - diff >= word_tokens.size()) {
                    break;
                }
                if (word_tokens.get(i-diff).charTerm.equals(chunk_tokens.get(i).charTerm)) {
                    entry.chunk_offset_end = chunk_tokens.get(i).end;
                } else {
                    matches = false;
                    break;
                }
            }
            if (!matches) {
                log.debug("not matching forward");
                continue;
            }
            // and backwards
            entry.chunk_offset_start = chunk_tokens.get(cursor_start_ti).start;
            for (int i = cursor_start_ti -1 ; i >= 0 ; i--) {
                if (i - diff < 0)
                    break;
                if (word_tokens.get(i-diff).charTerm.equals(chunk_tokens.get(i).charTerm)) {
                    entry.chunk_offset_start = chunk_tokens.get(i).start;
                } else {
                    matches = false;
                    break;
                }
            }
            if (!matches) {
                log.debug("not matching backward");
                continue;
            }
            entry.nb_tokens = word_tokens.size();
            res.add(entry);
        }
        return res;
    }
    
    public static List<Entry> getEntries(final String chunk, final String chunk_lang, final int cursor_start, final int cursor_end) throws IOException, RestException {
        log.debug("get entries for {}@{}", chunk, chunk_lang);
        List<Token> tokens = getTokens(chunk, chunk_lang, false);
        boolean ignore_stop_words = false;
        log.debug("found tokens {}", tokens);
        int[] cursor_tokens_range = getTokensRange(tokens, cursor_start, cursor_end);
        if (log.isDebugEnabled()) {
            log.debug("looking at tokens around {}", chunk.subSequence(cursor_start, cursor_end));
            log.debug("found tokens[{}:{}]", cursor_tokens_range[0], cursor_tokens_range[1]);
        }
        if (cursor_tokens_range[0] == -1) {
            ignore_stop_words = true;
            tokens = getTokens(chunk, chunk_lang, true);
            log.debug("found tokens (ignoring stop words) {}", tokens);
            cursor_tokens_range = getTokensRange(tokens, cursor_start, cursor_end);
        }
        if (cursor_tokens_range[0] == -1) {
            throw new RestException(404, 5000, "cannot find token around the cursor");
        }
        final List<Token> cursor_tokens = new ArrayList<>();
        String cursor_string = "";
        final String raw_cursor_string = chunk.substring(tokens.get(cursor_tokens_range[0]).start, tokens.get(cursor_tokens_range[1]).end)+"་";
        for (int i = cursor_tokens_range[0] ; i <= cursor_tokens_range[1] ; i++) {
            final Token t = tokens.get(i);
            cursor_tokens.add(t);
            cursor_string += t.charTerm+"་";
        }
        log.debug("cursor tokens {}, cursor string {}, raw cursor string {}", cursor_tokens, cursor_string, raw_cursor_string);
        String cursor_string_minus1 = null;
        if (cursor_tokens_range[0] > 0)
            cursor_string_minus1 = chunk.substring(tokens.get(cursor_tokens_range[0]-1).start, tokens.get(cursor_tokens_range[1]).end);
        String cursor_string_plus1 = null;
        if (cursor_tokens_range[1] < tokens.size()-1)
            cursor_string_plus1 = chunk.substring(tokens.get(cursor_tokens_range[0]).start, tokens.get(cursor_tokens_range[1]+1).end);
        List<Entry> entries = searchInFuseki(cursor_string, raw_cursor_string, cursor_string_minus1, cursor_string_plus1, chunk_lang);
        entries = selectAndFillEntries(entries, tokens, cursor_tokens, cursor_tokens_range[0], cursor_tokens_range[1], ignore_stop_words);
        return entries;
    }

}
