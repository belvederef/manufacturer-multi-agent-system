package napier.ac.uk;

import java.util.HashMap;

import jade.core.AID;
import napier.ac.uk.SupplierAgent.TickerWaiter;
import napier.ac.uk_ontology.elements.computerComponents.ComputerComponent;
import napier.ac.uk_ontology.elements.computerComponents.CpuDesktop;
import napier.ac.uk_ontology.elements.computerComponents.CpuLaptop;

public class SupplierMedAgent extends SupplierAgent {
  private HashMap<ComputerComponent, Integer> componentsForSale; // component, price
  private String name; // The name can be 'slow', 'medium' or 'fast', also showing how long it takes to deliver
  private int deliveryDays; // number of days for delivery
  
  @Override
  protected void setup() {
    super.componentsForSale = new HashMap<ComputerComponent, Integer>(); // pass list of components and prices
    super.componentsForSale.put(new CpuLaptop(), 175);
    super.componentsForSale.put(new CpuDesktop(), 130);
//    componentsForSale.put("LaptopCPU", 175);
//    componentsForSale.put("DesktopCPU", 130);
//    componentsForSale.put("LaptopMotherboard", 115);
//    componentsForSale.put("DesktopMotherboard", 60);
//    componentsForSale.put("RAM8", 40);
//    componentsForSale.put("RAM16", 80);
//    componentsForSale.put("HDD1", 45);
//    componentsForSale.put("HDD2", 65);
//    componentsForSale.put("LaptopScreen", 80);
//    componentsForSale.put("WindowsOS", 75);
//    componentsForSale.put("LinuxOS", 0);
    
    deliveryDays = 3;
    name = "slow"; // pass name/speed
    System.out.println("Created medium supplier");
    
    register();
    addBehaviour(new TickerWaiter(this));
  }
}
