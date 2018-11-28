package napier.ac.uk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

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
import napier.ac.uk.helpers.OrderWrapper;
import napier.ac.uk.helpers.SupplierHelper;
import napier.ac.uk_ontology.ShopOntology;
import napier.ac.uk_ontology.actions.AskSuppInfo;
import napier.ac.uk_ontology.actions.BuyComponents;
import napier.ac.uk_ontology.actions.MakeOrder;
import napier.ac.uk_ontology.computerComponents.OsLinux;
import napier.ac.uk_ontology.computerComponents.OsWindows;
import napier.ac.uk_ontology.concepts.ComputerComponent;
import napier.ac.uk_ontology.concepts.Order;
import napier.ac.uk_ontology.predicates.CanManufacture;
import napier.ac.uk_ontology.predicates.OwnsComponents;
import napier.ac.uk_ontology.predicates.SendPayment;
import napier.ac.uk_ontology.predicates.SendSuppInfo;
import napier.ac.uk_ontology.predicates.ShipComponents;
import napier.ac.uk_ontology.predicates.ShipOrder;

// A manufacturer is both a buyer and a seller
public class ManufactAgent extends Agent {
  private static final long serialVersionUID = 1L;
  private static final AtomicInteger orderIds = new AtomicInteger(0); 
  
  private Codec codec = new SLCodec();
  private Ontology ontology = ShopOntology.getInstance();
  
  private HashMap<AID, SupplierHelper> suppliers = new HashMap<>();
  private ArrayList<AID> customers = new ArrayList<>();
  
  private ArrayList<OrderWrapper> orders = new ArrayList<>();
  private HashMap<ComputerComponent, Integer> warehouse = new HashMap<>(); // components available to build computers, quantity
  
  private double dailyProfit = 0;
  private double totalProfit = 0;
  
  private AID tickerAgent;
  private int day = 1;
  private double lastSevenDayCosts=0;
  
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
    
    try {
      DFService.register(this, dfd);
    } catch(FIPAException e){
      e.printStackTrace();
    }
    
    addBehaviour(new TickerWaiter(this));
  }


  @Override
  protected void takeDown() {
    // Deregister from the yellow pages
    try {
      DFService.deregister(this);
    } catch(FIPAException e){
      e.printStackTrace();
    }
  }

  
  public class TickerWaiter extends CyclicBehaviour {
    private static final long serialVersionUID = 1L;

    // behaviour to wait for a new day
    public TickerWaiter(Agent a) {
      super(a);
    }

    @Override
    public void action() {
      MessageTemplate mt = MessageTemplate.or(MessageTemplate.MatchContent("new day"),
          MessageTemplate.MatchContent("terminate"));
      ACLMessage msg = myAgent.receive(mt); 
      
      if(msg != null) {
        if(tickerAgent == null) { tickerAgent = msg.getSender(); }
        
        if(msg.getContent().equals("new day")) {          
          // Spawn a new sequential behaviour for the day's activities
          SequentialBehaviour dailyActivity = new SequentialBehaviour();
          
          // Sub-behaviours execute in the order they are added
          dailyActivity.addSubBehaviour(new FindCustomers(myAgent));
          dailyActivity.addSubBehaviour(new FindSuppliers(myAgent));
          dailyActivity.addSubBehaviour(new GetInfoFromSuppliers(myAgent));
          dailyActivity.addSubBehaviour(new OrderReplyBehaviour(myAgent));
          dailyActivity.addSubBehaviour(new CollectOrderRequests(myAgent));
          dailyActivity.addSubBehaviour(new ReceiveComponents(myAgent));
          dailyActivity.addSubBehaviour(new ManufactureAndSend(myAgent));
          dailyActivity.addSubBehaviour(new ReceivePayment(myAgent));
          dailyActivity.addSubBehaviour(new EndDay(myAgent));
          
          myAgent.addBehaviour(dailyActivity);
        } else {
          // termination message to end simulation
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
      
      try {
        customers.clear();
        DFAgentDescription[] agentsType  = DFService.search(myAgent, customerTemplate); 
        
        for(int i=0; i<agentsType.length; i++){
          customers.add(agentsType[i].getName());
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
      
      try {
        suppliers.clear();
        DFAgentDescription[] agentsType = DFService.search(myAgent,supplierTemplate);
        
        for(int i=0; i<agentsType.length; i++){
          AID aid = agentsType[i].getName();
          suppliers.put(aid, new SupplierHelper(aid));
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
    private ACLMessage msg;
    private int step = 0;
    private int supplierListReceived = 0;

    public GetInfoFromSuppliers(Agent a) {
      super(a);
    }

    @Override
    public void action() {
      switch (step) {  
      case 0:        
        ACLMessage enquiryMsg = new ACLMessage(ACLMessage.REQUEST);
        enquiryMsg.setLanguage(codec.getName());
        enquiryMsg.setOntology(ontology.getName()); 
        enquiryMsg.setConversationId("supplier-info");
        
        AskSuppInfo askSuppInfo = new AskSuppInfo();
        askSuppInfo.setBuyer(myAgent.getAID());
        
        Action request = new Action();
        request.setAction(askSuppInfo);
        
        try {
          for (AID supplier : suppliers.keySet()) {
            enquiryMsg.addReceiver(supplier);
            request.setActor(supplier);
            getContentManager().fillContent(enquiryMsg, request);
            send(enquiryMsg);
            enquiryMsg.removeReceiver(supplier); // Remove to not send msg to same supp multiple times
          }
          step++;
         }
         catch (CodecException ce) {
          ce.printStackTrace();
         }
         catch (OntologyException oe) {
          oe.printStackTrace();
         } 
        break;

        
      case 1:
        // Receive the price list from all suppliers      
        mt = MessageTemplate.and(
            MessageTemplate.MatchPerformative(ACLMessage.INFORM),
            MessageTemplate.MatchConversationId("supplier-info"));
        msg = receive(mt);
        
        if(msg != null){
          try {
            ContentElement ce = null;
            ce = getContentManager().extractContent(msg);
            
            if (ce instanceof SendSuppInfo) {
              SendSuppInfo sendSuppInfo = (SendSuppInfo) ce;
              
              // Cannot send HashMaps by message. De-composed in two lists, re-compose here
              HashMap<ComputerComponent, Integer> priceList = new HashMap<>();
              ArrayList<ComputerComponent> compsKeys = sendSuppInfo.getComponentsForSaleKeys();
              ArrayList<Long> compsValues = sendSuppInfo.getComponentsForSaleVal();
              
              for(int i=0; i<compsKeys.size(); i++) {
                ComputerComponent comp = compsKeys.get(i); 
                int price = compsValues.get(i).intValue();
                
                priceList.put(comp, price);
              }
              
              // More info retrieved from message
              AID supplier = sendSuppInfo.getSupplier();
              int speed = sendSuppInfo.getSpeed();
              suppliers.get(supplier).setPriceList(priceList);
              suppliers.get(supplier).setDeliveryDays(speed);
              
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
  // 1 - Get price lists from all suppliers - done in previous function
  // 2 - Listen for query-ifs from customers. Decide if accepting or not
  // 3 - Send reply back to customer
  // 4 - If the customer order is accepted, order the needed components from supplier 
  
  // This accepts/decline an order offer. Cycles until all cust responses are received
  private class OrderReplyBehaviour extends Behaviour{
    private static final long serialVersionUID = 1L;
    private OrderWrapper orderWpr;
    private int repliesSent = 0;

    public OrderReplyBehaviour(Agent a) {
      super(a);
    }
    
    @Override
    public void action() {
      // Receive order and calculate profit
      MessageTemplate mt = MessageTemplate.and(
          MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF),
          MessageTemplate.MatchConversationId("customer-order"));
      ACLMessage msg = receive(mt);
      
      if(msg != null){
        try {
          ContentElement ce = null;
          ce = getContentManager().extractContent(msg);
          
          if (ce instanceof CanManufacture) {
            CanManufacture canManifacture = (CanManufacture) ce;
            Order order = canManifacture.getOrder();
            orderWpr = new OrderWrapper(order);
            orderWpr.setCustomer(msg.getSender());
            
            // Note: not needed. We will never have extra components in the warehouse
//              Boolean allCompsAvailable = true;
//              for (ComputerComponent comp : orderWpr.getOrder().getComputer().getComponentList()) {
//                // If there are not enough components in the warehouse, flag as false
//                if(!warehouse.containsKey(comp) || 
//                    (warehouse.containsKey(comp) && 
//                     warehouse.get(comp) < orderWpr.getOrder().getQuantity())) {
//                  allCompsAvailable = false;
//                  break;
//                }
//              }
            // Can add something like: if all comps are available, proceed to ship
              
            
            // Calc how much it would cost to fulfill the order for each supplier
            HashMap <AID, Double> supplierCosts = new HashMap<>();
            
            for (Entry<AID, SupplierHelper> supplier : suppliers.entrySet()) {
              double totCost = 0;
              
              for(ComputerComponent comp : orderWpr.getOrder().getComputer().getComponentList()) {
                if (comp != null) { // Some component, like the laptop screen, can be null
                  totCost += supplier.getValue().getPriceList().get(comp);
                }
              }
              
              totCost *= orderWpr.getOrder().getQuantity();
              supplierCosts.put(supplier.getKey(), totCost);
            }
            
            
            // Pick the supplier that will grant us the best profit
            AID bestSupplier = null;
            double maxProfit = 0, expectedProfit = 0;
            int lateDeliveryFee = 0, daysLate = 0;
            
            for (SupplierHelper supplier : suppliers.values()) {
              // Calc how many days after order due date the supplier will ship components 
              daysLate = supplier.getDeliveryDays() - orderWpr.getOrder().getDueInDays();
              if (daysLate > 0) {
                lateDeliveryFee = daysLate * 50;
              } else {
                lateDeliveryFee = 0;
              }
              expectedProfit = orderWpr.getOrder().getPrice() 
                  - supplierCosts.get(supplier.getAid())
                  - lateDeliveryFee;
              
              if (bestSupplier == null && expectedProfit > 0) {
                // If there is no best supplier, get the first that grants some profit
                bestSupplier = supplier.getAid();
                maxProfit = expectedProfit;
              } else if (expectedProfit > maxProfit) {
                bestSupplier = supplier.getAid();
                maxProfit = expectedProfit;
              }
            }
            
            // Send reply to buyer
            ACLMessage reply = msg.createReply();
            if (bestSupplier != null) {
              // We know that the profit is positive if this runs
              // Add to list of orders that we said yes to, but not yet confirmed
              orderWpr.setSupplierAssigned(bestSupplier);
              orderWpr.setTotalCost(supplierCosts.get(bestSupplier));
              orderWpr.setOrderState(OrderWrapper.State.APPROVED);
              orderWpr.setOrderedDate(day);
              orderWpr.getOrder().setOrderId(orderIds.incrementAndGet());
              orders.add(orderWpr); 
              
              // TODO: dev, remove
              int daysDue = orderWpr.getOrder().getDueInDays();
              System.out.println("Speed required in days is: " + daysDue);
              System.out.println("The best supplier for this order is: " + bestSupplier);
              
              // The order is profitable, accept
              reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
            } else {
              // The order is not profitable enough, decline
              reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
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
      } else {
        block();
      }
    }

    @Override
    public boolean done() {
      return repliesSent == customers.size();
    }
  }
  
  
  public class CollectOrderRequests extends Behaviour{
    private static final long serialVersionUID = 1L;
    private OrderWrapper orderWpr;
    private AID supplier;
    private int step = 0;
    
    // This behaviour accepts the requests for the orders approved in the previous query_if
    public CollectOrderRequests(Agent a) {
      super(a);
    }
    
    @Override
    public void action() {
      switch(step) {
      case 0:
        MessageTemplate mt = MessageTemplate.and(
            MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
            MessageTemplate.MatchConversationId("customer-order"));
        ACLMessage msg = receive(mt);
        
        if(msg != null){
          try {
            ContentElement ce = null;
            ce = getContentManager().extractContent(msg);
            
            if(ce instanceof Action) {
              Concept action = ((Action)ce).getAction();
              if (action instanceof MakeOrder) {
                MakeOrder makeOrder = (MakeOrder)action;
                             
                orderWpr = orders.stream()
                    .filter(o -> makeOrder.getOrder().equals(o.getOrder()))
                    .findFirst().orElse(null);
 
                if (orderWpr != null && orderWpr.getOrderState() == OrderWrapper.State.APPROVED) {
                  orderWpr.setOrderState(OrderWrapper.State.CONFIRMED);
                  System.out.println("\nAdded to confirmed orders. List of orders at "
                      + "the end of CollectOrderRequests is: " + orders);
                  step++;
                } else {
                  // If the order was not approved in the previous step, don't accept order request
                  System.out.println("\nThis order was not approved! Cannot process order!");
                  step = 0;
                  // TODO: if we reach here, the program stops. find out why
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
          block();
        }
        break;
       
        
      case 1:
        // Ask the supplier if they have all the components that we need for this order
        supplier = orderWpr.getSupplierAssigned();  
        
        ACLMessage queryMsg = new ACLMessage(ACLMessage.QUERY_IF);
        queryMsg.setLanguage(codec.getName());
        queryMsg.setOntology(ontology.getName()); 
        queryMsg.addReceiver(supplier);
        queryMsg.setConversationId("component-selling");
        
        OwnsComponents ownsComps = new OwnsComponents();
        ownsComps.setOwner(supplier);
        ownsComps.setQuantity(orderWpr.getOrder().getQuantity());
        
        try {
          ownsComps.setComponents(orderWpr.getOrder().getComputer().getComponentList());                            
          getContentManager().fillContent(queryMsg, ownsComps);
          send(queryMsg);
          step++;
         } catch (CodecException ce) {
          ce.printStackTrace();
         } catch (OntologyException oe) {
          oe.printStackTrace();
         } catch (Exception e) {
           e.printStackTrace();
         }
         break;
        
        
      case 2:
        // Send order message to supplier if they have the components in stock
        MessageTemplate cmt = MessageTemplate.and(
            MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL),
            MessageTemplate.MatchConversationId("component-selling"));
        
        ACLMessage confMsg = myAgent.receive(cmt);
        if(confMsg != null) {
          if(confMsg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
            // The supplier has the components in stock and confirmed
            System.out.println("\nThe supplier has the components in stock and confirmed! YAY! Now making request...");
            
            ACLMessage orderMsg = new ACLMessage(ACLMessage.REQUEST);
            orderMsg.setLanguage(codec.getName());
            orderMsg.setOntology(ontology.getName()); 
            orderMsg.setConversationId("component-selling");
            orderMsg.addReceiver(supplier);
            
            try {            
              BuyComponents buyComponents = new BuyComponents();
              buyComponents.setBuyer(myAgent.getAID());
              buyComponents.setComponents(orderWpr.getOrder().getComputer().getComponentList());
              buyComponents.setQuantity(orderWpr.getOrder().getQuantity());
              
              Action request = new Action();
              request.setAction(buyComponents);
              request.setActor(confMsg.getSender());
    
               getContentManager().fillContent(orderMsg, request);
               send(orderMsg);
               orderWpr.setExpectedCompsShipDate(day + 
                   suppliers.get(supplier).getDeliveryDays());               
               
               // calc last 7 days order costs
               if (day >= 83) {
                 lastSevenDayCosts += orderWpr.getTotalCost();
               }
                              
               System.out.println("Sending order request to supplier. msg is: " + orderMsg);
               step++;
            } catch (CodecException ce) {
             ce.printStackTrace();
            } catch (OntologyException oe) {
             oe.printStackTrace();
            } 
          } else {
            orderWpr.setOrderState(OrderWrapper.State.DISMISSED);
            step = 0;
          }
        } else {
          block();
        }
        break;
      case 3:
        // Send payment for purchased component
        ACLMessage payMsg = new ACLMessage(ACLMessage.INFORM);
        payMsg.setLanguage(codec.getName());
        payMsg.setOntology(ontology.getName()); 
        payMsg.setConversationId("payment");
        payMsg.addReceiver(supplier);
        
        SendPayment sendPayment = new SendPayment();
        sendPayment.setBuyer(myAgent.getAID());
        sendPayment.setMoney(orderWpr.getTotalCost());
        
        try {
          getContentManager().fillContent(payMsg, sendPayment);
          send(payMsg);
          // Subtract to profit what we paid for the components
          dailyProfit -= orderWpr.getTotalCost();
          step = 0;
          System.out.println("\nmanuf sending " + orderWpr.getTotalCost() + " to supp");
        } catch (CodecException ce) {
          ce.printStackTrace();
        } catch (OntologyException oe) {
          oe.printStackTrace();
        }   
      break;
      }
    }

    @Override
    public boolean done() {
      // TODO: dev only
      Boolean bool = orders.stream()
          .filter(o -> o.getOrderState() == OrderWrapper.State.APPROVED)
          .count() == 0;
      
      if (bool && step == 0) {
        System.out.println("CollectOrderRequests is done. done is true");  
      }
      
      // Loop until there are no orders that are yet to be confirmed
      return bool && step == 0;
    }
  }
  
  
  // Receive the components that we are awaiting from the supplier
  public class ReceiveComponents extends Behaviour { 
    private static final long serialVersionUID = 1L;
    private int orderCompsExpected = 0;
    private int orderCompsReceived = 0;
    
    public ReceiveComponents(Agent a) {
      super(a);
      orderCompsExpected = (int) orders.stream()
          .filter(o -> o.getExpectedCompsShipDate() == day)
          .count();
    }
     
    @Override
    public void action() { 
      MessageTemplate mt = MessageTemplate.and(
          MessageTemplate.MatchPerformative(ACLMessage.INFORM),
          MessageTemplate.MatchConversationId("component-selling"));
      
      for (OrderWrapper orderWpr : orders) {
        if (orderWpr.getExpectedCompsShipDate() != day) continue;
      
        ACLMessage msg = receive(mt);
        if(msg != null){
          try {
            ContentElement ce = null;
            ce = getContentManager().extractContent(msg);
            
            if (ce instanceof ShipComponents) {
              ShipComponents shipComponents = (ShipComponents) ce;
              ArrayList<ComputerComponent> compList = shipComponents.getComponents();
              int quantity = shipComponents.getQuantity();
              
              // The components received get added to the warehouse
              for (ComputerComponent comp : compList) {
                if (warehouse.get(comp) == null) {
                  warehouse.put(comp, quantity);
                } else {
                  warehouse.put(comp, warehouse.get(comp) + quantity);
                }
              }
              
              // Extract the received component and print it
              System.out.println("Received " + quantity + " components: " + compList);
              orderCompsReceived++;
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
    }
    
    @Override
    public boolean done() {
      return orderCompsExpected == orderCompsReceived;
    }
  }
  
  
  // TODO: receive customer payment it the behaviour below or another one after
  // Note: can experiment with the strategy of sorting the orders by dueDate and ship the ones
  // that have a closer duedate, regardless of if the components were for these orders or not
  public class ManufactureAndSend extends OneShotBehaviour {
    private static final long serialVersionUID = 1L;

    public ManufactureAndSend(Agent a) {
      super(a);
    }

    @Override
    public void action() {
//      sort orders by due date
//      orders.sort((OrderWrapper o1, OrderWrapper o2)->
//        o1.getOrderedDate() - o2.getOrderedDate()); 
      
      // For each order, if there are enough components, manufacture and send to customer
      for (OrderWrapper orderWpr : orders) {
        if (orderWpr.getOrderState() != OrderWrapper.State.CONFIRMED) continue;
        
        Boolean allCompsAvailable = true;
        for (ComputerComponent comp : orderWpr.getOrder().getComputer().getComponentList()) {
          // Dont mind linux. It doesnt need a licence
          if(!warehouse.containsKey(comp) || 
              (warehouse.containsKey(comp) && comp.getClass() != OsLinux.class &&
               warehouse.get(comp) < orderWpr.getOrder().getQuantity())) {
            allCompsAvailable = false;
            break;
          }
        }
        if (!allCompsAvailable) continue;
        
        
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.setLanguage(codec.getName());
        msg.setOntology(ontology.getName()); 
        msg.setConversationId("customer-order");
        msg.addReceiver(orderWpr.getCustomer());
        
        ShipOrder shipOrder = new ShipOrder();
        shipOrder.setSender(myAgent.getAID());
        shipOrder.setOrder(orderWpr.getOrder());
        
        try {
          getContentManager().fillContent(msg, shipOrder);
          send(msg);
          orderWpr.setOrderState(OrderWrapper.State.AWAITING_PAYMENT);
          
          // Remove used components from warehouse 
          for (ComputerComponent comp : orderWpr.getOrder().getComputer().getComponentList()) {
           warehouse.put(comp, warehouse.get(comp) - orderWpr.getOrder().getQuantity());
          }

          System.out.println("Sending order " + orderWpr + " to cust " + orderWpr.getCustomer());
         } catch (CodecException ce) {
          ce.printStackTrace();
         } catch (OntologyException oe) {
          oe.printStackTrace();
         } 
      }
    }
  }
  
  
  
  public class ReceivePayment extends Behaviour {
    private static final long serialVersionUID = 1L;
    private int numPaymentsLeft = 0;
    private OrderWrapper orderWrp;
    
    public ReceivePayment(Agent a) {
      super(a);
    }
    
    @Override
    public void action() {
      MessageTemplate mt = MessageTemplate.and(
          MessageTemplate.MatchPerformative(ACLMessage.INFORM),
          MessageTemplate.MatchConversationId("payment"));
      ACLMessage msg = receive(mt);
      
      numPaymentsLeft = (int) orders.stream()
          .filter(o -> o.getOrderState() == OrderWrapper.State.AWAITING_PAYMENT)
          .count(); 
      
      if(msg != null){
        try {
          ContentElement ce = null;
          ce = getContentManager().extractContent(msg);
          
          if (ce instanceof SendPayment) {
            SendPayment sendPayment = (SendPayment) ce;
            System.out.println("\nmanuf got " + sendPayment.getMoney() + " from cust " + sendPayment.getBuyer());

            orderWrp = orders.stream()
              .filter(o -> o.getOrder().getOrderId() == sendPayment.getOrderId())
              .findFirst().orElse(null);
            orderWrp.setOrderState(OrderWrapper.State.PAID);
            
            // Add to daily profit
            dailyProfit += sendPayment.getMoney();
          } else {
            System.out.println("Unknown predicate " + ce.getClass().getName());
          }
        } catch (CodecException ce) {
          ce.printStackTrace();
        } catch (OntologyException oe) {
          oe.printStackTrace();
        }
      } else if (numPaymentsLeft > 0){
        block();
      }
    }
    
    @Override
    public boolean done() {
      // Loop until there are no payments left to be received
      return numPaymentsLeft == 0;
    }
  }
  
  public class EndDay extends OneShotBehaviour {
    private static final long serialVersionUID = 1L;
    
    public EndDay(Agent a) {
      super(a);
    }

    @Override
    public void action() {
      for (OrderWrapper orderWpr : orders) {
        // Note: the money paid to the supplier are subtracted where these orders are made 
        
        // Calc and subtract penalty for late delivery
        if (orderWpr.getExactDayDue() < day) {
          dailyProfit -= 50;
        }
      }
      
      // Storage fee
      for (ComputerComponent comp : warehouse.keySet()) {
        // OSs dont take up space in the warehouse
        if (comp.getClass() == OsLinux.class || comp.getClass() == OsWindows.class) continue;
        double loss = warehouse.get(comp) * 5;
        dailyProfit -= loss;
      }
      
      // Once calculation is done, remove the orders flagged as paid (completed)
      orders.removeIf(o -> o.getOrderState() == OrderWrapper.State.PAID); 
      
      // Add to the total profit
      totalProfit += dailyProfit;
      
      System.out.println("\nToday's profit was: " + dailyProfit);
      System.out.println("Total profit is: " + totalProfit);
      System.out.println("lastSevenDayCosts: " + lastSevenDayCosts);
      
      if (day == 90) {
        System.out.println("");
      }
      
      // Inform the ticker agent and the manufacturer that we are done 
      ACLMessage doneMsg = new ACLMessage(ACLMessage.INFORM);
      doneMsg.setContent("done");
      doneMsg.addReceiver(tickerAgent);
      for(AID supplier : suppliers.keySet()) {
        doneMsg.addReceiver(supplier);
      }
      
      myAgent.send(doneMsg);
      day++;
      dailyProfit = 0;
    }
  }
}
