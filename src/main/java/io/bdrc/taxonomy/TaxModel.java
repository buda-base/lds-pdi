package io.bdrc.taxonomy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.reasoner.rulesys.Rule;
import org.apache.jena.reasoner.rulesys.Rule.Parser;
import org.apache.jena.vocabulary.ReasonerVocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaxModel {    
       
    public static InfModel infMod;    
    public final static Logger log=LoggerFactory.getLogger(TaxModel.class.getName());
    
    public static void init() {
        try {
            
            InputStream stream = TaxModel.class.getClassLoader().getResourceAsStream("O9TAXTBRC201605.ttl");
            Model m = ModelFactory.createDefaultModel();
            m.read(stream, "", "TURTLE");
            stream.close();
            log.info("Basic taxonomy model size >> "+m.size());
            List<Rule> rules = new ArrayList<Rule>(); 
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    TaxModel.class.getClassLoader().getResourceAsStream("taxonomy.rules")));
            Parser p = Rule.rulesParserFromReader(in);
            rules.addAll(Rule.parseRules(p));
            in.close();
            Reasoner reasoner = new GenericRuleReasoner(rules);
            reasoner.setParameter(ReasonerVocabulary.PROPruleMode, "forward");
            infMod = ModelFactory.createInfModel(reasoner, m);
            log.info("Inferred taxonomy size >> "+infMod.size());
    
        } catch (IOException io) {
            log.error("Error initializing OntModel", io);            
        }
    }
    
    public static void main(String[] args) {
        TaxModel.init();
    }

}
