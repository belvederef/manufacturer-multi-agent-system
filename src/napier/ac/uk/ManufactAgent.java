package napier.ac.uk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jade.content.Concept;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.lang.Codec.CodecException;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
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
import napier.ac.uk_ontology.elements.Computer;
import napier.ac.uk_ontology.elements.Order;
import napier.ac.uk_ontology.elements.actions.BuyComponent;
import napier.ac.uk_ontology.elements.actions.MakeOrder;
import napier.ac.uk_ontology.elements.computerComponents.ComputerComponent;
import napier.ac.uk_ontology.elements.predicates.CanManufacture;
import napier.ac.uk_ontology.elements.predicates.OwnsComponent;

// A manufacturer is both a buyer and a seller
public class ManufactAgent extends Agent {
  private static final long serialVersionUID = 1L;
  
  private Codec codec = new SLCodec();
  private Ontology ontology = ShopOntology.getInstance();
  
  private ArrayList<AID> suppliers = new ArrayList<>();
  private ArrayList<AID> customers = new ArrayList<>();
  
  private HashMap<AID, ArrayList<Order>> ordersApproved = new HashMap<>(); // List of the orders we said yes to
  // Note: having an additional list prevents an agent to simply request us to complete an order. 
  // They can only ask, if we said yes then they could request us to complete the order.
  private HashMap<AID, ArrayList<Order>> ordersConfirmed = new HashMap<>(); // List of the orders and the agents that they are for
  
  private ArrayList<ComputerComponent> componentsOrdered = new ArrayList<>(); // The orders that were accepted so far
  private ArrayList<ComputerComponent> componentsAvailable = new ArrayList<>(); // components available to build computers
  
  private AID tickerAgent;
  
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
    private static final long serialVersionUID = 1L;

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
          // TODO: Manufacturer should use a sequential behaviour!!! Only supplier should be cyclic
            // The manufacturer can be totally synchronous for the scope of this coursework
          // I can use a for loop in a one shot behav to a request for all the components I need 
          // Assume the agents will never fail, so always get a response
          // Introduce the conversation id to match the correct messages
          // Can match on the predicate/action as well using get getContentManager().extractContent
          // have a look at the JADE response things that Simon said, so that ShipOrder or ShipComponent
            // are predicates that respond to the actions. I can use an INFORM message that contains 
            // those predicates
          
          // TODO: explain in the paper that we developed the app in a very simplistic way, not ideal
          // for real world usage. This is because customers could make orders at any time during a day,
          // we do not account for that because after customers are found we do not keep searching.
          // Also, we do not account for failure. If one of the agents suddenly stopped functioning, the
          // other agents, who depend on the responses of some other agents will be stuck in a loop.
          // A proposed solution would be to use cyclic behaviours for every behaviour so that any
          // of them could receive new input. 
          
          
          // Spawn a new sequential behaviour for the day's activities
          SequentialBehaviour dailyActivity = new SequentialBehaviour();
          
          // Sub-behaviours will execute in the order they are added
          dailyActivity.addSubBehaviour(new FindSuppliers(myAgent));
          dailyActivity.addSubBehaviour(new FindCustomers(myAgent));
          dailyActivity.addSubBehaviour(new OrderReplyBehaviour(myAgent));
          dailyActivity.addSubBehaviour(new CollectOrderRequests(myAgent));
          dailyActivity.addSubBehaviour(new AskIfCanBuy(myAgent));
          dailyActivity.addSubBehaviour(new BuyComponentAction(myAgent));
          dailyActivity.addSubBehaviour(new EndDay(myAgent));
          
          myAgent.addBehaviour(dailyActivity);
        } else {
          //termination message to end simulation
          myAgent.doDelete();
        }
      } else {
        block();
      }
    }

  }

  public class FindSuppliers extends OneShotBehaviour {
    private static final long serialVersionUID = 1L;

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
    private static final long serialVersionUID = 1L;

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
  
  private class OrderReplyBehaviour extends Behaviour{
    private static final long serialVersionUID = 1L;
    
    private int repliesSent = 0;
    
    // This behaviour accepts or decline an order offer
    // It cycles until responses from all customers have been received
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
            
            // Extract the computer specs and print them
            System.out.println("The computer ordered is " + computer.toString());
            
            
            // Accept all orders just for development
      			ACLMessage reply = msg.createReply();
      			if(true) { 
      			  // TODO: build logic for accepting. e.g. if the supplier has the components in stock or we do
      			  
      			  // Add to list of orders that we said yes to, but not yet confirmed
      			  if (ordersApproved.containsKey(msg.getSender())) {
      			    // If customer has already made an order, add to that list
      			    ordersApproved.get(msg.getSender()).add(order);
      			  } else {
      			    ArrayList<Order> orderList = new ArrayList<>();
      			    orderList.add(order);
      			    ordersApproved.put(msg.getSender(), orderList);
      			  }
      			  
      				reply.setPerformative(ACLMessage.CONFIRM);
      				reply.setContent("Accepted");
      				
      				System.out.println("\nSending response to the customer. We accept.");
      			} else {
      				reply.setPerformative(ACLMessage.DISCONFIRM);
      			}
      			myAgent.send(reply);
      			repliesSent++;
          } else {
              System.out.println("Unknown predicate " + ce.getClass().getName());
          }
        }
        catch (CodecException ce) {
          ce.printStackTrace();
        }
        catch (OntologyException oe) {
          oe.printStackTrace();
        }
      } else{
        block();
      }
    }

    @Override
    public boolean done() {
      // TODO: dev only
      if (repliesSent == customers.size()) {
        System.out.println("OrderReplyBehaviour is done. done is true");  
      }
      
      return repliesSent == customers.size();
    }
  }
  
  
  private class CollectOrderRequests extends Behaviour{
    private static final long serialVersionUID = 1L;
    
    private int ordersReceived = 0;
    
    // This behaviour accepts the requests for the order it has accepted in the previous query_if
    // This behaviour accepts the order requests we said yes to, if the customer still wants them
    public CollectOrderRequests(Agent a) {
      super(a);
    }
    
    @Override
    public void action() {
      // This behaviour should only respond to REQUEST messages
      MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST); 
      ACLMessage msg = receive(mt);
      if(msg != null){
        try {
          ContentElement ce = null;
          System.out.println("\nmessage received in CollectOrderRequests is: " + msg.getContent());

          // Let JADE convert from String to Java objects
          ce = getContentManager().extractContent(msg);
          if(ce instanceof Action) {
            Concept action = ((Action)ce).getAction();
            if (action instanceof MakeOrder) {
              MakeOrder makeOrder = (MakeOrder)action;
              
              // if the same combination AID + order is present in the ordersApproved, move it from approved to
              // confirmed
              int idxOrder = ordersApproved.get(makeOrder.getBuyer()).indexOf(makeOrder.getOrder());
              
              System.out.println("\nmakeOrder.getOrder(): " + makeOrder.getOrder());
              System.out.println("\nFirst order approved for that buyer: " + ordersApproved.get(makeOrder.getBuyer()).get(0));
              
              if (idxOrder != -1) {
                // Runs if order is in list of orders we approved
                Order orderToMove = ordersApproved.get(makeOrder.getBuyer()).get(idxOrder);
                
                // Removed from approved list
                ordersApproved.get(makeOrder.getBuyer()).remove(idxOrder);
                
                // Add to confirmed list
                if (ordersConfirmed.containsKey(makeOrder.getBuyer())) {
                  // If customer has already made an order, add to that list
                  ordersConfirmed.get(makeOrder.getBuyer()).add(orderToMove);
                } else {
                  ArrayList<Order> orderListConf = new ArrayList<>();
                  orderListConf.add(orderToMove);
                  ordersConfirmed.put(makeOrder.getBuyer(), orderListConf);
                }
                ordersReceived++;
                System.out.println("\nAdded to confirmed orders. List of confirmed orders at the end of CollectOrderRequests is: " + ordersConfirmed);
                
              } else {
                // If the order was not approved in the previous step, don't accept order request
                System.out.println("\nThis order was not approved! Cannot process order!");
              }
            }
          }
        } catch (CodecException ce) {
          ce.printStackTrace();
        } catch (OntologyException oe) {
          oe.printStackTrace();
        } catch (Exception e) {
          e.printStackTrace();
        }
      } else {
        // If no message is received
        block();
      }
    }

    @Override
    public boolean done() {
      // TODO: dev only
      if (ordersReceived == customers.size()) {
        System.out.println("CollectOrderRequests is done. done is true");  
      }
      
      return ordersReceived == customers.size();
    }
  }
  
  
//  private class BuyComponents extends OneShotBehaviour{
//    // This behaviour buys the components needed to make the orders that we have on the list of confirmed orders
//    public BuyComponents(Agent a) {
//      super(a);
//    }
//    
//    @Override
//    public void action() {
//      // This behaviour should only respond to REQUEST messages
//      MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST); 
//      ACLMessage msg = receive(mt);
//      if(msg != null){
//        try {
//          ContentElement ce = null;
//          System.out.println(msg.getContent());
//
//          // Let JADE convert from String to Java objects
//          ce = getContentManager().extractContent(msg);
//          if(ce instanceof Action) {
//            Concept action = ((Action)ce).getAction();
//            if (action instanceof MakeOrder) {
//              MakeOrder makeOrder = (MakeOrder) action;
//              // TODO: the components of the orders added to ordersConfirmed should be bought in the next step 
//              ordersConfirmed.put(makeOrder.getBuyer(), makeOrder.getOrder());
//            }
//          }
//        } catch (CodecException ce) {
//          ce.printStackTrace();
//        } catch (OntologyException oe) {
//          oe.printStackTrace();
//        }
//      }
//      else{
//        block();
//      }
//    }
//  }
  
  
  public class AskIfCanBuy extends Behaviour {
    private static final long serialVersionUID = 1L;
    
    // TODO: This should be equal to the number or components that I need to query
    private int queryCount = 0;

    public AskIfCanBuy(Agent a) {
      super(a);
    }

    @Override
    public void action() {
      // TODO: this behaviour throws an error because it executes before ordersConfirmed is populated
      // TODO: this should run only after we add all requests to the confirmed order. Might achieve this with sequential behaviour
      // TODO: add logic to decide to which seller we want to send the message to and change suppliers.get(0) to the chosen one
      
      // Prepare the Query-IF message. Asks the supplier if they will accept the order
      
      // this should only run if something along the lines of this happens: 
      // for each of the components that I need, if I didnt ask for that component yet or if
      // my question did not get accepted, keep asking
      ACLMessage msg = new ACLMessage(ACLMessage.QUERY_IF);
      msg.setLanguage(codec.getName());
      msg.setOntology(ontology.getName()); 
      msg.setConversationId("manufacturer-order");
      msg.addReceiver(suppliers.get(0));
      
      OwnsComponent ownsComp = new OwnsComponent();
      ownsComp.setOwner(suppliers.get(0));
      if (ordersConfirmed.isEmpty()) {
        return;  
      }
      
      try {
        // TODO: NOTE the null exception error will go once I change stuff like customers.get(0) with 
        // actual implementation. remove if checks when that happens
        // Lets take into consideration the orders of the first customer
        ArrayList<Order> firstCustOrders = ordersConfirmed.get(customers.get(0));
        
        System.out.println("Ask the supplier if they have the component in stock. In AskIfCanBuy");
        System.out.println("In AskIfCanBuy. firstCustOrders: " + firstCustOrders);
        
        if (firstCustOrders == null || firstCustOrders.isEmpty()) {
          block(); 
          return;
        }
        ownsComp.setComponent(firstCustOrders.get(0).getComputer().getCpu()); // Loop to ask about all components of an order
        
        // Let JADE convert from Java objects to string
        getContentManager().fillContent(msg, ownsComp);
        send(msg);
        queryCount++;
      
       } catch (CodecException ce) {
        ce.printStackTrace();
       } catch (OntologyException oe) {
        oe.printStackTrace();
       } catch (Exception e) {
         e.printStackTrace();
       }
    }

    @Override
    public boolean done() {
      // TODO: change this
      return queryCount >= 1;
    }
  }

  
  public class BuyComponentAction extends Behaviour {
    private static final long serialVersionUID = 1L;
    
    // Make order for a component if the manufacturer said they accepted it
    // Otherwise proceed to end the day
    
    // TODO: using the boolean value below only works if we are dealing with one supplier!
    // Change logic for dealing with more suppliers
    
    // TODO: This should be equal to the number of components that I need to request
    private int requestsSent = 0;
    
    public BuyComponentAction(Agent a) {
      super(a);
    }

    @Override
    public void action() {
      // TODO: should probably match on some ontology
//      MessageTemplate mt = MessageTemplate.MatchOntology(value)
//          MatchSender(suppliers.get(0));
      
//      MessageTemplate.MatchConversationId("manufacturer-order")
      
      MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchConversationId("manufacturer-order"),
          MessageTemplate.MatchPerformative(ACLMessage.CONFIRM));
      
      ACLMessage msg = myAgent.receive(mt);
      System.out.println("\nmessage received in BuyComponentAction is: " + msg);
      if(msg != null) {
//        replyReceived = true;
        if(msg.getPerformative() == ACLMessage.CONFIRM) {
          // The order was accepted
          System.out.println("\nThe supplier accepted the component order! YAY! Now making request...");
          
          try {
            // Prepare the action request message
            ACLMessage orderMsg = new ACLMessage(ACLMessage.REQUEST);
            orderMsg.setLanguage(codec.getName());
            orderMsg.setOntology(ontology.getName()); 
            orderMsg.addReceiver(msg.getSender());
            System.out.println("Sending order request to supplier1. msg is: " + orderMsg);
            // Lets take into consideration the orders of the first customer
            ArrayList<Order> firstCustOrders = ordersConfirmed.get(customers.get(0));
            
            // Prepare the content. 
            BuyComponent buyComponent = new BuyComponent();
            buyComponent.setBuyer(myAgent.getAID());
            buyComponent.setComponent(firstCustOrders.get(0).getComputer().getCpu());
            
            Action request = new Action();
            request.setAction(buyComponent);
            request.setActor(suppliers.get(0));
  
             getContentManager().fillContent(orderMsg, request); //send the wrapper object
             send(orderMsg);
             requestsSent++;
             System.out.println("Sending order request to supplier. msg is: " + orderMsg);
             // Add this order to the list of ordered components that we are awaiting to receive
             componentsOrdered.add(firstCustOrders.get(0).getComputer().getCpu()); 
          }
          catch (CodecException ce) {
           ce.printStackTrace();
          }
          catch (OntologyException oe) {
           oe.printStackTrace();
          } 
        } else if(msg.getPerformative() == ACLMessage.REFUSE) {
          // The order was not accepted
          System.out.println("\nThe supplier did not accept the component order!");
        }
      } else {
        block();
      }
    }

    @Override
    public boolean done() {
      System.out.println("BuyComponentAction is done. requestsSent is: " + requestsSent);
      // TODO: change this
      return requestsSent >= 1;
    }

    @Override
    public int onEnd() {
      // Do something on end
      return 0;
    }

  }
  
  // Should there be a function awaiting for components to be received?
  
  
  
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
  
  
//  public class EndDayListener extends CyclicBehaviour {
//    private static final long serialVersionUID = 1L;
//    
//    private int customerFinished = 0;
////    private int supplierFinished = 0;
//    private List<Behaviour> toRemove;
//    
//    public EndDayListener(Agent a, List<Behaviour> toRemove) {
//      super(a);
//      this.toRemove = toRemove;
//    }
//
//    @Override
//    public void action() {
//      MessageTemplate mt = MessageTemplate.MatchContent("done");
//      ACLMessage msg = myAgent.receive(mt);
//      if(msg != null) {
//        customerFinished++;
//      } else {
//        block();
//      }
//      
//      if(customerFinished == customers.size()) {
//        // We are finished when we have received a finish message from all customers
//        // Inform the ticker agent that we are done 
//        ACLMessage doneMsg = new ACLMessage(ACLMessage.INFORM);
//        doneMsg.setContent("done");
//        doneMsg.addReceiver(tickerAgent);
//        myAgent.send(doneMsg);
//        
//        // Inform the suppliers that we are done
//        ACLMessage supplierDone = new ACLMessage(ACLMessage.INFORM);
//        supplierDone.setContent("done");
//        for(AID supplier : suppliers) {
//          supplierDone.addReceiver(supplier);
//        }
//        myAgent.send(supplierDone);
//        
//        // Remove cyclic behaviours
//        for(Behaviour b : toRemove) {
//          myAgent.removeBehaviour(b);
//        }
//        myAgent.removeBehaviour(this);
//      }
//    }
//  }
  
  public class EndDay extends OneShotBehaviour {
    private static final long serialVersionUID = 1L;
    
    public EndDay(Agent a) {
      super(a);
    }

    @Override
    public void action() {
      // Inform the ticker agent and the manufacturer that we are done 
      ACLMessage doneMsg = new ACLMessage(ACLMessage.INFORM);
      doneMsg.setContent("done");
      doneMsg.addReceiver(tickerAgent);
//      doneMsg.addReceiver(manufacturer);
      // TODO: remove the "done" message passing from agents to manufac
      
      myAgent.send(doneMsg);
    }
  }
}
