package napier.ac.uk_ontology;

import jade.content.onto.BeanOntology;
import jade.content.onto.BeanOntologyException;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;

public class ShopOntology extends BeanOntology {
  private static final long serialVersionUID = 1L;
  
  private static Ontology instance = new ShopOntology("my_ontology");
  
  public static Ontology getInstance(){
    return instance;
  }
  //singleton pattern
  private ShopOntology(String name) {
    super(name);
//    super(name, SerializableOntology.getInstance() );
//    super(name, new Ontology[]{BasicOntology.getInstance(), SerializableOntology.getInstance()} );
    
    try {
//      ObjectSchema serializableSchema = getSchema(SerializableOntology.SERIALIZABLE);
//      SerializableOntology.getInstance().add(serializableSchema, java.util.HashMap.class);
      
      add("napier.ac.uk_ontology.concepts");
      add("napier.ac.uk_ontology.actions");
      add("napier.ac.uk_ontology.computerComponents");
      add("napier.ac.uk_ontology.predicates");
    } catch (BeanOntologyException e) {
      e.printStackTrace();
    }
  }
}
