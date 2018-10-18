package napier.ac.uk_ontology.elements;

import jade.content.onto.annotations.Slot;

public class Desktop extends Computer {
  private String deskCpu;
  private String deskMotherboard;
  
  @Slot(mandatory = true)
  public String getDeskCpu() {
    return deskCpu;
  }
  public void setDeskCpu(String deskCpu) {
    this.deskCpu = deskCpu;
  }
  @Slot(mandatory = true)
  public String getDeskMotherboard() {
    return deskMotherboard;
  }
  public void setDeskMotherboard(String deskMotherboard) {
    this.deskMotherboard = deskMotherboard;
  }

}
