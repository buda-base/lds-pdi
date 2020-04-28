package io.bdrc.ldspdi.ontology.service.core;

import java.util.Comparator;

import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntProperty;

import io.bdrc.libraries.Prefixes;

public class OntologyUtils {

    public final static Comparator<OntClass> ontClassComparator = new Comparator<OntClass>() {
        @Override
        public int compare(OntClass class1, OntClass class2) {
            if (Prefixes.getPrefix(class1.getNameSpace()).equals(Prefixes.getPrefix(class2.getNameSpace()))) {
                return class1.getLocalName().compareTo(class2.getLocalName());
            }
            return Prefixes.getPrefix(class1.getNameSpace()).compareTo(Prefixes.getPrefix(class2.getNameSpace()));
        }

    };

    public final static Comparator<OntClassModel> ontClassModelComparator = new Comparator<OntClassModel>() {
        @Override
        public int compare(OntClassModel class1, OntClassModel class2) {
            return ontClassComparator.compare(class1.clazz, class2.clazz);
        }

    };

    public final static Comparator<Individual> individualComparator = new Comparator<Individual>() {
        @Override
        public int compare(Individual class1, Individual class2) {
            return class1.getLocalName().compareTo(class2.getLocalName());
        }

    };

    public final static Comparator<OntProperty> propComparator = new Comparator<OntProperty>() {
        @Override
        public int compare(OntProperty prop1, OntProperty prop2) {
            if (Prefixes.getPrefix(prop1.getNameSpace()).equals(Prefixes.getPrefix(prop2.getNameSpace()))) {
                return prop1.getLocalName().compareTo(prop2.getLocalName());
            }
            return Prefixes.getPrefix(prop1.getNameSpace()).compareTo(Prefixes.getPrefix(prop2.getNameSpace()));
        }

    };

    public static boolean isClass(final String uri, OntModel ontMod) {
        if (ontMod.getOntResource(uri) != null) {
            return ontMod.getOntResource(uri).isClass();
        } else {
            return false;
        }
    }

}
