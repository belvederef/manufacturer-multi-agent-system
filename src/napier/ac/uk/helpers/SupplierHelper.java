package napier.ac.uk.helpers;

import java.util.HashMap;

import jade.core.AID;
import napier.ac.uk_ontology.concepts.ComputerComponent;

public class SupplierHelper {
  private AID aid;
  private HashMap<ComputerComponent, Integer> priceList;
  private int deliveryDays;
  
  public SupplierHelper(AID aid) {
    this.aid = aid;
  }
  
  public AID getAid() {
    return aid;
  }
  public void setAid(AID aid) {
    this.aid = aid;
  }
  public HashMap<ComputerComponent, Integer> getPriceList() {
    return priceList;
  }
  public void setPriceList(HashMap<ComputerComponent, Integer> priceList) {
    this.priceList = priceList;
  }
  public int getDeliveryDays() {
    return deliveryDays;
  }
  public void setDeliveryDays(int deliveryDays) {
    this.deliveryDays = deliveryDays;
  }
}
