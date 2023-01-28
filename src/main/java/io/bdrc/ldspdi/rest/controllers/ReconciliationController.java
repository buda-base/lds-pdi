package io.bdrc.ldspdi.rest.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.bdrc.ldspdi.exceptions.RestException;

@RestController
@RequestMapping("/")
public class ReconciliationController {

    /*
     * Implementation of the Reconciliation API
     * https://reconciliation-api.github.io/specs/0.1/
     */
    
    
    public final static class PropertyValue {
        // pid
        // v
    }
    
    public final static class Query {
        // query
        // type (string?)
        // limit (int)
        // properties list of Property (pid, v)
        // type_strict ("should", "all" or "any")
    }
    
    public final static class QueryBatch {
        // query_id -> query
    }
    
    final static List<String> prefixes = new ArrayList<>();
    final static List<String> suffixes = new ArrayList<>();
    final static Pattern prefixPattern;
    final static Pattern suffixPattern;
    static {
        // discussed in https://github.com/buda-base/library-issues/issues/466
        prefixes.add("mkhan [pm]o ");
        prefixes.add("rgya gar kyi ");
        prefixes.add("mkhan chen ");
        prefixes.add("a lag ");
        prefixes.add("a khu ");
        prefixes.add("rgan ");
        prefixes.add("rgan lags ");
        prefixes.add("zhabs drung "); // can appear in the middle of words
        prefixes.add("mkhas grub ");
        prefixes.add("mkhas dbang ");
        prefixes.add("mkhas pa ");
        prefixes.add("bla ma ");
        prefixes.add("sman pa "); // ?
        prefixes.add("em chi "); // ?
        prefixes.add("yongs 'dzin "); // ?
        prefixes.add("ma hA ");
        prefixes.add("sngags pa ");
        prefixes.add("sngags mo ");
        prefixes.add("sngags pa'i rgyal po ");
        prefixes.add("sems dpa' chen po ");
        prefixes.add("rnal 'byor [pm]a ");
        prefixes.add("rje ");
        prefixes.add("rje btsun ");
        prefixes.add("rje btsun [pm]a ");
        prefixes.add("kun mkhyen ");
        prefixes.add("lo tsA ba ");
        prefixes.add("lo tswa ba ");
        prefixes.add("lo cA ba ");
        prefixes.add("lo chen ");
        prefixes.add("slob dpon ");
        prefixes.add("paN\\+Di ta ");
        prefixes.add("paN chen ");
        prefixes.add("srI ");
        prefixes.add("dpal ");
        prefixes.add("dge slong ");
        prefixes.add("dge slong ma ");
        prefixes.add("dge bshes ");
        prefixes.add("dge ba'i bshes gnyen ");
        prefixes.add("shAkya'i dge slong ");
        prefixes.add("'phags pa ");
        prefixes.add("A rya ");
        prefixes.add("gu ru ");
        prefixes.add("sprul sku ");
        prefixes.add("a ni ");
        prefixes.add("a ni lags ");
        prefixes.add("rig 'dzin ");
        prefixes.add("chen [pm]o ");
        prefixes.add("A tsar\\+yA ");
        prefixes.add("gter ston ");
        prefixes.add("gter chen ");
        prefixes.add("thams cad mkhyen pa ");
        prefixes.add("rgyal dbang ");
        prefixes.add("rgyal ba ");
        prefixes.add("btsun [pm]a ");
        prefixes.add("dge rgan ");
        prefixes.add("theg pa chen po'i ");
        // prefixes found in Mongolian names
        prefixes.add("hor ");
        prefixes.add("sog [pm]o ");
        prefixes.add("sog ");
        prefixes.add("a lags sha ");
        prefixes.add("khal kha ");
        prefixes.add("cha har ");
        prefixes.add("jung gar ");
        prefixes.add("o rad ");
        prefixes.add("hor chin ");
        prefixes.add("thu med ");
        prefixes.add("hor pa ");
        prefixes.add("na'i man ");
        prefixes.add("ne nam ");
        prefixes.add("su nyid ");
        prefixes.add("har chen ");
        
        suffixes.add(" dpal bzang po");
        suffixes.add(" lags");
        suffixes.add(" rin po che");
        suffixes.add(" sprul sku");
        suffixes.add(" le'u");
        suffixes.add(" rgyud kyi rgyal po");
        suffixes.add(" bzhugs so");
        suffixes.add(" sku gzhogs");
        suffixes.add(" (c|[sz])es bya ba");
        
        String patStr = String.join("|", prefixes);
        prefixPattern = Pattern.compile("^(?:"+patStr+")+");
        patStr = String.join("|", suffixes);
        suffixPattern = Pattern.compile("(?:"+patStr+")+$");
    }
    
    public static String normalize(String orig, final String type) {
        // TODO: if Tibetan Unicode, convert to Wylie
        String repl = orig;
        repl = repl.replaceAll("[\\s#_/\\-\\*\\.@\\d\\(\\)]+$", "");
        repl = repl.replaceAll("^[\\s#_/@\\*]+", "");
        repl = prefixPattern.matcher(repl).replaceAll("");
        // we add a space at the beginning so that suffixes can match from the start
        repl = suffixPattern.matcher(" "+repl).replaceAll("");
        if (repl.length() == 0 && orig.length() > 0)
            return orig;
        // offset by 1 because of space we added earlier
        return repl.substring(1);
    }
    
    @GetMapping(value = "/reconciliation/{lang}/service")
    public ResponseEntity<String> getResourceGraph(@PathVariable String res,
            @PathVariable("lang") String ext)
            throws RestException, IOException {
        return ResponseEntity.status(200).header("Content-Type", "application/json")
                .body(null);
    }
    
    @PostMapping(path = "/{lang}/query",
            consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    public ResponseEntity<String> query(@RequestParam Map<String,String> paramMap) {
        final String jsonStr = paramMap.get("queries");
        return ResponseEntity.status(200).header("Content-Type", "application/json")
                .body(null);
    }
    
}
