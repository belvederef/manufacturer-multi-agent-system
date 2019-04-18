package napier.ac.uk;

import jade.core.*;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import napier.ac.uk.helpers.SuppPriceLists;

public class Main {

	public static void main(String[] args) {
		Profile myProfile = new ProfileImpl();
		Runtime myRuntime = Runtime.instance();

		try {
			ContainerController myContainer = myRuntime.createMainContainer(myProfile);
			AgentController rma = myContainer.createNewAgent("rma", "jade.tools.rma.rma", null);
			rma.start();

			// Create three identica customers
			int numCustomers = 3;
			AgentController customer;
			for (int i = 0; i < numCustomers; i++) {
				customer = myContainer.createNewAgent("customer" + i, CustomerAgent.class.getCanonicalName(), null);
				customer.start();
			}

			// Create the three suppliers
			AgentController supplierSlow = myContainer.createNewAgent("supplierSlow",
					SupplierAgent.class.getCanonicalName(), new Object[] { SuppPriceLists.getSlowSuppComps(), 7 });
			AgentController supplierMed = myContainer.createNewAgent("supplierMed",
					SupplierAgent.class.getCanonicalName(), new Object[] { SuppPriceLists.getMedSuppComps(), 3 });
			AgentController supplierFast = myContainer.createNewAgent("supplierFast",
					SupplierAgent.class.getCanonicalName(), new Object[] { SuppPriceLists.getFastSuppComps(), 1 });
			supplierSlow.start();
			supplierMed.start();
			supplierFast.start();

			AgentController manufactAgent = myContainer.createNewAgent("manufacturer",
					ManufactAgent.class.getCanonicalName(), null);
			manufactAgent.start();

			// Create the ticker agent that keeps track of the working days
			AgentController tickerAgent = myContainer.createNewAgent("ticker", Ticker.class.getCanonicalName(), null);
			tickerAgent.start();

		} catch (Exception e) {
			System.out.println("Exception starting agent: " + e.toString());
		}
	}
}
