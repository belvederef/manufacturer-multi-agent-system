package napier.ac.uk;

import java.util.HashMap;

import napier.ac.uk_ontology.computerComponents.CpuDesktop;
import napier.ac.uk_ontology.computerComponents.CpuLaptop;
import napier.ac.uk_ontology.computerComponents.HardDrive;
import napier.ac.uk_ontology.computerComponents.MotherboardDesktop;
import napier.ac.uk_ontology.computerComponents.MotherboardLaptop;
import napier.ac.uk_ontology.computerComponents.OsLinux;
import napier.ac.uk_ontology.computerComponents.OsWindows;
import napier.ac.uk_ontology.computerComponents.Ram;
import napier.ac.uk_ontology.computerComponents.Screen;
import napier.ac.uk_ontology.concepts.ComputerComponent;

public class SupplierSlowAgent extends SupplierAgent {
  private static final long serialVersionUID = 1L;
  
//  private HashMap<String,Integer> componentsForSale = new HashMap<>(); // component, price
  private HashMap<ComputerComponent, Integer> componentsForSale; // component, price
  private String name; // The name can be 'slow', 'medium' or 'fast', also showing how long it takes to deliver
  private int deliveryDays; // number of days for delivery
  
  @Override
  protected void setup() {
    super.componentsForSale = new HashMap<ComputerComponent, Integer>(); // pass list of components and prices
    super.componentsForSale.put(new CpuLaptop(), 150);
    super.componentsForSale.put(new CpuDesktop(), 110);
    super.componentsForSale.put(new MotherboardDesktop(), 95);
    super.componentsForSale.put(new MotherboardLaptop(), 50);
    super.componentsForSale.put(new Ram("8GB"), 30);
    super.componentsForSale.put(new Ram("16GB"), 70);
    super.componentsForSale.put(new HardDrive("1TB"), 35);
    super.componentsForSale.put(new HardDrive("2TB"), 55);
    super.componentsForSale.put(new Screen(), 60);
    super.componentsForSale.put(new OsWindows(), 75);
    super.componentsForSale.put(new OsLinux(), 0);
    
    deliveryDays = 7;
    name = "slow"; // pass name/speed
    System.out.println("Created slow supplier");
    
    register();
    addBehaviour(new TickerWaiter(this));
  }
}
