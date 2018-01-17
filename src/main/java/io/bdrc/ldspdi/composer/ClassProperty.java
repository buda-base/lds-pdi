package io.bdrc.ldspdi.composer;

import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.OntResource;

public class ClassProperty {
	
	public static final String OBJECT_PROPERTY="ObjectProperty";
	public static final String DATA_TYPE_PROPERTY="DatatypeProperty";
	public static final String ANNOTATION_PROPERTY="AnnotationProperty";
	public static final String SYMMETRIC_PROPERTY="SymmetricProperty";
	public static final String FUNCTIONAL_PROPERTY="FunctionalProperty";
	public static final String CLASS_PROPERTY="ClassProperty";
	public static final String IRREFLEXIVE_PROPERTY="IrreflexiveProperty";
	
	public String className;
	public String name;
	public String rdfType;
	public String label;
	public String range;
	
	public ClassProperty(OntProperty prop, String className) {
		this.className=className;
		this.rdfType=prop.getRDFType().getLocalName();
		this.name=prop.getLocalName();
		String lab=prop.getLabel(null);
		if(lab!=null) {
			this.label=prop.getLabel(null);
		}else {
			this.label=name;
		}
		OntResource ontRes =prop.getRange();
		if(ontRes!=null) {
			this.range=ontRes.getLocalName();
		}else {
			this.range="";
		}
	}
	
	public String getClassName() {
		return className;
	}
	
	public void setClassName(String className) {
		this.className = className;
	}
	
	public String getRdfType() {
		return rdfType;
	}
	
	public void setRdfType(String rdfType) {
		this.rdfType = rdfType;
	}
	
	public String getLabel() {
		return label;
	}
	
	public void setLabel(String label) {
		this.label = label;
	}
	
	public String getRange() {
		return range;
	}
	
	public void setRange(String range) {
		this.range = range;
	}	
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "ClassProperty [className=" + className + ", rdfType=" + rdfType + ", label=" + label + ", range="
				+ range + "]";
	}

}