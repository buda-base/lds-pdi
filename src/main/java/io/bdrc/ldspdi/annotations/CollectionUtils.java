package io.bdrc.ldspdi.annotations;

import java.util.HashMap;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;

import io.bdrc.ldspdi.annotations.AnnotationCollectionEndpoint.Prefer;

public class CollectionUtils {

    public static final Map<Prefer,String> preferToPagePref = new HashMap<>();
    static {
        preferToPagePref.put(Prefer.MINIMAL, "pm");
        preferToPagePref.put(Prefer.IRI, "pi");
        preferToPagePref.put(Prefer.DESCRIPTION, "pd");
    }

    public static Model toW3CCollection(Model model, String fullUri, Prefer prefer) {
        Resource main = model.getResource(fullUri);
        model.add(main, RDF.type, AS.OrderedCollection);
        // TODO Auto-generated method stub
        return null;
    }

}
