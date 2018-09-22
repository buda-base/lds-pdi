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
import io.bdrc.ldspdi.sparql.LdsQuery;
import io.bdrc.ldspdi.sparql.LdsQueryService;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.restapi.exceptions.LdsError;
import io.bdrc.restapi.exceptions.RestException;

public class CollectionUtils {

    public static final String BDO = "http://purl.bdrc.io/ontology/core/";
    public static final Map<Prefer,String> preferToPagePref = new HashMap<>();
    public static final Map<Character,Prefer> pageUrlCharToPrefer = new HashMap<>();

    // this artificial integer is used indicate "until the end" of a resource
    // when subsetting collections. It will be a problem if we have texts of more
    // than 2 billion unicode characters.
    public static final int rangeIdxInfinit = 2000000000;

    public static enum SubsetType {
        VOLUMEPAGES,
        ETEXTCHARS,
        NONE
        ;
    }

    public static final Map<String,SubsetType> subsetUrlEltToSubsetType = new HashMap<>();
    public static final Map<SubsetType,String> subsetToURI = new HashMap<>();

    static {
        preferToPagePref.put(Prefer.MINIMAL, "pm");
        preferToPagePref.put(Prefer.IRI, "pi");
        preferToPagePref.put(Prefer.DESCRIPTION, "pd");

        //pageUrlCharToPrefer.put('m', Prefer.MINIMAL); // pages in minimal mode don't make sense
        pageUrlCharToPrefer.put('i', Prefer.IRI);
        pageUrlCharToPrefer.put('d', Prefer.DESCRIPTION);

        subsetUrlEltToSubsetType.put("pages", SubsetType.VOLUMEPAGES);
        subsetUrlEltToSubsetType.put("chars", SubsetType.ETEXTCHARS);

        subsetToURI.put(SubsetType.VOLUMEPAGES, "http://purl.bdrc.io/resource/SubsettingImageVolume");
        subsetToURI.put(SubsetType.ETEXTCHARS, "http://purl.bdrc.io/resource/SubsettingChars");
        subsetToURI.put(SubsetType.NONE, "http://purl.bdrc.io/resource/SubsettingNone");
    }



    public static Resource getPageResource(final int pagenum, final String collectionFullUri, final Prefer prefer) {
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
        final Resource firstPage = getPageResource(1, fullUri, prefer);
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

    public static Integer[] getRangeFromUrlElt(final String subcoordinates) throws NumberFormatException {
        final int dashidx = subcoordinates.indexOf('-');
        final int strlen = subcoordinates.length();
        if (strlen == 0)
            throw new NumberFormatException();
        int rangebegin;
        int rangeend;
        if (dashidx == -1) {
            rangebegin = Integer.parseInt(subcoordinates);
            if (rangebegin < 0)
                throw new NumberFormatException();
            return new Integer[] {rangebegin, rangebegin};
        }
        if (dashidx == 0) {
            rangeend = Integer.parseInt(subcoordinates.substring(1));
            if (rangeend < 1)
                throw new NumberFormatException();
            return new Integer[] {0, rangeend};
        }
        if (dashidx == strlen-1) {
            rangebegin = Integer.parseInt(subcoordinates.substring(0, dashidx));
            return new Integer[] {rangebegin, rangeIdxInfinit};
        }
        rangebegin = Integer.parseInt(subcoordinates.substring(0, dashidx));
        rangeend = Integer.parseInt(subcoordinates.substring(dashidx+1));
        return new Integer[] {rangebegin, rangeend};
    }

    public static Model getSubsetGraph(final String prefixedRes, final Prefer prefer, final String fusekiUrl,
            final String subtypeUrlElt, final String subcoordinates, final String resAlias) throws RestException {
        final SubsetType subType = subsetUrlEltToSubsetType.get(subtypeUrlElt);
        if (subType == null) {
            throw new RestException(404, new LdsError(LdsError.NO_GRAPH_ERR).setContext(prefixedRes));
        }
        final Integer[] range;
        try {
            range = getRangeFromUrlElt(subcoordinates);
        } catch (NumberFormatException e) {
            throw new RestException(404, new LdsError(LdsError.NO_GRAPH_ERR).setContext(prefixedRes));
        }
        return getSubsetGraph(prefixedRes, prefer, fusekiUrl, subType, range, resAlias);
    }

    public static Model getSubsetGraph(final String prefixedRes, final Prefer prefer, final String fusekiUrl,
            final SubsetType subType, final Integer[] range, final String resAlias) throws RestException {
        final String queryFileName = AnnotationCollectionEndpoint.preferToQueryFile.get(prefer);
        final LdsQuery qfp = LdsQueryService.get(queryFileName,"library");
        final Map<String,String> args = new HashMap<>();
        args.put("R_SUBMETHOD", subsetToURI.get(subType));
        args.put("R_RES", prefixedRes);
        args.put("R_RESALIAS", prefixedRes);
        args.put("I_SUBRANGEFIRST", range[0].toString());
        args.put("I_SUBRANGELAST", range[1].toString());
        final String query = qfp.getParametizedQuery(args, false);
        return QueryProcessor.getGraph(query, fusekiUrl, null);
    }


}
