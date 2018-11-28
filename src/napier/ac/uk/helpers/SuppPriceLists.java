package napier.ac.uk.helpers;

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

public class SuppPriceLists {

  @SuppressWarnings("serial")
  private static final HashMap<ComputerComponent, Integer> fastSuppComps = 
  new HashMap<ComputerComponent, Integer>() {{
    put(new CpuLaptop(), 200);
    put(new CpuDesktop(), 150);
    put(new MotherboardDesktop(), 125);
    put(new MotherboardLaptop(), 75);
    put(new Ram("8GB"), 50);
    put(new Ram("16GB"), 90);
    put(new HardDrive("1TB"), 50);
    put(new HardDrive("2TB"), 75);
    put(new Screen(), 100);
    put(new OsWindows(), 75);
    put(new OsLinux(), 0);
  }};
  
  @SuppressWarnings("serial")
  private static final HashMap<ComputerComponent, Integer> medSuppComps = 
  new HashMap<ComputerComponent, Integer>() {{
    put(new CpuLaptop(), 175);
    put(new CpuDesktop(), 130);
    put(new MotherboardDesktop(), 115);
    put(new MotherboardLaptop(), 60);
    put(new Ram("8GB"), 40);
    put(new Ram("16GB"), 80);
    put(new HardDrive("1TB"), 45);
    put(new HardDrive("2TB"), 65);
    put(new Screen(), 80);
    put(new OsWindows(), 75);
    put(new OsLinux(), 0);
  }};
  
  @SuppressWarnings("serial")
  private static final HashMap<ComputerComponent, Integer> slowSuppComps = 
  new HashMap<ComputerComponent, Integer>() {{
    put(new CpuLaptop(), 150);
    put(new CpuDesktop(), 110);
    put(new MotherboardDesktop(), 95);
    put(new MotherboardLaptop(), 50);
    put(new Ram("8GB"), 30);
    put(new Ram("16GB"), 70);
    put(new HardDrive("1TB"), 35);
    put(new HardDrive("2TB"), 55);
    put(new Screen(), 60);
    put(new OsWindows(), 75);
    put(new OsLinux(), 0);
  }};
  
  // Getters
  public static HashMap<ComputerComponent, Integer> getFastSuppComps() {
    return fastSuppComps;
  }
  public static HashMap<ComputerComponent, Integer> getMedSuppComps() {
    return medSuppComps;
  }
  public static HashMap<ComputerComponent, Integer> getSlowSuppComps() {
    return slowSuppComps;
  }
  
}