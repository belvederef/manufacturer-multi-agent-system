package napier.ac.uk;

import java.util.HashMap;

import jade.core.AID;
import napier.ac.uk.SupplierAgent.TickerWaiter;

public class SupplierFastAgent extends SupplierAgent {
  private HashMap<String,Integer> componentsForSale = new HashMap<>(); // component, price
  private String name; // The name can be 'slow', 'medium' or 'fast', also showing how long it takes to deliver
  private int deliveryDays; // number of days for delivery

  @Override
  protected void setup() {
    componentsForSale = new HashMap<String, Integer>(); // pass list of components and prices
    componentsForSale.put("LaptopCPU", 200);
    componentsForSale.put("DesktopCPU", 150);
    componentsForSale.put("LaptopMotherboard", 125);
    componentsForSale.put("DesktopMotherboard", 75);
    componentsForSale.put("RAM8", 50);
    componentsForSale.put("RAM16", 90);
    componentsForSale.put("HDD1", 50);
    componentsForSale.put("HDD2", 75);
    componentsForSale.put("LaptopScreen", 100);
    componentsForSale.put("WindowsOS", 75);
    componentsForSale.put("LinuxOS", 0);
    
    deliveryDays = 1;
    name = "slow"; // pass name/speed
    System.out.println("Created fast supplier");
    
    register();
    addBehaviour(new TickerWaiter(this));
  }
}
