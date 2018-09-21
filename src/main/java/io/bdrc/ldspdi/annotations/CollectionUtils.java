package io.bdrc.ldspdi.annotations;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Selector;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;

import io.bdrc.ldspdi.annotations.AnnotationCollectionEndpoint.Prefer;

public class CollectionUtils {

    public static final String BDO = "http://purl.bdrc.io/ontology/core/";
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

    static class SimpleUriResourceComparator implements Comparator<Resource>
    {
        @Override
        public int compare(Resource s1, Resource s2)
        {
            return s1.getURI().compareTo(s2.getURI());
        }
    }

    // transforms provided model into a valid W3C web annotation model
    public static void toW3CCollection(final Model model, final String fullUri, final Prefer prefer) {
        final Resource main = model.getResource(fullUri);
        model.add(main, RDF.type, AS.OrderedCollection);
        final Resource firstPage = getPage(1, fullUri, prefer);
        model.add(main, AS.first, firstPage);
        if (prefer == Prefer.MINIMAL)
            return;
        final Property annInCollection = model.createProperty(BDO, "annInCollection");
        model.add(firstPage, RDF.type, AS.OrderedCollectionPage);
        model.add(firstPage, AS.partOf, main);
        model.add(firstPage, AS.startIndex, model.createTypedLiteral(0, XSDDatatype.XSDinteger));
        int total = 0;
        final Set<Resource> items = new TreeSet<Resource>(new SimpleUriResourceComparator());
        final Selector sel = new SimpleSelector(null, annInCollection, main);
        final StmtIterator it =  model.listStatements(sel);
        while (it.hasNext()) {
            final Statement currentS = it.next();
            final Resource ann = currentS.getSubject();
            items.add(ann);
            total += 1;
            it.remove();
        }
        Resource itemsL = model.createList(items.iterator());
        model.add(firstPage, AS.items, itemsL);
        model.add(main, AS.totalItems, model.createTypedLiteral(total, XSDDatatype.XSDinteger));
    }

}
