package napier.ac.uk_ontology;

import jade.content.onto.BeanOntology;
import jade.content.onto.BeanOntologyException;
import jade.content.onto.Ontology;

public class ShopOntology extends BeanOntology{
  
  private static Ontology instance = new ShopOntology("my_ontology");
  
  public static Ontology getInstance(){
    return instance;
  }
  //singleton pattern
  private ShopOntology(String name) {
    super(name);
    try {
      add("napier.ac.uk_ontology.concepts");
      add("napier.ac.uk_ontology.predicates");
      add("napier.ac.uk_ontology.actions");
      add("napier.ac.uk_ontology.computerComponents");
    } catch (BeanOntologyException e) {
      e.printStackTrace();
    }
  }
}
