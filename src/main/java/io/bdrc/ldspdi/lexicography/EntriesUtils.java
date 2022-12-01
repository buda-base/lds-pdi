package io.bdrc.ldspdi.lexicography;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.datatypes.RDFDatatype;
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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.lucene.bo.TibetanAnalyzer;

public class EntriesUtils {
    
    public final static Logger log = LoggerFactory.getLogger(EntriesUtils.class);
    
    public static final class Token {
        public final int start;
        public final int end;
        public final String charTerm;
        
        public Token(final int start, final int end, final String charTerm) {
            this.start = start;
            this.end = end;
            this.charTerm = charTerm;
        }
    }
    
    static private List<Token> getTokens(final String inputStr, final String inputStr_lang) {
        final String method = inputStr_lang.equals("bo-x-ewts") ? "ewts" : "unicode";
        final TibetanAnalyzer ta;
        try {
            ta = new TibetanAnalyzer(false, true, true, method, "");
        } catch (IOException e1) {
            log.error("can't initialize Tibetan analyzer");
            return null;
        }
        final TokenStream ts =  ta.tokenStream("", inputStr);
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
            ta.close();
        }
        return res;
    }
    
    public static int[] getTokensRange(final List<Token> tokens, final int offset_start, final int offset_end) {
        final int[] res = new int[2];
        final boolean first_seen = false;
        int t_i = 0;
        for (final Token t : tokens) {
            if (t.start <= offset_end && t.end >= offset_start) {
                if (!first_seen)
                    res[0] = t_i; 
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
        
        public Entry(final Literal word, final Literal def, final Resource res) {
            this.word = word;
            this.def = def;
            this.res_uri = res.getURI();
        }
    }
    
    public static List<Entry> searchInFuseki(final String cursor_string, final String cursor_string_left, final String cursor_string_right, final String lang) {
        final List<Entry> res = new ArrayList<>();
        String sparql = "select distinct res, word, def where {";
        sparql +=       " {  ?res <https://www.w3.org/ns/lemon/ontolex#writtenForm> \""+cursor_string+"\"@"+lang+" . BIND(\""+cursor_string+"\"@"+lang+" as ?word) ?res <http://purl.bdrc.io/ontology/core/definitionGMD> ?def . }";
        if (cursor_string_left != null) {
            sparql +=   " union { (?res ?sc ?word) <http://jena.apache.org/text#query> (<https://www.w3.org/ns/lemon/ontolex#writtenForm> \"\\\""+cursor_string_left+"\\\"\"@"+lang+") . ?res <http://purl.bdrc.io/ontology/core/definitionGMD> ?def . }";
        }
        if (cursor_string_right != null) {
            sparql +=   " union { (?res ?sc ?word) <http://jena.apache.org/text#query> (<https://www.w3.org/ns/lemon/ontolex#writtenForm> \"\\\""+cursor_string_right+"\\\"\"@"+lang+") . ?res <http://purl.bdrc.io/ontology/core/definitionGMD> ?def . }";
        }
        sparql +=       "} limit 200";
        log.info(sparql);
        final String fusekiUrl = ServiceConfig.getProperty("fusekiLexiconsUrl");
        final Query q = QueryFactory.create(sparql);
        final QueryExecution qe = QueryExecution.service(fusekiUrl).query(q).build();
        final ResultSet rs = qe.execSelect();
        while (rs.hasNext()) {
            final QuerySolution qs = rs.next();
            final Resource lx = qs.getResource("res");
            final Literal word = qs.getLiteral("word");
            final Literal def = qs.getLiteral("def");
            final Entry e = new Entry(word, def, lx);
            res.add(e);
        }
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
    
    public static List<Entry> selectAndFillEntries(final List<Entry> entries, final List<Token> chunk_tokens, final List<Token> cursor_tokens, final int cursor_start_ti, final int cursor_end_ti) {
        final List<Entry> res = new ArrayList<>();
        for (final Entry entry : entries) {
            final List<Token> word_tokens = getTokens(entry.word.getLexicalForm(), entry.word.getLanguage());
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
            entry.cursor_in_entry_end = word_tokens.get(match_start_ti+cursor_tokens.size()).end;
            final int diff = match_start_ti - cursor_start_ti;
            boolean matches = false;
            // checking if entry matches forward
            for (int i = match_start_ti + cursor_tokens.size() +1 ; i < chunk_tokens.size() ; i ++) {
                if (i - diff >= word_tokens.size())
                    break;
                if (word_tokens.get(i-diff).charTerm.equals(chunk_tokens.get(i).charTerm)) {
                    entry.chunk_offset_end = chunk_tokens.get(i).end;
                } else {
                    matches = false;
                    break;
                }
            }
            if (!matches)
                continue;
            // and backwards
            for (int i = match_start_ti -1 ; i >= 0 ; i--) {
                if (i - diff <= 0)
                    break;
                if (word_tokens.get(i-diff).charTerm.equals(chunk_tokens.get(i).charTerm)) {
                    entry.chunk_offset_start = chunk_tokens.get(i).start;
                } else {
                    matches = false;
                    break;
                }
            }
            if (!matches)
                continue;
            entry.nb_tokens = word_tokens.size();
            res.add(entry);
        }
        return res;
    }
    
    public static List<Entry> getEntries(final String chunk, final String chunk_lang, final int cursor_start, final int cursor_end) {
        final List<Token> tokens = getTokens(chunk, chunk_lang);
        final int[] cursor_tokens_range = getTokensRange(tokens, cursor_start, cursor_end);
        final List<Token> cursor_tokens = new ArrayList<>();
        String cursor_string = "";
        for (int i = cursor_tokens_range[0] ; i <= cursor_tokens_range[1] ; i++) {
            final Token t = tokens.get(i);
            cursor_tokens.add(t);
            cursor_string += t.charTerm+"་";
        }
        String cursor_string_minus1 = null;
        if (cursor_tokens_range[0] > 0)
            cursor_string_minus1 = chunk.substring(tokens.get(cursor_tokens_range[0]-1).start, tokens.get(cursor_tokens_range[1]).start);
        String cursor_string_plus1 = null;
        if (cursor_tokens_range[1] < tokens.size()-1)
            cursor_string_plus1 = chunk.substring(tokens.get(cursor_tokens_range[0]).start, tokens.get(cursor_tokens_range[1]+1).start);
        List<Entry> entries = searchInFuseki(cursor_string, cursor_string_minus1, cursor_string_plus1, chunk_lang);
        entries = selectAndFillEntries(entries, tokens, cursor_tokens, cursor_tokens_range[0], cursor_tokens_range[1]);
        return entries;
    }
    
}
