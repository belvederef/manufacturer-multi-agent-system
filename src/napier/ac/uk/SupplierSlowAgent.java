package napier.ac.uk;

import java.util.HashMap;

import jade.core.AID;

public class SupplierSlowAgent extends SupplierAgent {
  private HashMap<String,Integer> componentsForSale = new HashMap<>(); // component, price
  private String name; // The name can be 'slow', 'medium' or 'fast', also showing how long it takes to deliver
  private int deliveryDays; // number of days for delivery
  
  @Override
  protected void setup() {
    componentsForSale = new HashMap<String, Integer>(); // pass list of components and prices
    componentsForSale.put("LaptopCPU", 150);
    componentsForSale.put("DesktopCPU", 110);
    componentsForSale.put("LaptopMotherboard", 95);
    componentsForSale.put("DesktopMotherboard", 50);
    componentsForSale.put("RAM8", 30);
    componentsForSale.put("RAM16", 70);
    componentsForSale.put("HDD1", 35);
    componentsForSale.put("HDD2", 55);
    componentsForSale.put("LaptopScreen", 60);
    componentsForSale.put("WindowsOS", 75);
    componentsForSale.put("LinuxOS", 0);
    
    deliveryDays = 7;
    name = "slow"; // pass name/speed
    System.out.println("Created slow supplier");
    
    register();
    addBehaviour(new TickerWaiter(this));
  }
}
