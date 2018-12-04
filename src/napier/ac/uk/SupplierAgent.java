package napier.ac.uk;

import java.util.ArrayList;
import java.util.HashMap;
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
import napier.ac.uk_ontology.predicates.SendsSuppInfo;
import napier.ac.uk_ontology.predicates.OwnsComponents;
import napier.ac.uk_ontology.predicates.SendsPayment;
import napier.ac.uk_ontology.predicates.ShipsComponents;

public class SupplierAgent extends Agent {
  private static final long serialVersionUID = 1L;
  
  private Codec codec = new SLCodec();
  private Ontology ontology = ShopOntology.getInstance();

  private AID tickerAgent;
  private int day = 1; // day count
  
  private ArrayList<AID> buyers = new ArrayList<>();
  private ArrayList <SuppOrderWrapper> orders = new ArrayList<>();
  
  // These are overriden by the specific supplier implementations
  protected HashMap<ComputerComponent, Integer> componentsForSale; // component, price
  protected int suppDeliveryDays; // number of days needed for delivery
  private double money;

  @Override
  @SuppressWarnings("unchecked")
  protected void setup() {
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
    
    
    // Add price list and the speed for this supplier
    Object[] args = getArguments();
    if (args != null && args.length > 1) {
    componentsForSale = (HashMap<ComputerComponent, Integer>) args[0];
    suppDeliveryDays = (int) args[1];
    }
    
    addBehaviour(new TickerWaiter(this));
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

          CyclicBehaviour rse = new ReplySuppInfo(myAgent);
          myAgent.addBehaviour(rse);
          cyclicBehaviours.add(rse);
          
          CyclicBehaviour os = new OffersServer(myAgent);
          myAgent.addBehaviour(os);
          cyclicBehaviours.add(os);

          CyclicBehaviour sb = new ReceiveRequests(myAgent);
          myAgent.addBehaviour(sb);
          cyclicBehaviours.add(sb);
          
          CyclicBehaviour rp = new ReceivePayment(myAgent);
          myAgent.addBehaviour(rp);
          cyclicBehaviours.add(rp);
          
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
            buyers.add(agentsType[i].getName());
          }
        } catch (FIPAException e) {
          e.printStackTrace();
        }
      }
    }
  }

  // Sends the supplier's catalogue to the manufacturer
  public class ReplySuppInfo extends CyclicBehaviour {
    private static final long serialVersionUID = 1L;

    public ReplySuppInfo(Agent a) {
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
          ce = getContentManager().extractContent(msg);
          
          if (ce instanceof Action) {
            Concept action = ((Action)ce).getAction();
            if (action instanceof AskSuppInfo) {    
              // Sends supp details to manufacturer in INFORM message
              ACLMessage reply = msg.createReply(); 
              reply.setPerformative(ACLMessage.INFORM);
              
              // Can't send Hashmaps by message. Split in two lists
              ArrayList<ComputerComponent> compsKeys = new ArrayList<>();
              ArrayList<Long> compsVals = new ArrayList<>();
              
              for (Map.Entry<ComputerComponent, Integer> entry : componentsForSale.entrySet()) {
                ComputerComponent key = entry.getKey();
                long value = entry.getValue().longValue();
                compsKeys.add(key);
                compsVals.add(value);
              }
              
              // Make message predicate
              SendsSuppInfo sendSuppInfo = new SendsSuppInfo();
              sendSuppInfo.setSupplier(myAgent.getAID());
              sendSuppInfo.setSpeed(suppDeliveryDays);
              sendSuppInfo.setComponentsForSaleKeys(compsKeys);
              sendSuppInfo.setComponentsForSaleVal(compsVals);
              
              // Fill content
              getContentManager().fillContent(reply, sendSuppInfo);
              send(reply);
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
  
  // Replies whether the supplier owns the number of components asked
  public class OffersServer extends CyclicBehaviour {
    private static final long serialVersionUID = 1L;

    public OffersServer(Agent a) {
      super(a);
    }

    @Override
    public void action() {
      MessageTemplate mt = MessageTemplate.and(
          MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF),
          MessageTemplate.MatchConversationId("component-selling-ask"));
      ACLMessage msg = myAgent.receive(mt);
      
      if (msg != null) {
        try {
          ContentElement ce = null;
          ce = getContentManager().extractContent(msg);
          
          if (ce instanceof OwnsComponents) {
            OwnsComponents ownsComponents = (OwnsComponents) ce;
            int quantity = (int) ownsComponents.getQuantity();
            ArrayList<ComputerComponent> components = 
                (ArrayList<ComputerComponent>) ownsComponents.getComponents();

            // Skip logic, we would need to check the stock but we have unlimited stock in this example project 
            
            ACLMessage reply = msg.createReply();
            reply.setPerformative(ACLMessage.CONFIRM);
            reply.setConversationId("component-selling-reply");
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
  public class ReceiveRequests extends CyclicBehaviour {
    private static final long serialVersionUID = 1L;

    public ReceiveRequests(Agent a) {
      super(a);
    }

    @Override
    public void action() {
      // This behaviour should only respond to REQUEST messages
      MessageTemplate mt = MessageTemplate.and(
          MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
          MessageTemplate.MatchConversationId("component-selling-req"));
      ACLMessage msg = receive(mt);
      
      if (msg != null) {
        try {
          ContentElement ce = null;
          ce = getContentManager().extractContent(msg);
          
          if (ce instanceof Action) {
            Concept action = ((Action) ce).getAction();
            if (action instanceof BuyComponents) {
              BuyComponents orderedComponents = (BuyComponents) action;
              ArrayList<ComputerComponent> compList = 
                  (ArrayList<ComputerComponent>) orderedComponents.getComponents();
              int quantity = (int) orderedComponents.getQuantity();
              
              SuppOrderWrapper order = new SuppOrderWrapper();
              order.setBuyer(msg.getSender());
              order.setDeliveryDay(day + suppDeliveryDays);
              order.setComponents(compList);
              order.setQuantity(quantity);
              order.setOrderId(orderedComponents.getOrderId());
              orders.add(order); // Add to total list of orders
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
  public class SendComponents extends OneShotBehaviour {
    private static final long serialVersionUID = 1L;
    
    public SendComponents(Agent a) {
      super(a);
    }
    
    @Override
    public void action() {       
      for (SuppOrderWrapper order : orders) {
        if (order.getDeliveryDay() != day) continue;
        
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.setLanguage(codec.getName());
        msg.setOntology(ontology.getName()); 
        msg.setConversationId("component-selling-send");
        msg.addReceiver(order.getBuyer());
        
        ShipsComponents shipComponents = new ShipsComponents();
        shipComponents.setSeller(myAgent.getAID());
        shipComponents.setComponents(order.getComponents());
        shipComponents.setQuantity(order.getQuantity());
        shipComponents.setOrderId(order.getOrderId());
        
        try {
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
  
  public class ReceivePayment extends CyclicBehaviour {
    private static final long serialVersionUID = 1L;
    
    public ReceivePayment(Agent a) {
      super(a);
    }
    
    @Override
    public void action() { 
      MessageTemplate mt = MessageTemplate.and(
          MessageTemplate.MatchPerformative(ACLMessage.INFORM),
          MessageTemplate.MatchConversationId("payment"));
      ACLMessage msg = receive(mt);
      
      if(msg != null){
        try {
          ContentElement ce = null;
          ce = getContentManager().extractContent(msg);
          
          if (ce instanceof SendsPayment) {
            SendsPayment sendPayment = (SendsPayment) ce;
            money += sendPayment.getMoney();
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
      
      if (msg != null) {
        buyersFinished++;

        if (buyersFinished == buyers.size()) {
  
          // Inform the ticker that we are done
          ACLMessage tick = new ACLMessage(ACLMessage.INFORM);
          tick.setContent("done");
          tick.addReceiver(tickerAgent);
          myAgent.send(tick);
          day++;
          
          // Remove cyclic behaviours
          for (Behaviour b : toRemove) {
            myAgent.removeBehaviour(b);
          }
          myAgent.removeBehaviour(this);
        }
      } else {
        block();
      }
    }
  }
}
