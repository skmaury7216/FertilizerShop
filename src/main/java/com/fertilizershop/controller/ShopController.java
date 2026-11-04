package com.fertilizershop.controller;
import com.fertilizershop.model.*;
import com.fertilizershop.repo.*;
import com.fertilizershop.service.ShopService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.util.*;
@Controller
public class ShopController {
 private final ShopService service;
 private final ProductRepository products;
 private final CustomerRepository customers;
 private final SupplierRepository suppliers;
 private final SaleRepository sales;
 private final StockPurchaseRepository purchases;
 private final PaymentRepository payments;
 public ShopController(ShopService sv,ProductRepository p,CustomerRepository c,SupplierRepository su,SaleRepository s,StockPurchaseRepository pu,PaymentRepository pa){service=sv;products=p;customers=c;suppliers=su;sales=s;purchases=pu;payments=pa;}
 private void common(Model model,String active,String title){model.addAttribute("active",active);model.addAttribute("pageTitle",title);model.addAttribute("shopName","मौर्या खाद भंडार");}
 @GetMapping("/") String root(){return "redirect:/dashboard";}
 @GetMapping("/login") String login(){return "login";}
 @GetMapping("/dashboard") String dashboard(Model m){common(m,"dashboard","Dashboard");m.addAttribute("products",products.findByActiveTrueOrderByNameAsc());m.addAttribute("sales",sales.findAllByOrderByCreatedAtDesc().stream().limit(6).toList());m.addAttribute("totalSales",service.totalSales());m.addAttribute("totalPaid",service.totalPaid());m.addAttribute("totalDue",service.totalDue());m.addAttribute("inventoryValue",service.inventoryValue());m.addAttribute("customersCount",customers.count());return "dashboard";}
 @GetMapping("/products") String productList(Model m){common(m,"products","खाद और उत्पाद");m.addAttribute("products",products.findByActiveTrueOrderByNameAsc());return "products";}
 @GetMapping("/products/new") String productNew(Model m){common(m,"products","नया उत्पाद");m.addAttribute("product",new Product());return "product-form";}
 @GetMapping("/products/{id}/edit") String productEdit(@PathVariable Long id,Model m){common(m,"products","उत्पाद बदलें");m.addAttribute("product",products.findById(id).orElseThrow(()->new IllegalArgumentException("Product not found")));return "product-form";}
 @PostMapping("/products") String productSave(@RequestParam(required=false) Long id,@RequestParam String name,@RequestParam(required=false) String brand,@RequestParam(required=false) String category,@RequestParam(defaultValue="Bag") String unit,@RequestParam BigDecimal purchasePrice,@RequestParam BigDecimal sellingPrice,@RequestParam(defaultValue="10") Integer reorderLevel,@RequestParam(required=false) String description,RedirectAttributes a){return go(a,"/products",()->service.saveProduct(id,name,brand,category,unit,purchasePrice,sellingPrice,reorderLevel,description));}
 @GetMapping("/customers") String customers(Model m){common(m,"customers","ग्राहक / किसान");m.addAttribute("customers",customers.findAllByOrderByNameAsc());return "customers";}
 @PostMapping("/customers") String customerSave(@RequestParam String name,@RequestParam(required=false) String phone,@RequestParam(required=false) String village,@RequestParam(required=false) String address,RedirectAttributes a){return go(a,"/customers",()->service.saveCustomer(name,phone,village,address));}
 @GetMapping("/customers/{id}") String customerDetail(@PathVariable Long id,Model m){common(m,"customers","ग्राहक विवरण");m.addAttribute("customer",customers.findById(id).orElseThrow(()->new IllegalArgumentException("Customer not found")));m.addAttribute("sales",sales.findByCustomerIdOrderByCreatedAtDesc(id));return "customer-detail";}
 @GetMapping("/suppliers") String suppliers(Model m){common(m,"suppliers","Supplier Management");m.addAttribute("suppliers",suppliers.findAllByOrderByNameAsc());return "suppliers";}
 @PostMapping("/suppliers") String supplierSave(@RequestParam String name,@RequestParam(required=false) String phone,@RequestParam(required=false) String address,RedirectAttributes a){return go(a,"/suppliers",()->service.saveSupplier(name,phone,address));}
 @GetMapping("/purchases") String purchases(Model m){common(m,"purchases","Stock Purchase");m.addAttribute("purchases",purchases.findAllByOrderByPurchasedAtDesc());m.addAttribute("products",products.findByActiveTrueOrderByNameAsc());m.addAttribute("suppliers",suppliers.findAllByOrderByNameAsc());return "purchases";}
 @PostMapping("/purchases") String addPurchase(@RequestParam Long productId,@RequestParam(required=false) Long supplierId,@RequestParam Integer quantity,@RequestParam BigDecimal unitCost,@RequestParam(required=false) String reference,RedirectAttributes a){return go(a,"/purchases",()->service.purchase(productId,supplierId,quantity,unitCost,reference));}
 @GetMapping("/sales") String sales(Model m){common(m,"sales","Sales & Bills");m.addAttribute("sales",sales.findAllByOrderByCreatedAtDesc());m.addAttribute("customers",customers.findAllByOrderByNameAsc());m.addAttribute("products",products.findByActiveTrueOrderByNameAsc());return "sales";}
 @PostMapping("/sales") String createSale(@RequestParam Long productId,@RequestParam Long customerId,@RequestParam Integer quantity,@RequestParam BigDecimal unitPrice,@RequestParam(defaultValue="0") BigDecimal advance,@RequestParam(defaultValue="Cash") String method,@RequestParam(required=false) String reference,RedirectAttributes a){try{Sale sale=service.sell(productId,customerId,quantity,unitPrice,advance,method,reference);a.addFlashAttribute("message","बिक्री और बिल सेव हो गया");return "redirect:/sales/"+sale.id;}catch(IllegalArgumentException e){a.addFlashAttribute("error",e.getMessage());return "redirect:/sales";}}
 @GetMapping("/sales/{id}") String invoice(@PathVariable Long id,Model m){common(m,"sales","बिल / Receipt");Sale sale=sales.findById(id).orElseThrow(()->new IllegalArgumentException("Bill not found"));m.addAttribute("sale",sale);m.addAttribute("payments",payments.findBySaleIdOrderByPaidAtDesc(id));return "invoice";}
 @PostMapping("/sales/{id}/payments") String pay(@PathVariable Long id,@RequestParam BigDecimal amount,@RequestParam(defaultValue="Cash") String method,@RequestParam(required=false) String reference,RedirectAttributes a){return go(a,"/sales/"+id,()->service.pay(id,amount,method,reference));}
 @GetMapping("/payments") String payments(Model m){common(m,"payments","उधारी और भुगतान");m.addAttribute("sales",sales.findAllByOrderByCreatedAtDesc());m.addAttribute("payments",payments.findAllByOrderByPaidAtDesc());m.addAttribute("totalDue",service.totalDue());return "payments";}
 @GetMapping("/reports") String reports(Model m){common(m,"reports","Business Reports");m.addAttribute("totalSales",service.totalSales());m.addAttribute("totalPaid",service.totalPaid());m.addAttribute("totalDue",service.totalDue());m.addAttribute("inventoryValue",service.inventoryValue());m.addAttribute("sales",sales.findAllByOrderByCreatedAtDesc());m.addAttribute("products",products.findByActiveTrueOrderByNameAsc());return "reports";}
 private String go(RedirectAttributes attrs,String path,Runnable action){try{action.run();attrs.addFlashAttribute("message","सफलतापूर्वक सेव हो गया");}catch(IllegalArgumentException | ArithmeticException e){attrs.addFlashAttribute("error",e.getMessage());}return "redirect:"+path;}
}
