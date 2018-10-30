package napier.ac.uk;

import java.util.HashMap;

import jade.core.AID;
import napier.ac.uk.SupplierAgent.TickerWaiter;
import napier.ac.uk_ontology.elements.computerComponents.ComputerComponent;
import napier.ac.uk_ontology.elements.computerComponents.CpuDesktop;
import napier.ac.uk_ontology.elements.computerComponents.CpuLaptop;
import napier.ac.uk_ontology.elements.computerComponents.MotherboardDesktop;
import napier.ac.uk_ontology.elements.computerComponents.MotherboardLaptop;
import napier.ac.uk_ontology.elements.computerComponents.Ram;

public class SupplierMedAgent extends SupplierAgent {
  private HashMap<ComputerComponent, Integer> componentsForSale; // component, price
  private String name; // The name can be 'slow', 'medium' or 'fast', also showing how long it takes to deliver
  private int deliveryDays; // number of days for delivery
  
  @Override
  protected void setup() {
    super.componentsForSale = new HashMap<ComputerComponent, Integer>(); // pass list of components and prices
    super.componentsForSale.put(new CpuLaptop(), 175);
    super.componentsForSale.put(new CpuDesktop(), 130);

    super.componentsForSale.put(new MotherboardDesktop(), 115);
    super.componentsForSale.put(new MotherboardLaptop(), 60);
    super.componentsForSale.put(new Ram("8GB"), 40);
//    super.componentsForSale.put(new RAM16(), 80);
//    super.componentsForSale.put(new HDD1(), 45);
//    super.componentsForSale.put(new HDD2(), 65);
//    super.componentsForSale.put(new LaptopScreen(), 80);
//    super.componentsForSale.put(new WindowsOS(), 75);
//    super.componentsForSale.put(new LinuxOS(), 0);
    
    deliveryDays = 3;
    name = "slow"; // pass name/speed
    System.out.println("Created medium supplier");
    
    register();
    addBehaviour(new TickerWaiter(this));
  }
}
