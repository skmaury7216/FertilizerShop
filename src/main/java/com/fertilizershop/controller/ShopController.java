
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

    public ShopController(
            ShopService sv,
            ProductRepository p,
            CustomerRepository c,
            SupplierRepository su,
            SaleRepository s,
            StockPurchaseRepository pu,
            PaymentRepository pa) {

        service = sv;
        products = p;
        customers = c;
        suppliers = su;
        sales = s;
        purchases = pu;
        payments = pa;
    }

    private void common(Model model, String active, String title) {
        model.addAttribute("active", active);
        model.addAttribute("pageTitle", title);
        model.addAttribute("shopName", "Maurya Fertilizer Shop");
    }

    // HOME AND LOGIN

    @GetMapping("/")
    String root() {
        return "redirect:/dashboard";
    }

    @GetMapping("/login")
    String login() {
        return "login";
    }

    // DASHBOARD

    @GetMapping("/dashboard")
    String dashboard(Model m) {
        common(m, "dashboard", "Dashboard");

        m.addAttribute("products",
                products.findByActiveTrueOrderByNameAsc());

        m.addAttribute("sales",
                sales.findAllByOrderByCreatedAtDesc()
                        .stream().limit(6).toList());

        m.addAttribute("totalSales", service.totalSales());
        m.addAttribute("totalPaid", service.totalPaid());
        m.addAttribute("totalDue", service.totalDue());
        m.addAttribute("inventoryValue", service.inventoryValue());
        m.addAttribute("customersCount", customers.count());

        return "dashboard";
    }

    // PRODUCTS

    @GetMapping("/products")
    String productList(Model m) {
        common(m, "products", "Products & Fertilizers");
        m.addAttribute("products",
                products.findByActiveTrueOrderByNameAsc());
        return "products";
    }

    @GetMapping("/products/new")
    String productNew(Model m) {
        common(m, "products", "New Product");
        m.addAttribute("product", new Product());
        return "product-form";
    }

    @GetMapping("/products/{id}/edit")
    String productEdit(@PathVariable Long id, Model m) {
        common(m, "products", "Edit Product");

        m.addAttribute("product",
                products.findById(id)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Product not found")));

        return "product-form";
    }

    @PostMapping("/products")
    String productSave(
            @RequestParam(required = false) Long id,
            @RequestParam String name,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "Bag") String unit,
            @RequestParam(required = false) String barcode,
            @RequestParam BigDecimal purchasePrice,
            @RequestParam BigDecimal sellingPrice,
            @RequestParam BigDecimal bagWeightKg,
            @RequestParam(defaultValue = "10") Integer reorderLevel,
            @RequestParam(required = false) String description,
            RedirectAttributes a) {

        return go(a, "/products",
                () -> service.saveProduct(
                        id, name, brand, category, unit,
                        barcode, purchasePrice, sellingPrice,
                        bagWeightKg, reorderLevel, description));
    }

    // CUSTOMERS

    @GetMapping("/customers")
    String customers(Model m) {
        common(m, "customers", "Customers");
        m.addAttribute("customers",
                customers.findAllByOrderByNameAsc());
        return "customers";
    }

    // UPDATED: CUSTOMER WITH OPTIONAL AADHAAR LAST 4

    @PostMapping("/customers")
    String customerSave(
            @RequestParam String name,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String village,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String aadhaarLast4,
            RedirectAttributes a) {

        return go(
                a,
                "/customers",
                () -> service.saveCustomer(
                        name,
                        phone,
                        village,
                        address,
                        aadhaarLast4));
    }

    @GetMapping("/customers/{id}")
    String customerDetail(@PathVariable Long id, Model m) {
        common(m, "customers", "Customer Details");

        m.addAttribute("customer",
                customers.findById(id)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Customer not found")));

        m.addAttribute("sales",
                sales.findByCustomerIdOrderByCreatedAtDesc(id));

        return "customer-detail";
    }

    // SUPPLIERS

    @GetMapping("/suppliers")
    String suppliers(Model m) {
        common(m, "suppliers", "Supplier Management");
        m.addAttribute("suppliers",
                suppliers.findAllByOrderByNameAsc());
        return "suppliers";
    }

    @PostMapping("/suppliers")
    String supplierSave(
            @RequestParam String name,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String address,
            RedirectAttributes a) {

        return go(a, "/suppliers",
                () -> service.saveSupplier(name, phone, address));
    }

    // STOCK PURCHASE

    @GetMapping("/purchases")
    String purchases(Model m) {
        common(m, "purchases", "Stock Purchase");

        m.addAttribute("purchases",
                purchases.findAllByOrderByPurchasedAtDesc());

        m.addAttribute("products",
                products.findByActiveTrueOrderByNameAsc());

        m.addAttribute("suppliers",
                suppliers.findAllByOrderByNameAsc());

        return "purchases";
    }

    @PostMapping("/purchases")
    String addPurchase(
            @RequestParam Long productId,
            @RequestParam(required = false) Long supplierId,
            @RequestParam Integer quantity,
            @RequestParam BigDecimal unitCost,
            @RequestParam(required = false) String reference,
            RedirectAttributes a) {

        return go(a, "/purchases",
                () -> service.purchase(
                        productId, supplierId, quantity,
                        unitCost, reference));
    }

    // SALES AND BILLING PAGE

    @GetMapping("/sales")
    String sales(Model m) {
        common(m, "sales", "Sales & Bills");

        m.addAttribute("invoiceRows",
                service.invoiceSummaries());

        m.addAttribute("products",
                products.findByActiveTrueOrderByNameAsc());

        return "sales";
    }

    // EXISTING MULTI-PRODUCT BILL

    @PostMapping("/sales")
    String createSale(
            @RequestParam String customerName,
            @RequestParam(required = false) String customerPhone,
            @RequestParam(required = false) String customerVillage,
            @RequestParam("productIds") List<Long> productIds,
            @RequestParam("quantities") List<Integer> quantities,
            @RequestParam("unitPrices") List<BigDecimal> unitPrices,
            @RequestParam(defaultValue = "0") BigDecimal advance,
            @RequestParam(defaultValue = "Cash") String method,
            @RequestParam(required = false) String reference,
            RedirectAttributes a) {

        try {
            Sale sale = service.sellMultiToNewCustomer(
                    customerName,
                    customerPhone,
                    customerVillage,
                    productIds,
                    quantities,
                    unitPrices,
                    advance,
                    method,
                    reference);

            a.addFlashAttribute("message",
                    "Invoice created successfully");

            return "redirect:/sales/" + sale.id;

        } catch (IllegalArgumentException | ArithmeticException e) {
            a.addFlashAttribute("error", e.getMessage());
            return "redirect:/sales";
        }
    }

    // GRAM / KG / BAG BILLING

    @PostMapping("/sales/weight")
    String createWeightSale(
            @RequestParam String customerName,
            @RequestParam(required = false) String customerPhone,
            @RequestParam(required = false) String customerVillage,
            @RequestParam Long productId,
            @RequestParam BigDecimal quantity,
            @RequestParam String unit,
            @RequestParam(defaultValue = "0") BigDecimal advance,
            @RequestParam(defaultValue = "Cash") String method,
            @RequestParam(required = false) String reference,
            RedirectAttributes a) {

        try {
            if (quantity == null || quantity.signum() <= 0) {
                throw new IllegalArgumentException(
                        "Enter a valid fertilizer quantity");
            }

            if (!"g".equals(unit)
                    && !"kg".equals(unit)
                    && !"bag".equals(unit)) {
                throw new IllegalArgumentException(
                        "Select Gram, Kilogram or Bag");
            }

            Sale sale = service.sellByWeight(
                    customerName,
                    customerPhone,
                    customerVillage,
                    productId,
                    quantity,
                    unit,
                    advance,
                    method,
                    reference);

            a.addFlashAttribute("message",
                    "Weight-based invoice created successfully");

            return "redirect:/sales/" + sale.id;

        } catch (IllegalArgumentException | ArithmeticException e) {
            a.addFlashAttribute("error", e.getMessage());
            return "redirect:/sales";
        }
    }

    // INVOICE DETAILS

    @GetMapping("/sales/{id}")
    String invoice(@PathVariable Long id, Model m) {
        common(m, "sales", "Invoice / Receipt");

        Sale sale = sales.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Bill not found"));

        List<Sale> items = service.invoiceItems(sale);

        m.addAttribute("sale", items.get(0));
        m.addAttribute("invoiceItems", items);

        BigDecimal invoiceTotal = items.stream()
                .map(x -> x.total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal invoicePaid = items.stream()
                .map(x -> x.paid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        m.addAttribute("invoiceTotal", invoiceTotal);
        m.addAttribute("invoicePaid", invoicePaid);

        m.addAttribute("invoiceDue",
                invoiceTotal.subtract(invoicePaid));

        m.addAttribute("invoiceStatus",
                invoiceTotal.compareTo(invoicePaid) == 0
                        ? "Paid"
                        : invoicePaid.signum() == 0
                                ? "Unpaid" : "Partial");

        List<Payment> history = new ArrayList<>();

        for (Sale line : items) {
            history.addAll(
                    payments.findBySaleIdOrderByPaidAtDesc(line.id));
        }

        history.sort(
                Comparator.comparing(
                        (Payment x) -> x.paidAt).reversed());

        m.addAttribute("payments", history);

        return "invoice";
    }

    // INVOICE PAYMENTS

    @PostMapping("/sales/{id}/payments")
    String pay(
            @PathVariable Long id,
            @RequestParam BigDecimal amount,
            @RequestParam(defaultValue = "Cash") String method,
            @RequestParam(required = false) String reference,
            RedirectAttributes a) {

        return go(a, "/sales/" + id,
                () -> service.payInvoice(
                        id, amount, method, reference));
    }

    // PAYMENTS AND DUES

    @GetMapping("/payments")
    String payments(Model m) {
        common(m, "payments", "Payments & Dues");

        m.addAttribute("sales",
                sales.findAllByOrderByCreatedAtDesc());

        m.addAttribute("payments",
                payments.findAllByOrderByPaidAtDesc());

        m.addAttribute("totalDue", service.totalDue());

        return "payments";
    }

    // REPORTS

    @GetMapping("/reports")
    String reports(Model m) {
        common(m, "reports", "Business Reports");

        m.addAttribute("totalSales", service.totalSales());
        m.addAttribute("totalPaid", service.totalPaid());
        m.addAttribute("totalDue", service.totalDue());
        m.addAttribute("inventoryValue", service.inventoryValue());

        m.addAttribute("sales",
                sales.findAllByOrderByCreatedAtDesc());

        m.addAttribute("products",
                products.findByActiveTrueOrderByNameAsc());

        return "reports";
    }

    // COMMON SAVE AND ERROR HANDLING

    private String go(
            RedirectAttributes attrs,
            String path,
            Runnable action) {

        try {
            action.run();
            attrs.addFlashAttribute(
                    "message", "Saved successfully");

        } catch (IllegalArgumentException | ArithmeticException e) {
            attrs.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:" + path;
    }
}
