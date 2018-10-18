package napier.ac.uk;
import java.util.HashMap;

import jade.core.*;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;


public class Main {
  
	public static void main(String[] args) {
		Profile myProfile = new ProfileImpl();
		Runtime myRuntime = Runtime.instance();
		try{
			ContainerController myContainer = myRuntime.createMainContainer(myProfile);	
			AgentController rma = myContainer.createNewAgent("rma", "jade.tools.rma.rma", null);
			rma.start();
			
//			AgentController simulationAgent = myContainer.createNewAgent("buyer1", BuyerAgent.class.getCanonicalName(), null);
//			simulationAgent.start();
			
			// Customers of the systems. Shops that make orders for a number of computers 
	    int numCustomers = 3;
      AgentController customer;
      for(int i=0; i<numCustomers; i++) {
        customer = myContainer.createNewAgent("customer" + i, 
            CustomerAgent.class.getCanonicalName(), null);
        customer.start();
      }
      	      
			// Create the three suppliers
      AgentController supplierSlow = myContainer.createNewAgent("supplierSlow", 
          SupplierSlowAgent.class.getCanonicalName(), null);
      AgentController supplierMed = myContainer.createNewAgent("supplierMed", 
          SupplierMedAgent.class.getCanonicalName(), null);
      AgentController supplierFast = myContainer.createNewAgent("supplierFast", 
          SupplierFastAgent.class.getCanonicalName(), null);
      supplierSlow.start();
      supplierMed.start();
      supplierFast.start();
      
      
      AgentController manufactAgent = myContainer.createNewAgent("manufacturer", 
          ManufactAgent.class.getCanonicalName(), null);
      manufactAgent.start();
			
			
      // Create the ticker agent that defines working days
			AgentController tickerAgent = myContainer.createNewAgent("ticker", 
			    Ticker.class.getCanonicalName(), null);
			tickerAgent.start();
			
		}
		catch(Exception e){
			System.out.println("Exception starting agent: " + e.toString());
		}


	}

}
