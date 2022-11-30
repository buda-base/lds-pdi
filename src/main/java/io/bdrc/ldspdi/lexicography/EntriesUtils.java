package io.bdrc.ldspdi.lexicography;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.Resource;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    
    public static class TokensAndRange {
        public int start;
        public int end;
        public final List<Token> tokens;
        
        public TokensAndRange(final int start, final int end, final List<Token> tokens) {
            this.start = start;
            this.end = end;
            this.tokens = tokens;
        }
    }
    
    public static TokensAndRange getTokensInRange(final List<Token> tokens, final int offset_start, final int offset_end) {
        final TokensAndRange res = new TokensAndRange(offset_start, offset_end, new ArrayList<Token>());
        for (final Token t : tokens) {
            if (t.start <= offset_end && t.end >= offset_start) {
                res.tokens.add(t);
                if (t.start < res.start)
                    res.start = t.start;
                if (t.end > res.end)
                    res.end = t.end;
            }
        }
        return res;
    }
    
    public static class Entry {
        public final String str;
        public final Resource res;
        public int nb_tokens_matching = 0;
        
        public Entry(final String str, final Resource res) {
            this.str = str;
            this.res = res;
        }
    }
    
    public static List<Entry> searchInFuseki(final String cursor_string, final String cursor_string_left, final String cursor_string_right, final List<Token> tokens) {
        final List<Entry> res = new ArrayList<>();
        
        return res;
    }
    
    public static List<EntryMatch> getOrderedEntries(final String chunk, final String chunk_lang, final int cursor_start, final int cursor_end) {
        final List<Token> tokens = getTokens(chunk, chunk_lang);
        final TokensAndRange cursor_tokens = getTokensInRange(tokens, cursor_start, cursor_end);
        final String cursor_string = chunk.substring(cursor_tokens.start, cursor_tokens.end);
        final List<Entry> entries = searchInFuseki(cursor_string, tokens);
        return null;
    }
    
}
