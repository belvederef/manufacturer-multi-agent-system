package napier.ac.uk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

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
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import napier.ac.uk.helpers.SuppOrderWrapper;
import napier.ac.uk_ontology.ShopOntology;
import napier.ac.uk_ontology.actions.AskSuppInfo;
import napier.ac.uk_ontology.actions.BuyComponents;
import napier.ac.uk_ontology.concepts.ComputerComponent;
import napier.ac.uk_ontology.predicates.SendSuppInfo;
import napier.ac.uk_ontology.predicates.OwnsComponents;
import napier.ac.uk_ontology.predicates.ShipComponents;
import napier.ac.uk_ontology.predicates.ShipOrder;

public abstract class SupplierAgent extends Agent {
  private static final long serialVersionUID = 1L;
  
  private Codec codec = new SLCodec();
  private Ontology ontology = ShopOntology.getInstance();

  private AID tickerAgent;
  private ArrayList<AID> buyers = new ArrayList<>();

  // List of the components order we said yes to
  private HashMap<AID, ComputerComponent> componentsApproved = new HashMap<>(); 
  // List of the components and the agents that they are for
  private HashMap<AID, ComputerComponent> componentsConfirmed = new HashMap<>(); 
  
  // These are overriden by the specific supplier implementations
  protected int suppDeliveryDays; // number of days for delivery
  protected HashMap<ComputerComponent, Integer> componentsForSale; // component, price
  private ArrayList <SuppOrderWrapper> orders = new ArrayList<>();
  private int day = 1;

  protected void setup() { }

  protected void register() {
    getContentManager().registerLanguage(codec);
    getContentManager().registerOntology(ontology);

    // add this agent to the yellow pages
    DFAgentDescription dfd = new DFAgentDescription();
    dfd.setName(getAID());
    ServiceDescription sd = new ServiceDescription();
    sd.setType("supplier");
    sd.setName(getLocalName() + "-supplier-agent");
    dfd.addServices(sd);
    try {
      DFService.register(this, dfd);
    } catch (FIPAException e) {
      e.printStackTrace();
    }
  }

  @Override
  protected void takeDown() {
    // Deregister from the yellow pages
    try {
      DFService.deregister(this);
    } catch (FIPAException e) {
      e.printStackTrace();
    }
  }

  public class TickerWaiter extends CyclicBehaviour {
    private static final long serialVersionUID = 1L;

    // behaviour that waits for a new day
    public TickerWaiter(Agent a) {
      super(a);
    }

    @Override
    public void action() {
      MessageTemplate mt = MessageTemplate.or(MessageTemplate.MatchContent("new day"),
          MessageTemplate.MatchContent("terminate"));
      ACLMessage msg = myAgent.receive(mt);
      if (msg != null) {
        if (tickerAgent == null) {
          tickerAgent = msg.getSender();
        }
        if (msg.getContent().equals("new day")) {
          myAgent.addBehaviour(new FindBuyers(myAgent));

          ArrayList<Behaviour> cyclicBehaviours = new ArrayList<>();

          CyclicBehaviour rse = new ReplySuppEnquiry(myAgent);
          myAgent.addBehaviour(rse);
          cyclicBehaviours.add(rse);
          
          CyclicBehaviour os = new OffersServer(myAgent);
          myAgent.addBehaviour(os);
          cyclicBehaviours.add(os);

          CyclicBehaviour sb = new ReceiveRequests(myAgent);
          myAgent.addBehaviour(sb);
          cyclicBehaviours.add(sb);

          myAgent.addBehaviour(new SendComponents(myAgent));
          myAgent.addBehaviour(new EndDayListener(myAgent, cyclicBehaviours));
        } else {
          // termination message to end simulation
          myAgent.doDelete();
        }
      } else {
        block();
      }
    }

    public class FindBuyers extends OneShotBehaviour {
      private static final long serialVersionUID = 1L;

      public FindBuyers(Agent a) {
        super(a);
      }

      @Override
      public void action() {
        DFAgentDescription buyerTemplate = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("manufacturer");
        buyerTemplate.addServices(sd);
        try {
          buyers.clear();
          DFAgentDescription[] agentsType = DFService.search(myAgent, buyerTemplate);
          for (int i = 0; i < agentsType.length; i++) {
            buyers.add(agentsType[i].getName()); // this is the AID
          }
        } catch (FIPAException e) {
          e.printStackTrace();
        }
      }
    }
  }

  // Sends the supplier's catalogue to the manufacturer
  public class ReplySuppEnquiry extends CyclicBehaviour {
    private static final long serialVersionUID = 1L;

    public ReplySuppEnquiry(Agent a) {
      super(a);
    }

    @Override
    public void action() {
      MessageTemplate mt = MessageTemplate.and(
          MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
          MessageTemplate.MatchConversationId("supplier-info"));
      ACLMessage msg = myAgent.receive(mt);
      
      if (msg != null) {
        try {
          ContentElement ce = null;
          System.out.println("\nmsg received in ReplySuppEnquiry is: " + msg.getContent()); // print out the message
                                                                                         // content in SL
          ce = getContentManager().extractContent(msg);
          if (ce instanceof Action) {
            Concept action = ((Action)ce).getAction();
            if (action instanceof AskSuppInfo) {
              
              // Prepare the INFORM message. Sends own details to manufacturer
              ACLMessage reply = msg.createReply(); 
              reply.setPerformative(ACLMessage.INFORM);
              
              // Can't send Hashmaps by message. Split in two lists
              ArrayList<ComputerComponent> compsKeys = new ArrayList<>();
              ArrayList<Long> compsVals = new ArrayList<>();
              
              
              // TODO: I could send a list of components that I need in the request message 
              // so that this seller agent only returns the prices for the components that I requested
              // run a if statement here. Probably I dont need to care about this for this sample program
              for (Map.Entry<ComputerComponent, Integer> entry : componentsForSale.entrySet()) {
                ComputerComponent key = entry.getKey();
                long value = entry.getValue().longValue();
                compsKeys.add(key);
                compsVals.add(value);
              }
              
              // Make message predicate
              SendSuppInfo sendSuppInfo = new SendSuppInfo();
              sendSuppInfo.setSupplier(myAgent.getAID());
              sendSuppInfo.setSpeed(suppDeliveryDays);
              sendSuppInfo.setComponentsForSaleKeys(compsKeys);
              sendSuppInfo.setComponentsForSaleVal(compsVals);
              
              // Fill content
              getContentManager().fillContent(reply, sendSuppInfo);
              send(reply);
             
              System.out.println("\nSending response to the manufacturer with price list.");
            }
          }
        }

        catch (CodecException ce) {
          ce.printStackTrace();
        } catch (OntologyException oe) {
          oe.printStackTrace();
        }

      } else {
        block();
      }
    }
  }
  
  // Replies that they own the number of components asked
  public class OffersServer extends CyclicBehaviour {
    private static final long serialVersionUID = 1L;

    public OffersServer(Agent a) {
      super(a);
    }

    @Override
    public void action() {
      // TODO: attach price with msg.setcontent()
      MessageTemplate mt = MessageTemplate.and(
          MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF),
          MessageTemplate.MatchConversationId("component-selling"));
      ACLMessage msg = myAgent.receive(mt);
      
      if (msg != null) {
        System.out.println("In supplier offerserver. msg: " + msg);
        try {
          ContentElement ce = null;

          // Print out the message content in SL
          System.out.println("Component message asked by manufacturer is: " + msg.getContent());

          // Let JADE convert from String to Java objects
          // Output will be a ContentElement
          ce = getContentManager().extractContent(msg);
          if (ce instanceof OwnsComponents) {
            OwnsComponents ownsComponents = (OwnsComponents) ce;
            int quantity = (int) ownsComponents.getQuantity();
            ArrayList<ComputerComponent> components = 
                (ArrayList<ComputerComponent>) ownsComponents.getComponents();

            // Extract the component print it
            System.out.println("The component asked to supplier is " + components.toString());

            // Skip logic, we would need to check the stock but we have 
            // unlimited stock in this example project 
            ACLMessage reply = msg.createReply();
            reply.setPerformative(ACLMessage.CONFIRM);
            reply.setConversationId("component-selling");

            System.out.println("\nSending response to the manufacturer. We own the component. reply: " + reply);
            myAgent.send(reply);
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

  // Receive requests for component orders
  private class ReceiveRequests extends CyclicBehaviour {
    private static final long serialVersionUID = 1L;

    public ReceiveRequests(Agent a) {
      super(a);
    }

    @Override
    public void action() {
      // This behaviour should only respond to REQUEST messages
      MessageTemplate mt = MessageTemplate.and(
          MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
          MessageTemplate.MatchConversationId("component-selling"));
      ACLMessage msg = receive(mt);
      
      if (msg != null) {
        try {
          ContentElement ce = null;
          System.out.println("\nmsg received in ReceiveRequests is: " + msg.getContent()); // print out the message
                                                                                         // content in SL
          ce = getContentManager().extractContent(msg);
          if (ce instanceof Action) {
            Concept action = ((Action) ce).getAction();
            if (action instanceof BuyComponents) {
              BuyComponents orderedComponents = (BuyComponents) action;
              ArrayList<ComputerComponent> compList = 
                  (ArrayList<ComputerComponent>) orderedComponents.getComponents();
              int quantity = (int) orderedComponents.getQuantity();
              
              // Note: No need to check stock. Example project with unlimited stock
              
              SuppOrderWrapper order = new SuppOrderWrapper();
              order.setBuyer(msg.getSender());
              order.setDeliveryDay(day + suppDeliveryDays);
              order.setComponents(compList);
              order.setQuantity(quantity);
              orders.add(order); // Add to list of orders
              
              System.out.println("The supplier speed is " + suppDeliveryDays + ", today is day " 
                  + day + ", the order will be sent on day " + (suppDeliveryDays + day) );
              
            } else {
            System.out.println("Unknown predicate " + ce.getClass().getName());
            }
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
  
  // Send components when the number of delivery days of the supplier have passed
  private class SendComponents extends OneShotBehaviour {
    private static final long serialVersionUID = 1L;
    
    public SendComponents(Agent a) {
      super(a);
    }
    
    @Override
    public void action() {
      for (SuppOrderWrapper order : orders) {
        if (order.getDeliveryDay() != day) continue;
        
        // Prepare the INFORM message.
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.setLanguage(codec.getName());
        msg.setOntology(ontology.getName()); 
        msg.setConversationId("component-selling");
        msg.addReceiver(order.getBuyer());
        
        ShipComponents shipComponents = new ShipComponents();
        shipComponents.setBuyer(order.getBuyer());
        shipComponents.setComponents(order.getComponents());
        shipComponents.setQuantity(order.getQuantity());
        
        try {
          // Fill content
          getContentManager().fillContent(msg, shipComponents);
          send(msg);
         } catch (CodecException ce) {
          ce.printStackTrace();
         } catch (OntologyException oe) {
          oe.printStackTrace();
         }   
      }
    }
  }

  public class EndDayListener extends CyclicBehaviour {
    private static final long serialVersionUID = 1L;
    
    private int buyersFinished = 0;
    private List<Behaviour> toRemove;

    public EndDayListener(Agent a, List<Behaviour> toRemove) {
      super(a);
      this.toRemove = toRemove;
    }

    @Override
    public void action() {
      MessageTemplate mt = MessageTemplate.MatchContent("done");
      ACLMessage msg = myAgent.receive(mt);
//      System.out.println("buyers.size(): " + buyers.size());
      if (msg != null) {
        buyersFinished++;
//        System.out.println("buyersFinished++: " + buyersFinished);
      } else {
        block();
      }
      
      if (buyersFinished == buyers.size()) {
        System.out.println("SUPPLIER " + myAgent.getName() + " IS DONE");
        // We are finished
        ACLMessage tick = new ACLMessage(ACLMessage.INFORM);
        tick.setContent("done");
        tick.addReceiver(tickerAgent);
        myAgent.send(tick);
        // remove behaviours
        for (Behaviour b : toRemove) {
          myAgent.removeBehaviour(b);
        }
        myAgent.removeBehaviour(this);
        day++;
      }
    }

  }
}
