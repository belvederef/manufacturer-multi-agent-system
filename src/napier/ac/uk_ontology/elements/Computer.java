package napier.ac.uk_ontology.elements;

import jade.content.Concept;
import jade.content.onto.annotations.Slot;

public class Computer implements Concept {
  private String ram;
  private String hardDrive; 
  private String os;

  @Slot (mandatory = true)
  public String getRam() {
    return ram;
  }
  public void setRam(String ram) {
    this.ram = ram;
  }
  @Slot (mandatory = true)
  public String getHardDrive() {
    return hardDrive;
  }
  public void setHardDrive(String hardDrive) {
    this.hardDrive = hardDrive;
  }
  @Slot (mandatory = true)
  public String getOs() {
    return os;
  }
  public void setOs(String os) {
    this.os = os;
  }
}
