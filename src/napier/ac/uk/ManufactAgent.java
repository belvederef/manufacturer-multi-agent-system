package napier.ac.uk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.stream.Stream;

import jade.content.Concept;
import jade.content.ContentElement;
import jade.content.abs.AbsPredicate;
import jade.content.abs.AbsVariable;
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
import napier.ac.uk.helpers.OrderManuf;
import napier.ac.uk_ontology.ShopOntology;
import napier.ac.uk_ontology.actions.AskSuppInfo;
import napier.ac.uk_ontology.actions.BuyComponents;
import napier.ac.uk_ontology.actions.MakeOrder;
import napier.ac.uk_ontology.concepts.Computer;
import napier.ac.uk_ontology.concepts.ComputerComponent;
import napier.ac.uk_ontology.concepts.Order;
import napier.ac.uk_ontology.predicates.CanManufacture;
import napier.ac.uk_ontology.predicates.SendSuppInfo;
import napier.ac.uk_ontology.predicates.OwnsComponents;
import napier.ac.uk_ontology.predicates.ShipComponents;
import napier.ac.uk_ontology.predicates.ShipOrder;

// A manufacturer is both a buyer and a seller
public class ManufactAgent extends Agent {
  private static final long serialVersionUID = 1L;
  
  private Codec codec = new SLCodec();
  private Ontology ontology = ShopOntology.getInstance();
  
  private ArrayList<AID> suppliers = new ArrayList<>();
  private ArrayList<AID> customers = new ArrayList<>();
  
  private ArrayList<OrderManuf> orders = new ArrayList<>();
//  private HashMap<AID, ArrayList<Order>> ordersApproved = new HashMap<>(); // List of the orders we said yes to
  // Note: having an additional list prevents an agent to simply request us to complete an order. 
  // They can only ask, if we said yes then they could request us to complete the order.
//  private HashMap<AID, ArrayList<Order>> ordersConfirmed = new HashMap<>(); // List of the orders and the agents that they are for
//  private HashMap<Order, AID> ordersBestSupp = new HashMap<>(); // List of orders and their best suppliers to buy from
  
  private HashMap<ComputerComponent, Integer> componentsOrdered = new HashMap<>(); // The orders accepted by supplier so far, quantity
  private HashMap<ComputerComponent, Integer> componentsAvailable = new HashMap<>(); // components available to build computers, quantity
  private HashMap<ComputerComponent, Integer> componentsReserved = new HashMap<>(); // components reserved for orders accepted, quantity

  private HashMap<AID, HashMap<ComputerComponent, Integer>> priceLists = new HashMap<>(); // suppliers pricelists
  private HashMap<AID, Integer> suppDeliveryDays= new HashMap<>(); // suppliers speed
  
  private double dailyProfit = 0;
  private double totalProfit = 0;
  
  private AID tickerAgent;
  private int day = 0;
  
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
          // TODO: ASK SIMON. HOW TO USE ABSTRACT CLASS FOR COMPUTER
          // ASK ABOUT INHERITANCE AND OVERRIDING OF ATTRIBUTES LIKE LAPTOP SCREEN
          
          // I can use a for loop in a one shot behav to a request for all the components I need 
          // Assume the agents will never fail, so always get a response
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
          // TODO: explain in the report that for making the app simple we ask the full list of components
          // to the supplier, whereas in a real system we would query the price only for the components that
          // we need to buy
          
          
          priceLists.clear();
          
          
          // Spawn a new sequential behaviour for the day's activities
          SequentialBehaviour dailyActivity = new SequentialBehaviour();
          
          // Sub-behaviours will execute in the order they are added
          dailyActivity.addSubBehaviour(new FindCustomers(myAgent));
          dailyActivity.addSubBehaviour(new FindSuppliers(myAgent));
          dailyActivity.addSubBehaviour(new GetInfoFromSuppliers(myAgent));
          dailyActivity.addSubBehaviour(new OrderReplyBehaviour(myAgent));
          dailyActivity.addSubBehaviour(new CollectOrderRequests(myAgent));
          dailyActivity.addSubBehaviour(new AskIfCanBuy(myAgent));
          dailyActivity.addSubBehaviour(new BuyComponentAction(myAgent));
          dailyActivity.addSubBehaviour(new ReceiveComponent(myAgent));
          dailyActivity.addSubBehaviour(new ManufactureAndSend(myAgent));
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
  
  
  // Get price lists from all suppliers
  public class GetInfoFromSuppliers extends Behaviour {
    private static final long serialVersionUID = 1L;
    
    MessageTemplate mt;
    private ACLMessage msg; // TODO: might remove message from here and use one for each step
    private int step = 0;
    private int supplierListReceived = 0;

    public GetInfoFromSuppliers(Agent a) {
      super(a);
    }

    @Override
    public void action() {
      switch (step) {  
      case 0:        
        // Prepare the action request message
        ACLMessage enquiryMsg = new ACLMessage(ACLMessage.REQUEST);
        enquiryMsg.setLanguage(codec.getName());
        enquiryMsg.setOntology(ontology.getName()); 
        enquiryMsg.setConversationId("supplier-info");
        
        AskSuppInfo askSuppInfo = new AskSuppInfo();
        askSuppInfo.setBuyer(myAgent.getAID());
        
        Action request = new Action();
        request.setAction(askSuppInfo);
        
        try {
          for (AID supplier : suppliers) {
            enquiryMsg.addReceiver(supplier);
            request.setActor(supplier);
            getContentManager().fillContent(enquiryMsg, request);
            send(enquiryMsg);
            enquiryMsg.removeReceiver(supplier); // Remove to not send to same supp multiple times
          }
          step++;
         }
         catch (CodecException ce) {
          ce.printStackTrace();
         }
         catch (OntologyException oe) {
          oe.printStackTrace();
         } 

        
      case 1:
        // Receive the price list from all suppliers      
        mt = MessageTemplate.and(
            MessageTemplate.MatchPerformative(ACLMessage.INFORM),
            MessageTemplate.MatchConversationId("supplier-info"));
        msg = receive(mt);
        
        if(msg != null){
          try {
            ContentElement ce = null;
            
            // Print out the message content in SL
            System.out.println("\nMessage received by manufacturer from supplier: " + msg.getContent()); 

            ce = getContentManager().extractContent(msg);
            if (ce instanceof SendSuppInfo) {
              SendSuppInfo sendSuppInfo = (SendSuppInfo) ce;
              
              // Could not be able to send HashMaps by message. I am transforming the hashmap
              // and two lists, keys and values. The end result is the same. Reassemble at this end
              ArrayList<ComputerComponent> compsKeys = sendSuppInfo.getComponentsForSaleKeys();
              ArrayList<Long> compsValues = sendSuppInfo.getComponentsForSaleVal();

              // Info retrieved from message
              AID supplier = sendSuppInfo.getSupplier();
              int speed = sendSuppInfo.getSpeed();
              HashMap<ComputerComponent, Integer> priceList = new HashMap<>();
              for(int i=0; i<compsKeys.size(); i++) {
                int price = compsValues.get(i).intValue();
                priceList.put(compsKeys.get(i), price);
              }
              
              
              priceLists.put(supplier, priceList); // Add to the known priceLists
              suppDeliveryDays.put(supplier, speed); // Add supplier speed for later reference
              
              System.out.println("Received price list is: " + priceList);
              supplierListReceived++;
            } else {
              System.out.println("Unknown predicate " + ce.getClass().getName());
            }
          } catch (CodecException ce) {
            ce.printStackTrace();
          } catch (OntologyException oe) {
            oe.printStackTrace();
          }
        } else {
          block();
        }
      }
    }

    @Override
    public boolean done() {
      return supplierListReceived == suppliers.size();
    }
  }
  
  
  // Thought process:
  // 1 - Get price lists from all suppliers - DINE in previous function
  // 2 - Listen for query-ifs from customers. Decide if accepting or not
  // 3 - Send reply back to customer
  // 4 - If the customer order is accepted, order the needed components from supplier 
  private class OrderReplyBehaviour extends Behaviour{
    private static final long serialVersionUID = 1L;
    
    private OrderManuf order;
    MessageTemplate mt;
    private ACLMessage msg; // TODO: might remove message from here and use one for each step
    private int step = 0;
    private int repliesSent = 0;
    private double profit;
    
    // This behaviour accepts or decline an order offer
    // It cycles until responses from all customers have been received
    public OrderReplyBehaviour(Agent a) {
      super(a);
    }
    
    @Override
    public void action() {
      switch (step) {  
      case 0:
        // Receive order and calculate profit
        // This behaviour should only respond to QUERY_IF messages
        mt = MessageTemplate.and(
            MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF),
            MessageTemplate.MatchConversationId("customer-order"));
        msg = receive(mt);
        
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
              Order tmpOrder = canManifacture.getOrder();
              order = new OrderManuf(tmpOrder);
              order.setCustomer(msg.getSender());
              
              Computer computer = (Computer) order.getComputer();
              
              // Extract the computer specs and print them
              System.out.println("The computer ordered is " + computer.toString());
              msg.setConversationId("customer-order");
//              ArrayList<ComputerComponent> componentsNeeded = new ArrayList<>();
              
              Boolean allCompsAvailable = true;
              for (ComputerComponent comp : order.getComputer().getComponentList()) {
                // If there are not enough components in the warehouse, flag as false
                if(!componentsAvailable.containsKey(comp) || 
                    (componentsAvailable.containsKey(comp) && 
                     componentsAvailable.get(comp) < order.getQuantity())) {
                  allCompsAvailable = false;
                  break;
                }
              }
              
              // Calculate profit
//              if(!allCompsAvailable) {
                // Query the supplier for the needed components and then calcute profit
//                step = 1;
//              } else {
              
             // If all components are available, calculate profit. If positive, accept
            // If I already have components available it is because I bought them from the
            // cheap supplier, knowing that I would have needed them
                
            // Profit in this case is the price the customer pays minus what I paid for the
            // components of the cheap supplier
            // so as long as the price that the customer pays us is higher than the price we
            // paid for the components, there is profit
                
            // minus what paid for comps can happen at the end of the day, so dont mind that for this 
            // simple model. Can point that out in the report. 
                
              
              
              // Calculate how much it would cost to get all the needed components for each supplier
              // Keep in mind we might have components available already
            
              // TODO: remove components already available from the total cost
              // AID, cost for all components for all computers
              HashMap <AID, Double> supplierCosts = new HashMap<>();
              for (AID supplier : suppliers) { // for each supplier
                double totCost = 0;
                for(ComputerComponent comp : order.getComputer().getComponentList()) { // Loop for all the components needed
                  if (comp != null) { // Some component, like the laptop screen, can be null
                    totCost += priceLists.get(supplier).get(comp);
                  }
                }
                
                totCost *= order.getQuantity();
                supplierCosts.put(supplier, totCost);
              }
              
              
              // Pick the supplier that sells the components at the cheapest price, with the constraint that it needs
              // to still deliver within the due days. ADVANCED: we can still accept order where the profit is still
              // positive even though we will be fined for late delivery
              // TODO: recheck this. Look at the advanced note above
              int daysDue = order.getDueInDays();
              AID bestSupplier = null;
              
              for (Entry<AID, Double> suppAndCost : supplierCosts.entrySet()) {
                AID thisSupplier = suppAndCost.getKey();
                int suppDelivDays = suppDeliveryDays.get(thisSupplier);
                
                if (bestSupplier == null && suppDelivDays <= daysDue) {
                    // If there is no best supplier, get the first that can deliver within the time limit
                  bestSupplier = thisSupplier;
                } else if (suppDelivDays <= daysDue
                    && suppAndCost.getValue() < supplierCosts.get(bestSupplier)) {
                  // If can deliver within time constraint and cost is lower
                  bestSupplier = thisSupplier;
                }
              }
              
              System.out.println("Speed required in days is: " + daysDue);
              System.out.println("The best supplier for this order is: " + bestSupplier);
              order.setSupplierAssigned(bestSupplier);
              
              profit = order.getPrice() - supplierCosts.get(bestSupplier);
              System.out.println("Profit for this order is: " + profit);
              dailyProfit += profit;
              
              // TODO: the PenaltyForLateOrders can be calculated at this stage as this
              // wont change. Maybe not actually as we will be getting new components from
              // the cheapest supplier on a daily basis
              
              step++;              
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
        } else {
          block();
        }
      break;
        
        
      case 1:
        // Profit on a single day 
//        TotalValueOfOrdersShipped(d)  – PenaltyForLateOrders(d) –
//        WarehouseStorage(d) – SuppliesPurchased(d),
        
//        where TotalValueOfOrdersShipped(d)  is  the price of  all orders  shipped to  
//        customers on  day d,  PenaltyForLateOrders(d) is  the penalty for any accepted  
//        orders  where the due date  has passed  and they  have  not been  shipped as  of  the 
//        end of  day d (Section  2.4), WarehouseStorage(d) is  the cost  of  all items currently 
//        in  the warehouse that  did not arrive  on  day d (Section  2.3), and 
//        SuppliesPurchased(d)  is  the cost  of  all components  purchased on  day d (Section  
//        2.3).
        
//        For the initial evaluation  of  your  system  you may assume  that  the number  of  
//        customers is 3 (c=3), the per-day late  delivery  penalty for an  order is  £50 
//        (p=50), and the cost  of  warehouse storage per-day per-component is  £5  
//        (w=5).
        
        
        
        // Send response
          ACLMessage reply = msg.createReply();
          if(profit > 0) { // if profit is positive
            
            
            // Add to list of orders that we said yes to, but not yet confirmed
            order.setOrderState(OrderManuf.State.APPROVED);
            orders.add(order);
            
//            if (ordersApproved.containsKey(msg.getSender())) {
//              // If customer has already made an order, add to that list
//              ordersApproved.get(msg.getSender()).add(order);
//            } else {
//              ArrayList<Order> orderList = new ArrayList<>();
//              orderList.add(order);
//              ordersApproved.put(msg.getSender(), orderList);
//            }
            
            reply.setPerformative(ACLMessage.CONFIRM);
            
            System.out.println("\nSending response to the customer. We accept.");
          } else {
            reply.setPerformative(ACLMessage.DISCONFIRM);
          }
          myAgent.send(reply);
          repliesSent++;
          step = 0; // This order was processed, reset cycle
          break;
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
      MessageTemplate mt = MessageTemplate.and(
          MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
          MessageTemplate.MatchConversationId("customer-order"));
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
              
              // if the order was approved, change its state from approved to confirmed              
              int idxOrder = orders.indexOf(makeOrder.getOrder());
              if (idxOrder != -1) {
                orders.get(idxOrder).setOrderState(OrderManuf.State.CONFIRMED);
                ordersReceived++;
                System.out.println("\nAdded to confirmed orders. List of orders at "
                    + "the end of CollectOrderRequests is: " + orders);
              } else {
                // If the order was not approved in the previous step, don't accept order request
                System.out.println("\nThis order was not approved! Cannot process order!");
              }
              
              
//              int idxOrder = ordersApproved.get(makeOrder.getBuyer()).indexOf(makeOrder.getOrder());
//              
//              System.out.println("\nmakeOrder.getOrder(): " + makeOrder.getOrder());
//              System.out.println("\nFirst order approved for that buyer: " + ordersApproved.get(makeOrder.getBuyer()).get(0));
//              
//              if (idxOrder != -1) {
//                // Runs if order is in list of orders we approved
//                Order orderToMove = ordersApproved.get(makeOrder.getBuyer()).get(idxOrder);
//                
//                // Removed from approved list
//                ordersApproved.get(makeOrder.getBuyer()).remove(idxOrder);
//                
//                // Add to confirmed list
//                if (ordersConfirmed.containsKey(makeOrder.getBuyer())) {
//                  // If customer has already made an order, add to that list
//                  ordersConfirmed.get(makeOrder.getBuyer()).add(orderToMove);
//                } else {
//                  ArrayList<Order> orderListConf = new ArrayList<>();
//                  orderListConf.add(orderToMove);
//                  ordersConfirmed.put(makeOrder.getBuyer(), orderListConf);
//                }
//                
//                ordersReceived++;
//                System.out.println("\nAdded to confirmed orders. List of confirmed orders at the end of CollectOrderRequests is: " + ordersConfirmed);
//                
//              } else {
//                // If the order was not approved in the previous step, don't accept order request
//                System.out.println("\nThis order was not approved! Cannot process order!");
//              }
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
        block();
      }
    }

    @Override
    public boolean done() {
      // TODO: dev only
      if (ordersReceived == orders.stream()
          .filter(o -> o.getOrderState() == OrderManuf.State.CONFIRMED).count()) {
        System.out.println("CollectOrderRequests is done. done is true");  
      }
      
      return ordersReceived == orders.stream()
          .filter(o -> o.getOrderState() == OrderManuf.State.CONFIRMED).count();
    }
  }
  
  
  public class AskIfCanBuy extends OneShotBehaviour {
    private static final long serialVersionUID = 1L;
    // Note: here we ask if the supplier owns the component and the quantity we need 
    // in a real system, if the supplier only owned a set quantity of components, we would
    // buy that quantity and buy the remaining from a more expensive supplier, perhaps.
    // This was out of the scope for this simple model.
    
    // TODO: This should be equal to the number or components that I need to query
    // TODO: change OwnsComponent with smth like askIfCanBuy. Better than just asking
    // if they have the things
//    private int ordersAsked = 0;

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
      
//      for (AID cust : ordersConfirmed.keySet()) { // for each customer
//        for (Order order : ordersConfirmed.get(cust)) { // for each cust order
      

      for (OrderManuf order : orders) {
        // Send requests only for confirmed orders
        if (order.getOrderState() != OrderManuf.State.CONFIRMED) continue;
        
        AID supplier = order.getSupplierAssigned();  
        
        ACLMessage msg = new ACLMessage(ACLMessage.QUERY_IF);
        msg.setLanguage(codec.getName());
        msg.setOntology(ontology.getName()); 
        msg.addReceiver(supplier);
        msg.setConversationId("component-selling");
        
        OwnsComponents ownsComps = new OwnsComponents();
        ownsComps.setOwner(supplier);
        ownsComps.setQuantity(order.getQuantity());
        
        try {
          // Note: something that could be done in a real system is, if supllier doesnt have 
          // the components that we require, buy from the second best only the ones that 
          // the best cant give us 
          ownsComps.setComponents(order.getComputer().getComponentList());                            
          getContentManager().fillContent(msg, ownsComps);
          send(msg);
//          ordersAsked++;
         } catch (CodecException ce) {
          ce.printStackTrace();
         } catch (OntologyException oe) {
          oe.printStackTrace();
         } catch (Exception e) {
           e.printStackTrace();
         }
      }
    }

//    @Override
//    public boolean done() {
//      return ordersAsked == orders.stream()
//        .filter(o -> o.getOrderState() == OrderManuf.State.CONFIRMED).count();
//    }
  }

  
  public class BuyComponentAction extends Behaviour {
    private static final long serialVersionUID = 1L;
    
    // Make order for a component if the manufacturer said they accepted it
    // Otherwise proceed to end the day
    
    // TODO: using the boolean value below only works if we are dealing with one supplier!
    // Change logic for dealing with more suppliers
    
    // TODO: This should be equal to the number of components that I need to request
    
    // TODO: send money when you request to buy a component
    private int requestsSent = 0;
    
    public BuyComponentAction(Agent a) {
      super(a);
    }

    @Override
    public void action() {      
      MessageTemplate mt = MessageTemplate.and(
          MessageTemplate.MatchPerformative(ACLMessage.CONFIRM),
          MessageTemplate.MatchConversationId("component-selling"));
      
      ACLMessage msg = myAgent.receive(mt);
      System.out.println("\nmessage received in BuyComponentAction is: " + msg);
      if(msg != null) {
//        replyReceived = true;
        if(msg.getPerformative() == ACLMessage.CONFIRM) {
          // The supplier has the components in stock and confirmed
          System.out.println("\nThe supplier has the components in stock and confirmed! YAY! Now making request...");
          
          // Prepare the action request message
          ACLMessage orderMsg = new ACLMessage(ACLMessage.REQUEST);
          orderMsg.setLanguage(codec.getName());
          orderMsg.setOntology(ontology.getName()); 
          orderMsg.setConversationId("component-selling");
          orderMsg.addReceiver(msg.getSender());
          
//          try {            
//            // Lets take into consideration the orders of the first customer
//            ArrayList<Order> firstCustOrders = ordersConfirmed.get(customers.get(0));
//            
//            // Prepare the content. 
//            BuyComponents buyComponent = new BuyComponents();
//            buyComponent.setBuyer(myAgent.getAID());
//            buyComponent.setComponent(firstCustOrders.get(0).getComputer().getCpu());
//            
//            Action request = new Action();
//            request.setAction(buyComponent);
//            request.setActor(suppliers.get(0));
//  
//             getContentManager().fillContent(orderMsg, request); //send the wrapper object
//             send(orderMsg);
//             requestsSent++;
//             System.out.println("Sending order request to supplier. msg is: " + orderMsg);
//             // Add this order to the list of ordered components that we are awaiting to receive
////             componentsOrdered.add(firstCustOrders.get(0).getComputer().getCpu()); 
//          }
//          catch (CodecException ce) {
//           ce.printStackTrace();
//          }
//          catch (OntologyException oe) {
//           oe.printStackTrace();
//          } 
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
  
  public class ReceiveComponent extends Behaviour {
    private static final long serialVersionUID = 1L;
    
    private int componentsReceived = 0;
    
    public ReceiveComponent(Agent a) {
      super(a);
    }
    
    // TODO: keep a list of components ordered with the AID of the supplier that has to send them to us.
    // then pop() the order from the orders we are awaiting and add it to the orders available 
    @Override
    public void action() { 
      MessageTemplate mt = MessageTemplate.and(
          MessageTemplate.MatchPerformative(ACLMessage.INFORM),
          MessageTemplate.MatchConversationId("component-selling"));
      ACLMessage msg = receive(mt);
      
      if(msg != null){
        try {
          ContentElement ce = null;
          
          // Print out the message content in SL
          System.out.println("\nMessage received by manufacturer from supplier " + msg.getContent()); 

          ce = getContentManager().extractContent(msg);
          if (ce instanceof ShipComponents) {
            ShipComponents shipComponent = (ShipComponents) ce;
            ComputerComponent component = shipComponent.getComponent();
            
            // Extract the received component and print it
            System.out.println("Received component " + component.toString());
            
//            componentsAvailable.add(component);
            componentsReceived++;
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
      // TODO Auto-generated method stub
      return componentsReceived >= 1;
    }
  }
  
  
  private class ManufactureAndSend extends Behaviour {
    private static final long serialVersionUID = 1L;
    
    private int sentOrders = 0;

    public ManufactureAndSend(Agent a) {
      super(a);
    }

    @Override
    public void action() {
      // for each order
      // If have enough components, manufacture components into an order and send to customer
      // TODO: remove components from componentsAvailable list when using them
      
      AID cust = customers.get(0);
      Order order = ordersConfirmed.get(cust).get(0);
      
      // Prepare the INFORM message. Asks the manufacturer to if they will accept the order
      ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
      msg.setLanguage(codec.getName());
      msg.setOntology(ontology.getName()); 
      msg.addReceiver(cust);
      
      ShipOrder shipOrder = new ShipOrder();
      shipOrder.setBuyer(cust);
      shipOrder.setOrder(order);
      
      try {
        // Fill content
        getContentManager().fillContent(msg, shipOrder);
        send(msg);
        sentOrders++;
        System.out.println("Sending order " + order + " to cust " + cust.getLocalName());
       }
       catch (CodecException ce) {
        ce.printStackTrace();
       }
       catch (OntologyException oe) {
        oe.printStackTrace();
       } 
    }
      

    @Override
    public boolean done() {
      // TODO Auto-generated method stub
      return sentOrders >= 1;
    }

  }
  
  
  public class EndDay extends OneShotBehaviour {
    private static final long serialVersionUID = 1L;
    
    public EndDay(Agent a) {
      super(a);
    }

    @Override
    public void action() {
      // TODO: Calculate loss on profit (for each of the components in the warehouse)
      // total profit - components
      
      // Add to the total profit
      totalProfit += dailyProfit;
      
      System.out.println("\nToday's profit was: " + dailyProfit);
      System.out.println("Total profit is: " + totalProfit);
      
      
      // Inform the ticker agent and the manufacturer that we are done 
      ACLMessage doneMsg = new ACLMessage(ACLMessage.INFORM);
      doneMsg.setContent("done");
      doneMsg.addReceiver(tickerAgent);
      for(AID supplier : suppliers) {
        doneMsg.addReceiver(supplier);
      }
      
      myAgent.send(doneMsg);
      day++;
    }
  }
  
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
}
