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

public class SupplierFastAgent extends SupplierAgent {
  private static final long serialVersionUID = 1L;
 
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
    
    super.suppDeliveryDays = 1;
    System.out.println("Created fast supplier");
    
    register();
    addBehaviour(new TickerWaiter(this));
  }
}
