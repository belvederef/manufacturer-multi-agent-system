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
import napier.ac.uk_ontology.elements.Computer;
import napier.ac.uk_ontology.elements.Desktop;
import napier.ac.uk_ontology.elements.Laptop;
import napier.ac.uk_ontology.elements.Order;
import napier.ac.uk_ontology.elements.actions.MakeOrder;
import napier.ac.uk_ontology.elements.computerComponents.HardDrive;
import napier.ac.uk_ontology.elements.computerComponents.Os;
import napier.ac.uk_ontology.elements.computerComponents.OsLinux;
import napier.ac.uk_ontology.elements.computerComponents.OsWindows;
import napier.ac.uk_ontology.elements.computerComponents.Ram;
import napier.ac.uk_ontology.elements.predicates.CanManufacture;


// TODO: Add logic to reset if this agent cannot find a manufacturer
public class CustomerAgent extends Agent {
  private Codec codec = new SLCodec();
  private Ontology ontology = ShopOntology.getInstance();
  
  private AID manufacturer;
  private ArrayList<Order> currentOrders = new ArrayList<>(); // The orders that were accepted so far
  private Order order; // The order for today
  private AID tickerAgent;
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
//          dailyActivity.addSubBehaviour(new ReceiveOrder(myAgent));
          dailyActivity.addSubBehaviour(new EndDay(myAgent));
          
          myAgent.addBehaviour(dailyActivity);
        }
        else {
          // Termination message to end simulation
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
    	ram = new Ram();
    	ram.setCapacity("8GB");
      } else {
    	ram = new Ram();
    	ram.setCapacity("16GB");
      }
      if(rand.nextFloat() < 0.5) {
    	hardDrive = new HardDrive();
    	hardDrive.setCapacity("1TB");
      } else {
        hardDrive = new HardDrive();
        hardDrive.setCapacity("2TB");
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
        } else {
          // if no manufacturer is found, keep searching
        }
      }
      catch(FIPAException e) {
        e.printStackTrace();
      }
    }
  }

  public class AskIfCanManufacture extends OneShotBehaviour {

    public AskIfCanManufacture(Agent a) {
      super(a);
    }

    @Override
    public void action() {
      // Prepare the Query-IF message. Asks the manufacturer to if they will accept the order
      ACLMessage msg = new ACLMessage(ACLMessage.QUERY_IF);
      msg.setLanguage(codec.getName());
      msg.setOntology(ontology.getName()); 
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
    // Make order if the manufacturer said they can accept the order
    // Otherwise proceed to end the day
    private Boolean replyReceived = false;
    
    public MakeOrderAction(Agent a) {
      super(a);
    }

    @Override
    public void action() {
      MessageTemplate mt = MessageTemplate.MatchSender(manufacturer);
      ACLMessage msg = myAgent.receive(mt);
      System.out.println(msg);
      if(msg != null) {
        replyReceived = true;
        if(msg.getPerformative() == ACLMessage.CONFIRM) {
          // The order was accepted
          System.out.println("\nThe order was accepted! YAY! Now making request...");
          
          // Prepare the action request message
          ACLMessage orderMsg = new ACLMessage(ACLMessage.REQUEST);
          orderMsg.setLanguage(codec.getName());
          orderMsg.setOntology(ontology.getName()); 
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
           currentOrders.add(order); // Add this order to the list of orders we are awaiting to receive
          }
          catch (CodecException ce) {
           ce.printStackTrace();
          }
          catch (OntologyException oe) {
           oe.printStackTrace();
          } 
        }
      } else {
        block();
      }
    }

    @Override
    public boolean done() {
      return replyReceived;
    }

    @Override
    public int onEnd() {
      // Do something on end
      return 0;
    }

  }
  
  
  public class ReceiveOrder extends Behaviour {
    // this is faulty and makes the app crash.
    private int numRepliesReceived = 0;
    
    public ReceiveOrder(Agent a) {
      super(a);
    }

    
    @Override
    public void action() {
      // TODO: use an ontology action such us matchPerformative shipOrder instead of sender
      // I can try with .matchOntology(shipOrder)
      MessageTemplate mt = MessageTemplate.MatchSender(manufacturer);
      ACLMessage msg = myAgent.receive(mt);
      if(msg != null) {
        
        try {
          ContentElement ce = null;
          
          // Let JADE convert from String to Java objects
          // Output will be a ContentElement
          ce = getContentManager().extractContent(msg);
          if (ce instanceof Order) {
            Order order = (Order) ce;
            
            // Optional, for testing
            Computer comp = order.getComputer();
            System.out.println("The computer is " + comp.toString());
           
            // Check that the order received was one that we were waiting for, that is contained in currentOrders
            
            // If it is, remove the receive order from the list currentOrders
            
          }
          
          
          
        } catch (CodecException ce) {
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
      return true;
    }

    @Override
    public int onEnd() {
      // Do something on end
      return 0;
    }

  }
  
  
  
  
  
  public class EndDay extends OneShotBehaviour {
    
    public EndDay(Agent a) {
      super(a);
    }

    @Override
    public void action() {
      // Inform the ticker agent and the manufacturer that we are done 
      ACLMessage doneMsg = new ACLMessage(ACLMessage.INFORM);
      doneMsg.setContent("done");
      doneMsg.addReceiver(tickerAgent);
      doneMsg.addReceiver(manufacturer);
      
      myAgent.send(doneMsg);
    }
  }
}
