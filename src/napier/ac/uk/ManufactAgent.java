package napier.ac.uk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jade.content.ContentElement;
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

// A manufacturer is both a buyer and a seller
public class ManufactAgent extends Agent {
  private Codec codec = new SLCodec();
  private Ontology ontology = ShopOntology.getInstance();
  
  private ArrayList<AID> suppliers = new ArrayList<>();
  private ArrayList<AID> customers = new ArrayList<>();
  
  private HashMap<AID, Order> allOrders = new HashMap<>(); // List of the computers and the agents that they are for
  private ArrayList<Object> componentsAvailable = new ArrayList<>(); // components available to build computers
  
  
  private ArrayList<String> componentsToBuy = new ArrayList<>(); // components to buy for orders
  private HashMap<String, ArrayList<Order>> currentOrders = new HashMap<>();
  private HashMap<AID,ArrayList<Computer>> computersAvailable = new HashMap<>(); // List of completed computers
  
  private AID tickerAgent;
  private int numQueriesSent;
  
  @Override
  protected void setup() {
    getContentManager().registerLanguage(codec);
    getContentManager().registerOntology(ontology);
    
    //add this agent to the yellow pages
    DFAgentDescription dfd = new DFAgentDescription();
    dfd.setName(getAID());
    ServiceDescription sd = new ServiceDescription();
    sd.setType("manufacturer");
    sd.setName(getLocalName() + "-manufacturer-agent");
    dfd.addServices(sd);
    try{
      DFService.register(this, dfd);
    }
    catch(FIPAException e){
      e.printStackTrace();
    }
    
    System.out.println("Created manufacturer");
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
          // Spawn a new sequential behaviour for the day's activities
//          SequentialBehaviour dailyActivity = new SequentialBehaviour();
          
          // Sub-behaviours will execute in the order they are added
          myAgent.addBehaviour(new FindSuppliers(myAgent));
          myAgent.addBehaviour(new FindCustomers(myAgent));
          
          // Add the cyclic behavior to a list that will remove all of them at the end of the day
          CyclicBehaviour orb = new OrderReplyBehaviour(myAgent);
          myAgent.addBehaviour(orb);
          
          ArrayList<Behaviour> cyclicBehaviours = new ArrayList<>();
          cyclicBehaviours.add(orb);
          

          myAgent.addBehaviour(new EndDayListener(myAgent, cyclicBehaviours));
          
          
//        dailyActivity.addSubBehaviour(new CollectOrders(myAgent));
//        dailyActivity.addSubBehaviour(new SendEnquiries(myAgent));
//        dailyActivity.addSubBehaviour(new CollectOffers(myAgent));
          
//          myAgent.addBehaviour(dailyActivity);
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

  public class FindSuppliers extends OneShotBehaviour {

    public FindSuppliers(Agent a) {
      super(a);
    }

    @Override
    public void action() {
      DFAgentDescription supplierTemplate = new DFAgentDescription();
      ServiceDescription sd = new ServiceDescription();
      sd.setType("supplier");
      supplierTemplate.addServices(sd);
      try{
        suppliers.clear();
        DFAgentDescription[] agentsType = DFService.search(myAgent,supplierTemplate); 
        for(int i=0; i<agentsType.length; i++){
          suppliers.add(agentsType[i].getName()); // this is the AID
//          System.out.println("found supplier " + agentsType[i].getName());
        }
      }
      catch(FIPAException e) {
        e.printStackTrace();
      }
    }
  }
  
  public class FindCustomers extends OneShotBehaviour {

    public FindCustomers(Agent a) {
      super(a);
    }

    @Override
    public void action() {
      DFAgentDescription customerTemplate = new DFAgentDescription();
      ServiceDescription sd = new ServiceDescription();
      sd.setType("customer");
      customerTemplate.addServices(sd);
      try{
        customers.clear();
        DFAgentDescription[] agentsType  = DFService.search(myAgent, customerTemplate); 
        for(int i=0; i<agentsType.length; i++){
          customers.add(agentsType[i].getName()); // this is the AID
//          System.out.println("found customer " + agentsType[i].getName());
        }
      }
      catch(FIPAException e) {
        e.printStackTrace();
      }

    }
  }
  
  private class OrderReplyBehaviour extends CyclicBehaviour{
    // This behaviour accepts or decline an order offer
    public OrderReplyBehaviour(Agent a) {
      super(a);
    }
    
    @Override
    public void action() {
      //This behaviour should only respond to QUERY_IF messages
      MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF); 
      ACLMessage msg = receive(mt);
      if(msg != null){
        try {
          ContentElement ce = null;
          
          // Print out the message content in SL
          System.out.println("\nMessage received by manufacturer from client" + msg.getContent()); 

          // Let JADE convert from String to Java objects
          // Output will be a ContentElement
          ce = getContentManager().extractContent(msg);
          if (ce instanceof CanManufacture) {
            CanManufacture canManifacture = (CanManufacture) ce;
            Order order = canManifacture.getOrder();
            Computer computer = (Computer) order.getComputer();
            
            // Extract the computer specs and print them to demonstrate use of the ontology
            System.out.println("The computer ordered is " + computer.toString());
            
            
            //check if supplier has the components in stock
//            if(itemsForSale.containsKey(cd.getSerialNumber())) {
            	// If all the components can be purchased, accept the order
//              System.out.println("Order accepted!");
//            }
//            else {
//              System.out.println("Order declined");
//            }
            
            // Accept all orders just for development
      			ACLMessage reply = msg.createReply();
      			if(true) { // TODO: build logic for accepting. e.g. if the supplier has the components in stock
      				// We can accept an order
      			  allOrders.put(msg.getSender(), order); // Add to list of orders
      				reply.setPerformative(ACLMessage.CONFIRM);
      				reply.setContent("Accepted");
      				
      				System.out.println("\nSending response to the customer. We accept.");
      			}
      			else {
      				reply.setPerformative(ACLMessage.DISCONFIRM);
      			}
      			myAgent.send(reply);
          }
        }
        catch (CodecException ce) {
          ce.printStackTrace();
        }
        catch (OntologyException oe) {
          oe.printStackTrace();
        }
      }
      else{
        block();
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
//      for(String bookTitle : booksToBuy) {
//        MessageTemplate mt = MessageTemplate.MatchConversationId(bookTitle);
//        ACLMessage msg = myAgent.receive(mt);
//        if(msg != null) {
//          received = true;
//          numRepliesReceived++;
//          if(msg.getPerformative() == ACLMessage.PROPOSE) {
//            //we have an offer
//            //the first offer for a book today
//            if(!currentOrders.containsKey(bookTitle)) {
//              ArrayList<Offer> offers = new ArrayList<>();
//              offers.add(new Offer(msg.getSender(),
//                  Integer.parseInt(msg.getContent())));
//              currentOrders.put(bookTitle, offers);
//            }
//            //subsequent offers
//            else {
//              ArrayList<Offer> offers = currentOrders.get(bookTitle);
//              offers.add(new Offer(msg.getSender(),
//                  Integer.parseInt(msg.getContent())));
//            }
//              
//          }
//
//        }
//      }
//      if(!received) {
//        block();
//      }
//    }
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
//          ArrayList<Offer> offers = currentOrders.get(book);
//          for(Offer o : offers) {
//            System.out.println(book + "," + o.getSeller().getLocalName() + "," + o.getPrice());
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
  
  
//
//  public class SendEnquiries extends OneShotBehaviour {
//
//    public SendEnquiries(Agent a) {
//      super(a);
//    }
//
//    @Override
//    public void action() {
//      //send out a call for proposals for each book
//      numQueriesSent = 0;
//      for(String bookTitle : booksToBuy) {
//        ACLMessage enquiry = new ACLMessage(ACLMessage.CFP);
//        enquiry.setContent(bookTitle);
//        enquiry.setConversationId(bookTitle);
//        for(AID seller : sellers) {
//          enquiry.addReceiver(seller);
//          numQueriesSent++;
//        }
//        myAgent.send(enquiry);
//        
//      }
//
//    }
//  }
//
//  public class CollectOffers extends Behaviour {
//    private int numRepliesReceived = 0;
//    
//    public CollectOffers(Agent a) {
//      super(a);
//      currentOrders.clear();
//    }
//
//    
//    @Override
//    public void action() {
//      boolean received = false;
//      for(String bookTitle : booksToBuy) {
//        MessageTemplate mt = MessageTemplate.MatchConversationId(bookTitle);
//        ACLMessage msg = myAgent.receive(mt);
//        if(msg != null) {
//          received = true;
//          numRepliesReceived++;
//          if(msg.getPerformative() == ACLMessage.PROPOSE) {
//            //we have an offer
//            //the first offer for a book today
//            if(!currentOrders.containsKey(bookTitle)) {
//              ArrayList<Offer> offers = new ArrayList<>();
//              offers.add(new Offer(msg.getSender(),
//                  Integer.parseInt(msg.getContent())));
//              currentOrders.put(bookTitle, offers);
//            }
//            //subsequent offers
//            else {
//              ArrayList<Offer> offers = currentOrders.get(bookTitle);
//              offers.add(new Offer(msg.getSender(),
//                  Integer.parseInt(msg.getContent())));
//            }
//              
//          }
//
//        }
//      }
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
//          ArrayList<Offer> offers = currentOrders.get(book);
//          for(Offer o : offers) {
//            System.out.println(book + "," + o.getSeller().getLocalName() + "," + o.getPrice());
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
//  
  
  
//  public class EndDay extends OneShotBehaviour {
//    // This came from the buyer agent example. Probably not necessary for this agent
//    
//    public EndDay(Agent a) {
//      super(a);
//    }
//
//    @Override
//    public void action() {
//      // Inform the other agents we are communicating with that we are done
//      ACLMessage supplierDone = new ACLMessage(ACLMessage.INFORM);
//      supplierDone.setContent("done");
//      for(AID supplier : suppliers) {
//        supplierDone.addReceiver(supplier);
//      }
//      myAgent.send(supplierDone);
//      
//      ACLMessage customerDone = new ACLMessage(ACLMessage.INFORM);
//      customerDone.setContent("done");
//      for(AID customer : customers) {
//        customerDone.addReceiver(customer);
//      }
//      myAgent.send(customerDone);
//      
//      
//     
//     // Inform the ticker agent that we are done 
//      ACLMessage doneMsg = new ACLMessage(ACLMessage.INFORM);
//      doneMsg.setContent("done");
//      doneMsg.addReceiver(tickerAgent);
//      myAgent.send(doneMsg);
//  }
  
  
  public class EndDayListener extends CyclicBehaviour {
    private int customerFinished = 0;
    private List<Behaviour> toRemove;
    
    public EndDayListener(Agent a, List<Behaviour> toRemove) {
      super(a);
      this.toRemove = toRemove;
    }

    @Override
    public void action() {
      MessageTemplate mt = MessageTemplate.MatchContent("done");
      ACLMessage msg = myAgent.receive(mt);
      if(msg != null) {
        customerFinished++;
        System.out.println("receiving from customer: " + msg.getContent());
      }
      else {
        block();
      }
      if(customerFinished == customers.size()) {
        // We are finished when we have received a finish message from all customers
        // Inform the ticker agent that we are done 
        ACLMessage doneMsg = new ACLMessage(ACLMessage.INFORM);
        doneMsg.setContent("done");
        doneMsg.addReceiver(tickerAgent);
        myAgent.send(doneMsg);
        
        // Remove cyclic behaviours
        for(Behaviour b : toRemove) {
          myAgent.removeBehaviour(b);
        }
        myAgent.removeBehaviour(this);
      }
    }
    
  }
  

}
