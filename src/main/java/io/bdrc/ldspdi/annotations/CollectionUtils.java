package io.bdrc.ldspdi.annotations;

import java.util.HashMap;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDF;

import io.bdrc.ldspdi.annotations.AnnotationCollectionEndpoint.Prefer;

public class CollectionUtils {

    public static final Map<Prefer,String> preferToPagePref = new HashMap<>();
    static {
        preferToPagePref.put(Prefer.MINIMAL, "pm");
        preferToPagePref.put(Prefer.IRI, "pi");
        preferToPagePref.put(Prefer.DESCRIPTION, "pd");
    }

    public static Resource getPage(final int pagenum, final String collectionFullUri, final Prefer prefer) {
        final StringBuilder sb = new StringBuilder(collectionFullUri);
        sb.append('/');
        sb.append(preferToPagePref.get(prefer));
        sb.append('/');
        sb.append(pagenum);
        return ResourceFactory.createResource(sb.toString());
    }

    // transforms provided model into a valid W3C web annotation model
    public static void toW3CCollection(final Model model, final String fullUri, final Prefer prefer) {
        final Resource main = model.getResource(fullUri);
        model.add(main, RDF.type, AS.OrderedCollection);
        final Resource firstPage = getPage(1, fullUri, prefer);
        model.add(main, AS.first, firstPage);
    }

}
