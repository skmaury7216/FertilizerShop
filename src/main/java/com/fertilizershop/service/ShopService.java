package com.fertilizershop.service;
import com.fertilizershop.model.*;
import com.fertilizershop.repo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
@Service
public class ShopService {
 private final ProductRepository products;
 private final CustomerRepository customers;
 private final SupplierRepository suppliers;
 private final SaleRepository sales;
 private final StockPurchaseRepository purchases;
 private final PaymentRepository payments;
 public ShopService(ProductRepository p,CustomerRepository c,SupplierRepository s,SaleRepository sa,StockPurchaseRepository pu,PaymentRepository pa){products=p;customers=c;suppliers=s;sales=sa;purchases=pu;payments=pa;}
 public BigDecimal money(BigDecimal n){if(n==null)throw new IllegalArgumentException("राशि भरें");return n.setScale(2,RoundingMode.HALF_UP);}
 private void nonnegative(BigDecimal n,String field){if(n.signum()<0)throw new IllegalArgumentException(field+" negative नहीं हो सकता");}
 private void positive(BigDecimal n,String field){if(n.signum()<=0)throw new IllegalArgumentException(field+" zero से अधिक होना चाहिए");}
 private void qty(Integer n){if(n==null||n<=0)throw new IllegalArgumentException("Quantity zero से अधिक होनी चाहिए");}
 @Transactional public Product saveProduct(Long id,String name,String brand,String category,String unit,BigDecimal buy,BigDecimal sell,Integer reorder,String desc){
   if(name==null||name.isBlank())throw new IllegalArgumentException("Product name required");
   Product p=id==null?new Product():products.findById(id).orElseThrow(()->new IllegalArgumentException("Product not found"));
   p.name=name.trim();p.brand=brand;p.category=category;p.unit=unit;p.purchasePrice=money(buy);p.sellingPrice=money(sell);
   nonnegative(p.purchasePrice,"Purchase price");nonnegative(p.sellingPrice,"Selling price");
   p.reorderLevel=reorder==null?10:reorder;if(p.reorderLevel<0)throw new IllegalArgumentException("Reorder level invalid");
   p.description=desc;return products.save(p);
 }
 @Transactional public Customer saveCustomer(String name,String phone,String village,String address){
   if(name==null||name.isBlank())throw new IllegalArgumentException("Customer name required");
   Customer c=new Customer();c.name=name.trim();c.phone=phone;c.village=village;c.address=address;return customers.save(c);
 }
 @Transactional public Supplier saveSupplier(String name,String phone,String address){
   if(name==null||name.isBlank())throw new IllegalArgumentException("Supplier name required");
   Supplier s=new Supplier();s.name=name.trim();s.phone=phone;s.address=address;return suppliers.save(s);
 }
 @Transactional public StockPurchase purchase(Long productId,Long supplierId,Integer quantity,BigDecimal unitCost,String reference){
   qty(quantity);Product p=products.findById(productId).orElseThrow(()->new IllegalArgumentException("Product not found"));
   Supplier supplier=supplierId==null?null:suppliers.findById(supplierId).orElseThrow(()->new IllegalArgumentException("Supplier not found"));
   BigDecimal cost=money(unitCost);nonnegative(cost,"Cost");
   p.stock=Math.addExact(p.stock,quantity);p.purchasePrice=cost;products.save(p);
   StockPurchase v=new StockPurchase();v.product=p;v.supplier=supplier;v.quantity=quantity;v.unitCost=cost;
   v.totalCost=cost.multiply(BigDecimal.valueOf(quantity));v.reference=reference;v.purchasedAt=LocalDateTime.now();return purchases.save(v);
 }
 @Transactional public Sale sell(Long productId,Long customerId,Integer quantity,BigDecimal unitPrice,BigDecimal advance,String method,String reference){
   qty(quantity);Product p=products.findById(productId).orElseThrow(()->new IllegalArgumentException("Product not found"));
   Customer c=customers.findById(customerId).orElseThrow(()->new IllegalArgumentException("Customer not found"));
   if(!p.active)throw new IllegalArgumentException("Product inactive");
   if(p.stock<quantity)throw new IllegalArgumentException("Stock insufficient: available "+p.stock);
   BigDecimal price=money(unitPrice), paid=money(advance);positive(price,"Selling price");nonnegative(paid,"Advance");
   BigDecimal total=price.multiply(BigDecimal.valueOf(quantity));
   if(paid.compareTo(total)>0)throw new IllegalArgumentException("Paid amount total से अधिक है");
   p.stock-=quantity;products.save(p);
   Sale sale=new Sale();sale.product=p;sale.customer=c;sale.quantity=quantity;sale.unitPrice=price;sale.total=total;sale.paid=paid;sale.createdAt=LocalDateTime.now();
   sales.save(sale);if(paid.signum()>0)savePayment(sale,paid,method,reference);return sale;
 }
 @Transactional public Payment pay(Long saleId,BigDecimal amount,String method,String reference){
   Sale s=sales.findById(saleId).orElseThrow(()->new IllegalArgumentException("Sale not found"));
   BigDecimal a=money(amount);positive(a,"Payment amount");
   if(a.compareTo(s.getDue())>0)throw new IllegalArgumentException("Payment बाकी राशि से अधिक है");
   s.paid=s.paid.add(a);sales.save(s);return savePayment(s,a,method,reference);
 }
 private Payment savePayment(Sale s,BigDecimal amount,String method,String reference){
   if(!("Cash".equals(method)||"UPI (verified offline)".equals(method)||"Bank Transfer (verified offline)".equals(method)))
     throw new IllegalArgumentException("Payment method invalid");
   Payment p=new Payment();p.sale=s;p.amount=amount;p.method=method;p.reference=reference;p.paidAt=LocalDateTime.now();return payments.save(p);
 }
 public BigDecimal totalSales(){return sales.findAll().stream().map(s->s.total).reduce(BigDecimal.ZERO,BigDecimal::add);}
 public BigDecimal totalPaid(){return sales.findAll().stream().map(s->s.paid).reduce(BigDecimal.ZERO,BigDecimal::add);}
 public BigDecimal totalDue(){return totalSales().subtract(totalPaid());}
 public BigDecimal inventoryValue(){return products.findAll().stream().map(p->p.purchasePrice.multiply(BigDecimal.valueOf(p.stock))).reduce(BigDecimal.ZERO,BigDecimal::add);}
}
