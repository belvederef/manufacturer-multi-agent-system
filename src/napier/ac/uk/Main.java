package napier.ac.uk;
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
			
			// Create three identica customers
			int numCustomers = 3;
      AgentController customer;
      for(int i=0; i<numCustomers; i++) {
        customer = myContainer.createNewAgent("customer" + i, 
            CustomerAgent.class.getCanonicalName(), null);
        customer.start();
      }
      	      
      
			// Create the three suppliers
      // Note: argue that as the different suppliers have different names, components list
      // prices and delivery times they are essentially different suppliers, thus must inherit
      // only the commong methods from a parent supplier class (which are all apart from the setup)
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
			
			
      // Create the ticker agent that keeps track of the working days
			AgentController tickerAgent = myContainer.createNewAgent("ticker", 
			    Ticker.class.getCanonicalName(), null);
			tickerAgent.start();
			
		}
		catch(Exception e){
			System.out.println("Exception starting agent: " + e.toString());
		}
	}
}
