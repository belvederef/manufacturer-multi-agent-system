package napier.ac.uk;

import java.util.ArrayList;
import java.util.Random;

import jade.content.lang.Codec;
import jade.content.lang.Codec.CodecException;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import napier.ac.uk_ontology.ShopOntology;
import napier.ac.uk_ontology.elements.CanManufacture;
import napier.ac.uk_ontology.elements.Computer;
import napier.ac.uk_ontology.elements.Order;

public class CustomerAgent extends Agent {
  private Codec codec = new SLCodec();
  private Ontology ontology = ShopOntology.getInstance();
  
  private ArrayList<AID> manufacturers = new ArrayList<>();
  private ArrayList<Order> currentOrders = new ArrayList<>(); // The orders that the agent has made so far
  private Order order; // The order for today
  private AID tickerAgent;
  private int numQueriesSent;
  @Override
  protected void setup() {
    // Set up the ontology
    getContentManager().registerLanguage(codec);
    getContentManager().registerOntology(ontology);
    
    //add this agent to the yellow pages
    DFAgentDescription dfd = new DFAgentDescription();
    dfd.setName(getAID());
    ServiceDescription sd = new ServiceDescription();
    sd.setType("customer");
    sd.setName(getLocalName() + "-customer-agent");
    dfd.addServices(sd);
    try{
      DFService.register(this, dfd);
    }
    catch(FIPAException e){
      e.printStackTrace();
    }
    
    addBehaviour(new TickerWaiter(this));
  }


  @Override
  protected void takeDown() {
    //Deregister from the yellow pages
    try{
      DFService.deregister(this);
    }
    catch(FIPAException e){
      e.printStackTrace();
    }
  }

  public class TickerWaiter extends CyclicBehaviour {

    //behaviour to wait for a new day
    public TickerWaiter(Agent a) {
      super(a);
    }

    @Override
    public void action() {
      MessageTemplate mt = MessageTemplate.or(MessageTemplate.MatchContent("new day"),
          MessageTemplate.MatchContent("terminate"));
      ACLMessage msg = myAgent.receive(mt); 
      if(msg != null) {
        if(tickerAgent == null) {
          tickerAgent = msg.getSender();
        }
        if(msg.getContent().equals("new day")) {
          //spawn new sequential behaviour for day's activities
          SequentialBehaviour dailyActivity = new SequentialBehaviour();
          //sub-behaviours will execute in the order they are added
          dailyActivity.addSubBehaviour(new CreateOrder(myAgent));
          dailyActivity.addSubBehaviour(new FindManufacturers(myAgent));
          dailyActivity.addSubBehaviour(new AskOrder(myAgent));
//          dailyActivity.addSubBehaviour(new CollectOrders(myAgent));
          dailyActivity.addSubBehaviour(new EndDay(myAgent));
          myAgent.addBehaviour(dailyActivity);
        }
        else {
          //termination message to end simulation
          myAgent.doDelete();
        }
      }
      else{
        block();
      }
    }

  }
  
  public class CreateOrder extends OneShotBehaviour {

    public CreateOrder(Agent a) {
      super(a);
    }

    @Override
    public void action() {
      // Add a new order for each day. Prepare the content. 
      
      Random rand = new Random();
      Computer computer = new Computer();
      // Add computer
      if(rand.nextFloat() < 0.5) {
        // Desktop
        computer.setCpu("desktopCPU");
        computer.setMotherboard("desktopMotherboard");
        computer.setScreen(false);
      } else {
        // Laptop
        computer.setCpu("laptopCPU");
        computer.setMotherboard("laptopMotherboard");
        computer.setScreen(true);
      }
      if(rand.nextFloat() < 0.5) {
        computer.setRam("8GB"); 
      } else {
        computer.setRam("16GB");
      }
      if(rand.nextFloat() < 0.5) {
        computer.setHardDrive("1TB");
      } else {
        computer.setHardDrive("2TB");
      }
      if(rand.nextFloat() < 0.5) {
        computer.setOs("Windows");
      } else {
        computer.setOs("Linux");
      }
      
      order = new Order();
      order.setComputer(computer);
      int quantity = (int) Math.floor(1 + 50 * rand.nextFloat());
      order.setQuantity(quantity);
      order.setPrice(quantity * (int) Math.floor(600 + 200 * rand.nextFloat()));
      order.setDueInDays((int) Math.floor(1 + 10 * rand.nextFloat()));
      
      System.out.print("New customer agent order is: ");
      System.out.println(order);
     
    }
  }
  

  public class FindManufacturers extends OneShotBehaviour {

    public FindManufacturers(Agent a) {
      super(a);
    }

    @Override
    public void action() {
      DFAgentDescription manufacturerTemplate = new DFAgentDescription();
      ServiceDescription sd = new ServiceDescription();
      sd.setType("manufacturer");
      manufacturerTemplate.addServices(sd);
      try{
        manufacturers.clear();
        DFAgentDescription[] agentsType  = DFService.search(myAgent,manufacturerTemplate); 
        for(int i=0; i<agentsType.length; i++){
          manufacturers.add(agentsType[i].getName()); // this is the AID
        }
      }
      catch(FIPAException e) {
        e.printStackTrace();
      }

    }

  }

  public class AskOrder extends OneShotBehaviour {

    public AskOrder(Agent a) {
      super(a);
    }

    @Override
    public void action() {
      numQueriesSent = 0;
      // Prepare the Query-IF message. Asks the manufacturer to if they will accept order
      ACLMessage msg = new ACLMessage(ACLMessage.QUERY_IF);
      msg.setLanguage(codec.getName());
      msg.setOntology(ontology.getName()); 
      
      
      for(AID manufacturer : manufacturers) {
        // I am not sure whether this solution is the best. Because CanManifacture
        // expects a manifacturer AID, the line canManufacture.setManufacturer(manufacturer);
        // can only work within a loop going through all manufacturers.
        // Using msg.clearAllReceiver(); I prevent that the line send(msg); sends the same message 
        // to the same manifacturer (as send(msg) sends the message to all the AIDs in the list of 
        // receivers). Another solution could be to use msg.removeReceiver at the end of the loop
        msg.clearAllReceiver();
        msg.addReceiver(manufacturer);
        
        CanManufacture canManufacture = new CanManufacture();
        canManufacture.setManufacturer(manufacturer);
        canManufacture.setOrder(order);
        
        try {
          // Let JADE convert from Java objects to string
          getContentManager().fillContent(msg, canManufacture);
          send(msg);
          numQueriesSent++;
         }
         catch (CodecException ce) {
          ce.printStackTrace();
         }
         catch (OntologyException oe) {
          oe.printStackTrace();
         } 
        
      }
    }
  }

//  public class CollectOrders extends Behaviour {
//    private int numRepliesReceived = 0;
//    
//    public CollectOrders(Agent a) {
//      super(a);
//      currentOrders.clear();
//    }
//
//    
//    @Override
//    public void action() {
//      boolean received = false;
//      MessageTemplate mt = MessageTemplate.MatchConversationId(bookTitle);
//      ACLMessage msg = myAgent.receive(mt);
//      if(msg != null) {
//        received = true;
//        numRepliesReceived++;
//        if(msg.getPerformative() == ACLMessage.PROPOSE) {
//          //we have an offer
//          //the first offer for a book today
//          if(!currentOrders.containsKey(bookTitle)) {
//            ArrayList<Order> orders = new ArrayList<>();
//            orders.add(new Order(msg.getSender(),
//                Integer.parseInt(msg.getContent())));
//            currentOrders.put(bookTitle, orders);
//          }
//          //subsequent offers
//          else {
//            ArrayList<Order> orders = currentOrders.get(bookTitle);
//            orders.add(new Order(msg.getSender(),
//                Integer.parseInt(msg.getContent())));
//          }
//            
//        }
//
//        }
//      if(!received) {
//        block();
//      }
//    }
//
//    
//
//    @Override
//    public boolean done() {
//      return numRepliesReceived == numQueriesSent;
//    }
//
//    @Override
//    public int onEnd() {
//      //print the offers
//      for(String book : booksToBuy) {
//        if(currentOrders.containsKey(book)) {
//          ArrayList<Order> orders = currentOrders.get(book);
//          for(Offer o : orders) {
//            System.out.println(book + "," + o.getManufacturer().getLocalName() + "," + o.getPrice());
//          }
//        }
//        else {
//          System.out.println("No offers for " + book);
//        }
//      }
//      return 0;
//    }
//
//  }
  
  
  
  public class EndDay extends OneShotBehaviour {
    
    public EndDay(Agent a) {
      super(a);
    }

    @Override
    public void action() {
      ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
      msg.addReceiver(tickerAgent);
      msg.setContent("done");
      myAgent.send(msg);
      //send a message to each manufacturer informing that we have finished ordering for today
      ACLMessage manufacturerDone = new ACLMessage(ACLMessage.INFORM);
      manufacturerDone.setContent("done");
      for(AID manufacturer : manufacturers) {
        manufacturerDone.addReceiver(manufacturer);
      }
      myAgent.send(manufacturerDone);
    }
    
  }
}
