package napier.ac.uk;

import java.util.ArrayList;
import java.util.Random;

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
import napier.ac.uk_ontology.actions.MakeOrder;
import napier.ac.uk_ontology.computerComponents.HardDrive;
import napier.ac.uk_ontology.computerComponents.Os;
import napier.ac.uk_ontology.computerComponents.OsLinux;
import napier.ac.uk_ontology.computerComponents.OsWindows;
import napier.ac.uk_ontology.computerComponents.Ram;
import napier.ac.uk_ontology.concepts.Computer;
import napier.ac.uk_ontology.concepts.Desktop;
import napier.ac.uk_ontology.concepts.Laptop;
import napier.ac.uk_ontology.concepts.Order;
import napier.ac.uk_ontology.predicates.CanManufacture;
import napier.ac.uk_ontology.predicates.SendPayment;
import napier.ac.uk_ontology.predicates.ShipOrder;


public class CustomerAgent extends Agent {
  private static final long serialVersionUID = 1L;
  
  private Codec codec = new SLCodec();
  private Ontology ontology = ShopOntology.getInstance();
  
  private AID manufacturer;
  private ArrayList<Order> orders = new ArrayList<>(); // The orders that were accepted
  private Order order; // The order for today
  private AID tickerAgent;
  private int day = 1;
  @Override
  protected void setup() {
    // Set up the ontology
    getContentManager().registerLanguage(codec);
    getContentManager().registerOntology(ontology);
    
    // Add this agent to the yellow pages
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
    // Deregister from the yellow pages
    try{
      DFService.deregister(this);
    }
    catch(FIPAException e){
      e.printStackTrace();
    }
  }

  public class TickerWaiter extends CyclicBehaviour {
    private static final long serialVersionUID = 1L;

    // Behaviour to wait for a new day
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
          SequentialBehaviour dailyActivity = new SequentialBehaviour();
          
          // Sub-behaviours will execute in the order they are added
          dailyActivity.addSubBehaviour(new CreateOrder(myAgent));
          dailyActivity.addSubBehaviour(new FindManufacturers(myAgent));
          dailyActivity.addSubBehaviour(new AskIfCanManufacture(myAgent));
          dailyActivity.addSubBehaviour(new MakeOrderAction(myAgent));          
          dailyActivity.addSubBehaviour(new EndDay(myAgent));
          
          // Made cyclic so we dont need to know the number of orders we are expecting for each day
          // as they could be delayed anyway
//          myAgent.addBehaviour();           
          myAgent.addBehaviour(dailyActivity);
          if (day == 1) {
            // Add cyclic behaviour only once and keep it till the end. This was made cyclic
            // as we dont know for sure when to expect the orders. They could be delayed
            myAgent.addBehaviour(new ReceiveOrder(myAgent));
          }
        } else {
          // Termination message to end simulation
          myAgent.doDelete();
        }
      } else {
        block();
      }
    }

  }
  
  public class CreateOrder extends OneShotBehaviour {
    private static final long serialVersionUID = 1L;

    public CreateOrder(Agent a) {
      super(a);
    }

    @Override
    public void action() {
      // Add a new order for each day. Prepare the content. 
      Random rand = new Random();
      
      // Declare variable parts
      Computer computer;
      Ram ram;
      HardDrive hardDrive;
      Os os;
      
      // Randomly generate components
      if(rand.nextFloat() < 0.5) {
    	computer = new Desktop();
      } else {
    	computer = new Laptop();
      }
      if(rand.nextFloat() < 0.5) {
    	ram = new Ram("8GB");
      } else {
    	ram = new Ram("16GB");
      }
      if(rand.nextFloat() < 0.5) {
    	hardDrive = new HardDrive("1TB");
      } else {
        hardDrive = new HardDrive("2TB");
      }
      if(rand.nextFloat() < 0.5) {
    	os = new OsWindows();
      } else {
    	os = new OsLinux();
      }
      
      // Add randomly generated components
      computer.setRam(ram);
      computer.setHardDrive(hardDrive);
      computer.setOs(os);
      
      
      order = new Order();
      order.setComputer(computer);
      int quantity = (int) Math.floor(1 + 50 * rand.nextFloat());
      order.setQuantity(quantity);
      order.setPrice(quantity * (int) Math.floor(600 + 200 * rand.nextFloat()));
      order.setDueInDays((int) Math.floor(1 + 10 * rand.nextFloat()));
      
      System.out.println("\n New customer agent order is: " + order);
    }
  }
  

  public class FindManufacturers extends OneShotBehaviour {
    private static final long serialVersionUID = 1L;

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
        DFAgentDescription[] agentsList  = DFService.search(myAgent, manufacturerTemplate);
        if (agentsList.length > 0) {
          manufacturer = agentsList[0].getName(); // Get only the first manufacturer found 
        }
      }
      catch(FIPAException e) {
        e.printStackTrace();
      }
    }
  }

  public class AskIfCanManufacture extends OneShotBehaviour {
    private static final long serialVersionUID = 1L;

    public AskIfCanManufacture(Agent a) {
      super(a);
    }

    @Override
    public void action() {
      // Prepare the Query-IF message. Asks the manufacturer to if they will accept the order
      ACLMessage msg = new ACLMessage(ACLMessage.QUERY_IF);
      msg.setLanguage(codec.getName());
      msg.setOntology(ontology.getName()); 
      msg.setConversationId("customer-order");
      msg.addReceiver(manufacturer);
      
      CanManufacture canManufacture = new CanManufacture();
      canManufacture.setManufacturer(manufacturer);
      canManufacture.setOrder(order);
      
      try {
        // Let JADE convert from Java objects to string
        getContentManager().fillContent(msg, canManufacture);
        send(msg);
       }
       catch (CodecException ce) {
        ce.printStackTrace();
       }
       catch (OntologyException oe) {
        oe.printStackTrace();
       } 
    }
  }

  
  public class MakeOrderAction extends Behaviour {
    private static final long serialVersionUID = 1L;
    
    // Make order if the manufacturer said they can accept the order
    // Otherwise proceed to end the day
    private Boolean replyReceived = false;
    
    public MakeOrderAction(Agent a) {
      super(a);
    }

    @Override
    public void action() {    
      // Match the conversation id and a confirm or disconfirm message
      MessageTemplate mt = MessageTemplate.and(
          MessageTemplate.MatchConversationId("customer-order"),
          MessageTemplate.or(
              MessageTemplate.MatchPerformative(ACLMessage.CONFIRM),
              MessageTemplate.MatchPerformative(ACLMessage.DISCONFIRM)));

      
      ACLMessage msg = myAgent.receive(mt);
      System.out.println("\nmsg received in MakeOrderAction is: " + msg);
      if(msg != null) {
        replyReceived = true;
        if(msg.getPerformative() == ACLMessage.CONFIRM) {
          // The order was accepted
          System.out.println("\nThe order was accepted! YAY! Now making request...");
          
          // Prepare the action request message
          ACLMessage orderMsg = new ACLMessage(ACLMessage.REQUEST);
          orderMsg.setLanguage(codec.getName());
          orderMsg.setOntology(ontology.getName()); 
          orderMsg.setConversationId("customer-order");
          orderMsg.addReceiver(manufacturer);
          
          // Prepare the content. 
          MakeOrder makeOrder = new MakeOrder();
          makeOrder.setBuyer(myAgent.getAID());
          makeOrder.setOrder(order);
          
          Action request = new Action();
          request.setAction(makeOrder);
          request.setActor(manufacturer);
          try {
           getContentManager().fillContent(orderMsg, request); //send the wrapper object
           send(orderMsg);
           orders.add(order); // Add this order to the list of orders we are awaiting to receive
          }
          catch (CodecException ce) {
           ce.printStackTrace();
          }
          catch (OntologyException oe) {
           oe.printStackTrace();
          } 
        } else if(msg.getPerformative() == ACLMessage.REFUSE) {
          // The order was not accepted
          System.out.println("\nThe order was not accepted! Costumer " + myAgent.getLocalName() + " is done.");
        }
      } else {
        block();
      }
    }

    @Override
    public boolean done() {
      System.out.println("MakeOrderAction is done. replyReceived is: " + replyReceived);
      return replyReceived;
    }
  }
  
  // Made cyclic so we dont need to know the number of orders we are expecting for each day
  // as they could be delayed anyway
  public class ReceiveOrder extends CyclicBehaviour {
    private static final long serialVersionUID = 1L;
    
    public ReceiveOrder(Agent a) {
      super(a);
    }
    
    @Override
    public void action() { 
      MessageTemplate mt = MessageTemplate.and(
          MessageTemplate.MatchPerformative(ACLMessage.INFORM),
          MessageTemplate.MatchConversationId("customer-order"));
      ACLMessage msg = receive(mt);
      
      if(msg != null){
        try {
          ContentElement ce = null;
          ce = getContentManager().extractContent(msg);
          
          if (ce instanceof ShipOrder) {
            ShipOrder shipOrder = (ShipOrder) ce;
            Order order = (Order) shipOrder.getOrder();
            orders.remove(order); // Received, remove
            
            // Send payment for the received order
            ACLMessage payMsg = new ACLMessage(ACLMessage.INFORM);
            payMsg.setLanguage(codec.getName());
            payMsg.setOntology(ontology.getName()); 
            payMsg.setConversationId("payment");
            payMsg.addReceiver(shipOrder.getSender());
            
            SendPayment sendPayment = new SendPayment();
            sendPayment.setBuyer(myAgent.getAID());
            sendPayment.setMoney(shipOrder.getOrder().getPrice());
            sendPayment.setOrderId(order.getOrderId());
            
            getContentManager().fillContent(payMsg, sendPayment);
            send(payMsg);
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
  

  public class EndDay extends OneShotBehaviour {
    private static final long serialVersionUID = 1L;
    
    public EndDay(Agent a) {
      super(a);
    }

    @Override
    public void action() {
      // Inform the ticker agent that we are done 
      ACLMessage doneMsg = new ACLMessage(ACLMessage.INFORM);
      doneMsg.setContent("done");
      doneMsg.addReceiver(tickerAgent);
      
      myAgent.send(doneMsg);
      day++;
    }
  }
}
