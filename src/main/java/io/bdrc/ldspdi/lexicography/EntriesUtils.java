package io.bdrc.ldspdi.lexicography;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.lucene.bo.TibetanAnalyzer;

public class EntriesUtils {
    
    public final static Logger log = LoggerFactory.getLogger(EntriesUtils.class);

    public static final class EntryMatch {
        public int start_in_chunk;
        public int end_in_chunk;
        public int cursor_start_in_entry;
        public int cursor_end_in_entry;
        public String url;
        public String entry;
    }
    
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
    
    public static class Entry {
        public final Literal word;
        public final Literal def;
        public final Resource res;
        public int nb_tokens_matching = 0;
        public int match_offset_start = 0;
        public int chunk_offset_start = 0;
        public int match_offset_end = 0;
        public int chunk_offset_end = 0;
        
        public Entry(final Literal word, final Literal def, final Resource res) {
            this.word = word;
            this.def = def;
            this.res = res;
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
    
    public static void fill_entry(final Entry entry, final List<Token> chunk_tokens, final List<Token> cursor_tokens, final int cursor_start_ti, final int cursor_end_ti) {
        final List<Token> word_tokens = getTokens(entry.word.getLexicalForm(), entry.word.getLanguage());
        final int match_start_ti = get_first_match(cursor_tokens, word_tokens, 0);
        if (match_start_ti == -1) {
            log.error("couldn't find {} in {}", cursor_tokens.toString(), word_tokens.toString());
            return;
        }
        int ti_start = match_start_ti;
        int ti_end = match_start_ti + cursor_tokens.size();
        final int diff = match_start_ti - cursor_start_ti;
        // adding matching tokens after the match:
        for (int i = match_start_ti + cursor_tokens.size() +1 ; i < chunk_tokens.size() ; i ++) {
            if (i - diff >= word_tokens.size())
                break;
            if (word_tokens.get(i-diff).charTerm.equals(chunk_tokens.get(i).charTerm)) {
                ti_end = i;
                entry.chunk_offset_end = chunk_tokens.get(i).end;
                entry.match_offset_end = word_tokens.get(i-diff).end;
            }
            else
                break;
        }
        for (int i = match_start_ti -1 ; i >= 0 ; i--) {
            if (i - diff <= 0)
                break;
            if (word_tokens.get(i-diff).charTerm.equals(chunk_tokens.get(i).charTerm)) {
                ti_start = i;
                entry.chunk_offset_start = chunk_tokens.get(i).start;
                entry.match_offset_start = word_tokens.get(i-diff).start;
            }
            else
                break;
        }
        entry.nb_tokens_matching = ti_end - ti_start;
    }
    
    public static List<EntryMatch> getOrderedEntries(final String chunk, final String chunk_lang, final int cursor_start, final int cursor_end) {
        final List<Token> tokens = getTokens(chunk, chunk_lang);
        final int[] cursor_tokens_range = getTokensRange(tokens, cursor_start, cursor_end);
        final List<Token> cursor_tokens = new ArrayList<>();
        for (int i = cursor_tokens_range[0] ; i <= cursor_tokens_range[1] ; i++) {
            cursor_tokens.add(tokens.get(i));
        }
        final String cursor_string = chunk.substring(tokens.get(cursor_tokens_range[0]).start, tokens.get(cursor_tokens_range[1]).start);
        String cursor_string_minus1 = null;
        if (cursor_tokens_range[0] > 0)
            cursor_string_minus1 = chunk.substring(tokens.get(cursor_tokens_range[0]-1).start, tokens.get(cursor_tokens_range[1]).start);
        String cursor_string_plus1 = null;
        if (cursor_tokens_range[1] < tokens.size()-1)
            cursor_string_plus1 = chunk.substring(tokens.get(cursor_tokens_range[0]).start, tokens.get(cursor_tokens_range[1]+1).start);
        final List<Entry> entries = searchInFuseki(cursor_string, cursor_string_minus1, cursor_string_plus1, chunk_lang);
        for (Entry e : entries) {
            fill_entry(e, tokens, cursor_tokens, cursor_tokens_range[0], cursor_tokens_range[1]);
        }
        return null;
    }
    
}
