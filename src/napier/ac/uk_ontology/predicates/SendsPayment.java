package napier.ac.uk_ontology.predicates;

import jade.content.Predicate;
import jade.content.onto.annotations.Slot;
import jade.core.AID;

public class SendsPayment implements Predicate {
  private static final long serialVersionUID = 1L;

  private AID buyer;
  private int orderId;
  private double money;

  @Slot(mandatory = true)
  public AID getBuyer() {
    return buyer;
  }
  public void setBuyer(AID buyer) {
    this.buyer = buyer;
  }
  public int getOrderId() {
    return orderId;
  }
  public void setOrderId(int orderId) {
    this.orderId = orderId;
  }
  @Slot(mandatory = true)
  public double getMoney() {
    return money;
  }
  public void setMoney(double money) {
    this.money = money;
  }
  
  
}
