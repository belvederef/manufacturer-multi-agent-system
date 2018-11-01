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
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import napier.ac.uk_ontology.ShopOntology;
import napier.ac.uk_ontology.actions.BuyComponent;
import napier.ac.uk_ontology.concepts.ComputerComponent;
import napier.ac.uk_ontology.predicates.OwnsComponent;
import napier.ac.uk_ontology.predicates.ShipComponent;

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
  private int deliveryDays; // number of days for delivery
  protected HashMap<ComputerComponent, Integer> componentsForSale; // component, price

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

          CyclicBehaviour os = new OffersServer(myAgent);
          myAgent.addBehaviour(os);
          cyclicBehaviours.add(os);

          CyclicBehaviour sb = new SellBehaviour(myAgent);
          myAgent.addBehaviour(sb);
          cyclicBehaviours.add(sb);

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
          if (ce instanceof OwnsComponent) {
            OwnsComponent ownsComponent = (OwnsComponent) ce;
            ComputerComponent component = (ComputerComponent) ownsComponent.getComponent();

            // Extract the component print it
            System.out.println("The component asked to supplier is " + component.toString());

            // Accept all questions, we have unlimited stock
            ACLMessage reply = msg.createReply();
            reply.setPerformative(ACLMessage.CONFIRM);
            reply.setConversationId("component-selling");

            System.out.println("\nSending response to the manufacturer. We own the component. reply: " + reply);
            myAgent.send(reply);
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

  private class SellBehaviour extends CyclicBehaviour {
    private static final long serialVersionUID = 1L;

    public SellBehaviour(Agent a) {
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
          System.out.println("\nmsg received in SellBehaviour is: " + msg.getContent()); // print out the message
                                                                                         // content in SL
          ce = getContentManager().extractContent(msg);
          if (ce instanceof Action) {
            Concept action = ((Action) ce).getAction();
            if (action instanceof BuyComponent) {
              BuyComponent orderedComponent = (BuyComponent) action;
              ComputerComponent component = orderedComponent.getComponent();

              // We have unlimited components, no need to check the stock or remove from list
              // TODO: check that componentsForSale is different for every supplier
              System.out.println("componentsForSale: " + componentsForSale);
              if (componentsForSale.containsKey(component)) {
                if (sendComponent(component, orderedComponent.getBuyer())) {
                  System.out.println("\nSent component " + component + " to " + orderedComponent.getBuyer());
                } else {
                  System.out.println("\nCould not send component " + component + " to " + orderedComponent.getBuyer());
                }
              }
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
    
    
    private Boolean sendComponent(ComputerComponent component, AID buyer) {
      // Prepare the INFORM message.
      ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
      msg.setLanguage(codec.getName());
      msg.setOntology(ontology.getName()); 
      msg.setConversationId("component-selling");
      msg.addReceiver(buyer);
      
      ShipComponent shipComponent = new ShipComponent();
      shipComponent.setBuyer(buyer);
      shipComponent.setComponent(component);
      
      try {
        // Fill content
        getContentManager().fillContent(msg, shipComponent);
        send(msg);
        return true;
       }
       catch (CodecException ce) {
        ce.printStackTrace();
       }
       catch (OntologyException oe) {
        oe.printStackTrace();
       } 
      return false;
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
      }
    }

  }
}
