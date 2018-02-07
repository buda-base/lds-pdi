package io.bdrc.ontology.service.core;

/*******************************************************************************
 * Copyright (c) 2017 Buddhist Digital Resource Center (BDRC)
 * 
 * If this file is a derivation of another work the license header will appear below; 
 * otherwise, this work is licensed under the Apache License, Version 2.0 
 * (the "License"); you may not use this file except in compliance with the License.
 * 
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * 
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.ontology.AllDifferent;
import org.apache.jena.ontology.AnnotationProperty;
import org.apache.jena.ontology.ComplementClass;
import org.apache.jena.ontology.DataRange;
import org.apache.jena.ontology.DatatypeProperty;
import org.apache.jena.ontology.EnumeratedClass;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.IntersectionClass;
import org.apache.jena.ontology.ObjectProperty;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.ontology.Ontology;
import org.apache.jena.ontology.Profile;
import org.apache.jena.ontology.Restriction;
import org.apache.jena.ontology.UnionClass;
import org.apache.jena.rdf.model.AnonId;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.RDFVisitor;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.rdf.model.impl.StmtIteratorImpl;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.util.iterator.WrappedIterator;

public class OntClassNotPresent implements OntClass {
    public static OntClassNotPresent INSTANCE = new OntClassNotPresent();

    private OntClassNotPresent() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public OntModel getOntModel() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Profile getProfile() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isOntLanguageTerm() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setSameAs(Resource res) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void addSameAs(Resource res) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public OntResource getSameAs() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ExtendedIterator<? extends Resource> listSameAs() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isSameAs(Resource res) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void removeSameAs(Resource res) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setDifferentFrom(Resource res) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void addDifferentFrom(Resource res) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public OntResource getDifferentFrom() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ExtendedIterator<? extends Resource> listDifferentFrom() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isDifferentFrom(Resource res) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void removeDifferentFrom(Resource res) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setSeeAlso(Resource res) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void addSeeAlso(Resource res) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Resource getSeeAlso() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ExtendedIterator<RDFNode> listSeeAlso() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasSeeAlso(Resource res) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void removeSeeAlso(Resource res) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setIsDefinedBy(Resource res) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void addIsDefinedBy(Resource res) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Resource getIsDefinedBy() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ExtendedIterator<RDFNode> listIsDefinedBy() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isDefinedBy(Resource res) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void removeDefinedBy(Resource res) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setVersionInfo(String info) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void addVersionInfo(String info) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public String getVersionInfo() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ExtendedIterator<String> listVersionInfo() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasVersionInfo(String info) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void removeVersionInfo(String info) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setLabel(String label, String lang) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void addLabel(String label, String lang) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void addLabel(Literal label) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public String getLabel(String lang) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ExtendedIterator<RDFNode> listLabels(String lang) {
        List<RDFNode> foo = new ArrayList<>();
        return WrappedIterator.create(foo.iterator());
    }

    @Override
    public boolean hasLabel(String label, String lang) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean hasLabel(Literal label) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void removeLabel(String label, String lang) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void removeLabel(Literal label) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setComment(String comment, String lang) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void addComment(String comment, String lang) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void addComment(Literal comment) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public String getComment(String lang) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ExtendedIterator<RDFNode> listComments(String lang) {
        List<RDFNode> foo = new ArrayList<>();
        return WrappedIterator.create(foo.iterator());
    }

    @Override
    public boolean hasComment(String comment, String lang) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean hasComment(Literal comment) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void removeComment(String comment, String lang) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void removeComment(Literal comment) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setRDFType(Resource cls) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void addRDFType(Resource cls) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Resource getRDFType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Resource getRDFType(boolean direct) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ExtendedIterator<Resource> listRDFTypes(boolean direct) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasRDFType(Resource ontClass, boolean direct) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean hasRDFType(Resource ontClass) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void removeRDFType(Resource cls) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean hasRDFType(String uri) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int getCardinality(Property p) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setPropertyValue(Property property, RDFNode value) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public RDFNode getPropertyValue(Property property) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NodeIterator listPropertyValues(Property property) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void removeProperty(Property property, RDFNode value) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void remove() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public OntProperty asProperty() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public AnnotationProperty asAnnotationProperty() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ObjectProperty asObjectProperty() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DatatypeProperty asDatatypeProperty() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Individual asIndividual() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public OntClass asClass() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Ontology asOntology() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DataRange asDataRange() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public AllDifferent asAllDifferent() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isProperty() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isAnnotationProperty() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isObjectProperty() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isDatatypeProperty() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isIndividual() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isClass() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isOntology() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isDataRange() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isAllDifferent() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public AnonId getId() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Resource inModel(Model m) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasURI(String uri) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getURI() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getNameSpace() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getLocalName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Statement getRequiredProperty(Property p) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Statement getRequiredProperty(Property p, String lang) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Statement getProperty(Property p) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Statement getProperty(Property p, String lang) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public StmtIterator listProperties(Property p) {
        List<Statement> foo = new ArrayList<>();
        return new StmtIteratorImpl(foo.iterator());
    }

    @Override
    public StmtIterator listProperties(Property p, String lang) {
        List<Statement> foo = new ArrayList<>();
        return new StmtIteratorImpl(foo.iterator());
    }

    @Override
    public StmtIterator listProperties() {
        List<Statement> foo = new ArrayList<>();
        return new StmtIteratorImpl(foo.iterator());
    }

    @Override
    public Resource addLiteral(Property p, boolean o) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Resource addLiteral(Property p, long o) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Resource addLiteral(Property p, char o) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Resource addLiteral(Property value, double d) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Resource addLiteral(Property value, float d) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Resource addLiteral(Property p, Object o) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Resource addLiteral(Property p, Literal o) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Resource addProperty(Property p, String o) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Resource addProperty(Property p, String o, String l) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Resource addProperty(Property p, String lexicalForm, RDFDatatype datatype) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Resource addProperty(Property p, RDFNode o) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasProperty(Property p) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean hasLiteral(Property p, boolean o) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean hasLiteral(Property p, long o) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean hasLiteral(Property p, char o) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean hasLiteral(Property p, double o) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean hasLiteral(Property p, float o) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean hasLiteral(Property p, Object o) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean hasProperty(Property p, String o) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean hasProperty(Property p, String o, String l) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean hasProperty(Property p, RDFNode o) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Resource removeProperties() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Resource removeAll(Property p) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Resource begin() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Resource abort() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Resource commit() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Resource getPropertyResourceValue(Property p) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isAnon() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isLiteral() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isURIResource() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isResource() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public <T extends RDFNode> T as(Class<T> view) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T extends RDFNode> boolean canAs(Class<T> view) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Model getModel() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitWith(RDFVisitor rv) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Resource asResource() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Literal asLiteral() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Node asNode() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setSuperClass(Resource cls) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void addSuperClass(Resource cls) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public OntClass getSuperClass() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ExtendedIterator<OntClass> listSuperClasses() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ExtendedIterator<OntClass> listSuperClasses(boolean direct) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasSuperClass(Resource cls) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean hasSuperClass() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean hasSuperClass(Resource cls, boolean direct) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void removeSuperClass(Resource cls) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setSubClass(Resource cls) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void addSubClass(Resource cls) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public OntClass getSubClass() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ExtendedIterator<OntClass> listSubClasses() {
        List<OntClass> foo = new ArrayList<>();
        return WrappedIterator.create(foo.iterator());
    }

    @Override
    public ExtendedIterator<OntClass> listSubClasses(boolean direct) {
        List<OntClass> foo = new ArrayList<>();
        return WrappedIterator.create(foo.iterator());
    }

    @Override
    public boolean hasSubClass(Resource cls) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean hasSubClass() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean hasSubClass(Resource cls, boolean direct) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void removeSubClass(Resource cls) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setEquivalentClass(Resource cls) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void addEquivalentClass(Resource cls) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public OntClass getEquivalentClass() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ExtendedIterator<OntClass> listEquivalentClasses() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasEquivalentClass(Resource cls) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void removeEquivalentClass(Resource cls) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setDisjointWith(Resource cls) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void addDisjointWith(Resource cls) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public OntClass getDisjointWith() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ExtendedIterator<OntClass> listDisjointWith() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isDisjointWith(Resource cls) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void removeDisjointWith(Resource cls) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public ExtendedIterator<OntProperty> listDeclaredProperties() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ExtendedIterator<OntProperty> listDeclaredProperties(boolean direct) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasDeclaredProperty(Property p, boolean direct) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public ExtendedIterator<? extends OntResource> listInstances() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ExtendedIterator<? extends OntResource> listInstances(boolean direct) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Individual createIndividual() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Individual createIndividual(String uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void dropIndividual(Resource individual) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean isHierarchyRoot() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public EnumeratedClass asEnumeratedClass() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UnionClass asUnionClass() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IntersectionClass asIntersectionClass() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ComplementClass asComplementClass() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Restriction asRestriction() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isEnumeratedClass() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isUnionClass() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isIntersectionClass() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isComplementClass() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isRestriction() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public EnumeratedClass convertToEnumeratedClass(RDFList individuals) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IntersectionClass convertToIntersectionClass(RDFList classes) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UnionClass convertToUnionClass(RDFList classes) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ComplementClass convertToComplementClass(Resource cls) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Restriction convertToRestriction(Property prop) {
        // TODO Auto-generated method stub
        return null;
    }

}

