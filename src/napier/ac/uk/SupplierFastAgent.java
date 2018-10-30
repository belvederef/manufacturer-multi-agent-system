package napier.ac.uk;

import java.util.HashMap;

import napier.ac.uk_ontology.elements.computerComponents.ComputerComponent;
import napier.ac.uk_ontology.elements.computerComponents.CpuDesktop;
import napier.ac.uk_ontology.elements.computerComponents.CpuLaptop;
import napier.ac.uk_ontology.elements.computerComponents.HardDrive;
import napier.ac.uk_ontology.elements.computerComponents.MotherboardDesktop;
import napier.ac.uk_ontology.elements.computerComponents.MotherboardLaptop;
import napier.ac.uk_ontology.elements.computerComponents.OsLinux;
import napier.ac.uk_ontology.elements.computerComponents.OsWindows;
import napier.ac.uk_ontology.elements.computerComponents.Ram;
import napier.ac.uk_ontology.elements.computerComponents.Screen;

public class SupplierFastAgent extends SupplierAgent {
  private static final long serialVersionUID = 1L;
  
//  private HashMap<String, Integer> componentsForSale = new HashMap<>(); // component, price
  private HashMap<ComputerComponent, Integer> componentsForSale; // component, price
  private String name; // The name can be 'slow', 'medium' or 'fast', also showing how long it takes to deliver
  private int deliveryDays; // number of days for delivery

  public SupplierFastAgent() {
    
  }
  
  @Override
  protected void setup() {
    super.componentsForSale = new HashMap<ComputerComponent, Integer>(); // pass list of components and prices
    super.componentsForSale.put(new CpuLaptop(), 200);
    super.componentsForSale.put(new CpuDesktop(), 150);
    super.componentsForSale.put(new MotherboardDesktop(), 125);
    super.componentsForSale.put(new MotherboardLaptop(), 75);
    super.componentsForSale.put(new Ram("8GB"), 50);
    super.componentsForSale.put(new Ram("16GB"), 90);
    super.componentsForSale.put(new HardDrive("1TB"), 50);
    super.componentsForSale.put(new HardDrive("2TB"), 75);
    super.componentsForSale.put(new Screen(), 100);
    super.componentsForSale.put(new OsWindows(), 75);
    super.componentsForSale.put(new OsLinux(), 0);
    
    deliveryDays = 1;
    name = "slow"; // pass name/speed
    System.out.println("Created fast supplier");
    
    register();
    addBehaviour(new TickerWaiter(this));
  }
}
