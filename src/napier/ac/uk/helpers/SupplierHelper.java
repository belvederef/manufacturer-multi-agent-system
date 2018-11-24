package napier.ac.uk.helpers;

import java.util.HashMap;

import napier.ac.uk_ontology.concepts.ComputerComponent;

public class SupplierHelper {
  private HashMap<ComputerComponent, Integer> priceList;
  private int deliveryDays;
  
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
