package set10111.simulation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

public class SellerAgent extends Agent {
	private HashMap<String,Integer> booksForSale = new HashMap<>();
	private AID tickerAgent;
	private ArrayList<AID> buyers = new ArrayList<>();
	@Override
	protected void setup() {
		//add this agent to the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("seller");
		sd.setName(getLocalName() + "-seller-agent");
		dfd.addServices(sd);
		try{
			DFService.register(this, dfd);
		}
		catch(FIPAException e){
			e.printStackTrace();
		}
		
		addBehaviour(new TickerWaiter(this));
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
					myAgent.addBehaviour(new BookGenerator());
					myAgent.addBehaviour(new FindBuyers(myAgent));
					CyclicBehaviour os = new OffersServer(myAgent);
					myAgent.addBehaviour(os);
					ArrayList<Behaviour> cyclicBehaviours = new ArrayList<>();
					cyclicBehaviours.add(os);
					myAgent.addBehaviour(new EndDayListener(myAgent,cyclicBehaviours));
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
		
		public class BookGenerator extends OneShotBehaviour {

			@Override
			public void action() {
				booksForSale.clear();
				//select one book for sale per day
				int rand = (int)Math.round((1 + 2 * Math.random()));
				//price will be between 1 and 50 GBP
				int price = (int)Math.round((1 + 49 * Math.random()));
				switch(rand) {
					case 1 :
						booksForSale.put("Java for Dummies", price);
						break;
					case 2 :
						booksForSale.put("JADE: the Inside Story", price);
						break;
					case 3 :
						booksForSale.put("Multi-Agent Systems for Everybody", price);
						break;
				}
			}
			
		}
		
		public class FindBuyers extends OneShotBehaviour {

			public FindBuyers(Agent a) {
				super(a);
			}

			@Override
			public void action() {
				DFAgentDescription buyerTemplate = new DFAgentDescription();
				ServiceDescription sd = new ServiceDescription();
				sd.setType("buyer");
				buyerTemplate.addServices(sd);
				try{
					buyers.clear();
					DFAgentDescription[] agentsType1  = DFService.search(myAgent,buyerTemplate); 
					for(int i=0; i<agentsType1.length; i++){
						buyers.add(agentsType1[i].getName()); // this is the AID
					}
				}
				catch(FIPAException e) {
					e.printStackTrace();
				}

			}

		}

	}
	
	public class OffersServer extends CyclicBehaviour {
		
		public OffersServer(Agent a) {
			super(a);
		}

		@Override
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage msg = myAgent.receive(mt);
			if(msg != null) {
				ACLMessage reply = msg.createReply();
				String book = msg.getContent();
				if(booksForSale.containsKey(book)) {
					//we can send an offer
					reply.setPerformative(ACLMessage.PROPOSE);
					reply.setContent(String.valueOf(booksForSale.get(book)));
				}
				else {
					reply.setPerformative(ACLMessage.REFUSE);
				}
				myAgent.send(reply);
			}
			else {
				block();
			}
			
		}
		
	}
	
	public class EndDayListener extends CyclicBehaviour {
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
			if(msg != null) {
				buyersFinished++;
			}
			else {
				block();
			}
			if(buyersFinished == buyers.size()) {
				//we are finished
				ACLMessage tick = new ACLMessage(ACLMessage.INFORM);
				tick.setContent("done");
				tick.addReceiver(tickerAgent);
				myAgent.send(tick);
				//remove behaviours
				for(Behaviour b : toRemove) {
					myAgent.removeBehaviour(b);
				}
				myAgent.removeBehaviour(this);
			}
		}
		
	}
}
