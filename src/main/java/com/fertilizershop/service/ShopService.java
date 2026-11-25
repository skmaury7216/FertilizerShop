
package com.fertilizershop.service;

import com.fertilizershop.model.*;
import com.fertilizershop.repo.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ShopService {

    private final ProductRepository products;
    private final CustomerRepository customers;
    private final SupplierRepository suppliers;
    private final SaleRepository sales;
    private final StockPurchaseRepository purchases;
    private final PaymentRepository payments;

    public ShopService(
            ProductRepository p,
            CustomerRepository c,
            SupplierRepository s,
            SaleRepository sa,
            StockPurchaseRepository pu,
            PaymentRepository pa) {

        products = p;
        customers = c;
        suppliers = s;
        sales = sa;
        purchases = pu;
        payments = pa;
    }

    public BigDecimal money(BigDecimal n) {
        if (n == null) {
            throw new IllegalArgumentException("Enter an amount");
        }
        return n.setScale(2, RoundingMode.HALF_UP);
    }

    private void nonnegative(BigDecimal n, String field) {
        if (n == null || n.signum() < 0) {
            throw new IllegalArgumentException(
                    field + " cannot be negative");
        }
    }

    private void positive(BigDecimal n, String field) {
        if (n == null || n.signum() <= 0) {
            throw new IllegalArgumentException(
                    field + " must be greater than zero");
        }
    }

    private void qty(Integer n) {
        if (n == null || n <= 0) {
            throw new IllegalArgumentException(
                    "Quantity must be greater than zero");
        }
    }

    // =====================================
    // PRODUCT MANAGEMENT
    // =====================================

    @Transactional
public Product saveProduct(
        Long id,
        String name,
        String brand,
        String category,
        String unit,
        String barcode,
        BigDecimal buy,
        BigDecimal sell,
        BigDecimal bagWeightKg,
        Integer reorder,
        String desc) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Product name required");
        }

        Product p = id == null
                ? new Product()
                : products.findById(id).orElseThrow(
                    () -> new IllegalArgumentException(
                            "Product not found"));

        String cleanBarcode = barcode == null
                ? null : barcode.trim();

        if (cleanBarcode != null
                && cleanBarcode.isEmpty()) {
            cleanBarcode = null;
        }

        if (cleanBarcode != null) {
            if (cleanBarcode.length() > 80
                    || !cleanBarcode.matches(
                            "[A-Za-z0-9._-]+")) {
                throw new IllegalArgumentException(
                        "Invalid product barcode");
            }

            var duplicate = products.findByBarcode(
                    cleanBarcode);

            if (duplicate.isPresent()
                    && !duplicate.get().id.equals(p.id)) {
                throw new IllegalArgumentException(
                        "Barcode already assigned to another product");
            }
        }

        p.barcode = cleanBarcode;
        p.name = name.trim();
        p.brand = brand;
        p.category = category;
        p.unit = unit;

        p.purchasePrice = money(buy);
        p.sellingPrice = money(sell);
        if (bagWeightKg == null || bagWeightKg.signum() <= 0) {
    throw new IllegalArgumentException(
            "Bag weight must be greater than zero");
}

p.bagWeightKg = bagWeightKg.setScale(
        3, RoundingMode.UNNECESSARY);

        nonnegative(p.purchasePrice, "Purchase price");
        nonnegative(p.sellingPrice, "Selling price");

        p.reorderLevel = reorder == null ? 10 : reorder;

        if (p.reorderLevel < 0) {
            throw new IllegalArgumentException(
                    "Reorder level invalid");
        }

        p.description = desc;
        return products.save(p);
    }

    
    // =====================================
    // CUSTOMER MANAGEMENT
    // =====================================

    @Transactional
    public Customer saveCustomer(
            String name,
            String phone,
            String village,
            String address,
            String aadhaarLast4) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Customer name required");
        }

        String cleanName = name.trim();

        if (cleanName.length() > 120) {
            throw new IllegalArgumentException(
                    "Customer name is too long");
        }

        if (phone != null && phone.length() > 20) {
            throw new IllegalArgumentException(
                    "Mobile number is too long");
        }

        if (village != null && village.length() > 200) {
            throw new IllegalArgumentException(
                    "Village name is too long");
        }

        String last4 = aadhaarLast4 == null
                ? ""
                : aadhaarLast4.trim();

        // Aadhaar is optional.
        // Never store the complete Aadhaar number.
        if (!last4.isEmpty()
                && !last4.matches("[0-9]{4}")) {
            throw new IllegalArgumentException(
                    "Enter exactly 4 Aadhaar digits");
        }

        Customer c = new Customer();

        c.name = cleanName;
        c.phone = phone;
        c.village = village;
        c.address = address;

        // Save only the final four digits.
        c.aadhaarLast4 = last4.isEmpty()
                ? null
                : last4;

        return customers.save(c);
    }

    // Compatibility with existing sales and billing.
    @Transactional
    public Customer saveCustomer(
            String name,
            String phone,
            String village,
            String address) {

        return saveCustomer(
                name,
                phone,
                village,
                address,
                null);
    }

    // =====================================
    // SUPPLIER MANAGEMENT
    // =====================================

    @Transactional
    public Supplier saveSupplier(
            String name,
            String phone,
            String address) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Supplier name required");
        }

        Supplier s = new Supplier();

        s.name = name.trim();
        s.phone = phone;
        s.address = address;

        return suppliers.save(s);
    }

    // =====================================
    // STOCK PURCHASE
    // =====================================

    @Transactional
    public StockPurchase purchase(
            Long productId,
            Long supplierId,
            Integer quantity,
            BigDecimal unitCost,
            String reference) {

        qty(quantity);

        Product p = products.findById(productId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Product not found"));

        Supplier supplier = supplierId == null
                ? null
                : suppliers.findById(supplierId)
                    .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Supplier not found"));

        BigDecimal cost = money(unitCost);

        nonnegative(cost, "Cost");

        p.stock = Math.addExact(p.stock, quantity);
        p.purchasePrice = cost;

        products.save(p);

        StockPurchase v = new StockPurchase();

        v.product = p;
        v.supplier = supplier;
        v.quantity = quantity;
        v.unitCost = cost;

        v.totalCost = cost.multiply(
                BigDecimal.valueOf(quantity));

        v.reference = reference;
        v.purchasedAt = LocalDateTime.now();

        return purchases.save(v);
    }

    // =====================================
    // EXISTING SINGLE PRODUCT SALE
    // =====================================

    @Transactional
    public Sale sell(
            Long productId,
            Long customerId,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal advance,
            String method,
            String reference) {

        qty(quantity);

        Product p = products.findById(productId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Product not found"));

        Customer c = customers.findById(customerId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Customer not found"));

        if (!p.active) {
            throw new IllegalArgumentException(
                    "Product inactive");
        }

        if (p.stock < quantity) {
            throw new IllegalArgumentException(
                    "Stock insufficient: available " + p.stock);
        }

        BigDecimal price = money(unitPrice);
        BigDecimal paid = money(advance);

        positive(price, "Selling price");
        nonnegative(paid, "Advance");

        BigDecimal total = price.multiply(
                BigDecimal.valueOf(quantity));

        if (paid.compareTo(total) > 0) {
            throw new IllegalArgumentException(
                    "Paid amount exceeds the bill total");
        }

        p.stock -= quantity;
        products.save(p);

        Sale sale = new Sale();

        sale.product = p;
        sale.customer = c;
        sale.quantity = quantity;
        sale.unitPrice = price;
        sale.total = total;
        sale.paid = paid;
        sale.createdAt = LocalDateTime.now();

        sales.save(sale);

        if (paid.signum() > 0) {
            savePayment(sale, paid, method, reference);
        }

        return sale;
    }

    // =====================================
    // SALE TO NEW CUSTOMER
    // =====================================

    @Transactional
    public Sale sellToNewCustomer(
            String customerName,
            String customerPhone,
            String customerVillage,
            Long productId,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal advance,
            String method,
            String reference) {

        if (customerName == null
                || customerName.isBlank()) {
            throw new IllegalArgumentException(
                    "Enter the new customer name");
        }

        if (customerName.trim().length() > 120) {
            throw new IllegalArgumentException(
                    "Customer name is too long");
        }

        if (customerPhone != null
                && customerPhone.length() > 20) {
            throw new IllegalArgumentException(
                    "Mobile number is too long");
        }

        if (customerVillage != null
                && customerVillage.length() > 200) {
            throw new IllegalArgumentException(
                    "Village or address is too long");
        }

        qty(quantity);

        Product p = products.findById(productId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Product not found"));

        if (!p.active) {
            throw new IllegalArgumentException(
                    "Product inactive");
        }

        if (p.stock < quantity) {
            throw new IllegalArgumentException(
                    "Stock insufficient: available " + p.stock);
        }

        BigDecimal price = money(unitPrice);
        BigDecimal paid = money(advance);

        positive(price, "Selling price");
        nonnegative(paid, "Advance");

        if (paid.compareTo(
                price.multiply(
                        BigDecimal.valueOf(quantity))) > 0) {
            throw new IllegalArgumentException(
                    "Paid amount exceeds the bill total");
        }

        if (paid.signum() > 0) {
            validatePaymentMethod(method);
        }

        Customer c = saveCustomer(
                customerName,
                customerPhone,
                customerVillage,
                "");

        return sell(
                productId,
                c.id,
                quantity,
                price,
                paid,
                method,
                reference);
    }

    // =====================================
    // EXISTING MULTI-PRODUCT BILLING
    // =====================================

    @Transactional
    public Sale sellMultiToNewCustomer(
            String name,
            String phone,
            String village,
            List<Long> productIds,
            List<Integer> quantities,
            List<BigDecimal> unitPrices,
            BigDecimal advance,
            String method,
            String reference) {

        if (name == null
                || name.isBlank()
                || name.trim().length() > 120) {
            throw new IllegalArgumentException(
                    "Enter a valid customer name (up to 120 characters)");
        }

        if (phone != null && phone.length() > 20) {
            throw new IllegalArgumentException(
                    "Mobile number is too long");
        }

        if (village != null && village.length() > 200) {
            throw new IllegalArgumentException(
                    "Village or address is too long");
        }

        if (productIds == null
                || quantities == null
                || unitPrices == null
                || productIds.isEmpty()
                || productIds.size() != quantities.size()
                || productIds.size() != unitPrices.size()
                || productIds.size() > 50) {
            throw new IllegalArgumentException(
                    "Enter 1 to 50 complete product rows");
        }

        if (new HashSet<>(productIds).size()
                != productIds.size()) {
            throw new IllegalArgumentException(
                    "Each product can appear only once");
        }

        List<Product> selected = new ArrayList<>();
        List<BigDecimal> prices = new ArrayList<>();

        BigDecimal grandTotal = BigDecimal.ZERO;

        for (int i = 0; i < productIds.size(); i++) {

            qty(quantities.get(i));

            Product product = products
                    .findById(productIds.get(i))
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Product not found"));

            if (!product.active
                    || product.stock < quantities.get(i)) {
                throw new IllegalArgumentException(
                        "Insufficient stock for " + product.name);
            }

            BigDecimal price = money(unitPrices.get(i));

            positive(price, "Selling price");

            selected.add(product);
            prices.add(price);

            grandTotal = grandTotal.add(
                    price.multiply(
                            BigDecimal.valueOf(
                                    quantities.get(i))));
        }

        BigDecimal remainingPayment = money(advance);

        nonnegative(remainingPayment, "Paid amount");

        if (remainingPayment.compareTo(grandTotal) > 0) {
            throw new IllegalArgumentException(
                    "Paid amount exceeds the invoice total");
        }

        if (remainingPayment.signum() > 0) {
            validatePaymentMethod(method);
        }

        Customer customer = saveCustomer(
                name, phone, village, "");

        String groupId = UUID.randomUUID().toString();

        Sale first = null;
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < selected.size(); i++) {

            Product p = selected.get(i);
            int amount = quantities.get(i);

            BigDecimal lineTotal = prices.get(i)
                    .multiply(BigDecimal.valueOf(amount));

            BigDecimal linePaid =
                    remainingPayment.min(lineTotal);

            remainingPayment =
                    remainingPayment.subtract(linePaid);

            p.stock -= amount;
            products.save(p);

            Sale row = new Sale();

            row.product = p;
            row.customer = customer;
            row.quantity = amount;
            row.unitPrice = prices.get(i);
            row.total = lineTotal;
            row.paid = linePaid;
            row.createdAt = now;
            row.billGroup = groupId;

            row = sales.save(row);

            if (linePaid.signum() > 0) {
                savePayment(
                        row, linePaid, method, reference);
            }

            if (first == null) {
                first = row;
            }
        }

        return first;
    }

    // =====================================
    // NEW: SELL BY GRAM, KG OR BAG
    // =====================================

    @Transactional
    public Sale sellByWeight(
            String customerName,
            String customerPhone,
            String customerVillage,
            Long productId,
            BigDecimal quantity,
            String unit,
            BigDecimal advance,
            String method,
            String reference) {

        if (customerName == null
                || customerName.isBlank()
                || customerName.trim().length() > 120) {
            throw new IllegalArgumentException(
                    "Enter a valid customer name");
        }

        if (customerPhone != null
                && customerPhone.length() > 20) {
            throw new IllegalArgumentException(
                    "Mobile number is too long");
        }

        if (customerVillage != null
                && customerVillage.length() > 200) {
            throw new IllegalArgumentException(
                    "Village or address is too long");
        }

        positive(quantity, "Quantity");

        Product p = products.findById(productId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Product not found"));

        if (!p.active) {
            throw new IllegalArgumentException(
                    "Product inactive");
        }

        if (p.bagWeightKg == null
                || p.bagWeightKg.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Set bag weight for " + p.name);
        }

        // Convert entered quantity to kilograms.
        BigDecimal weightKg;

        if ("g".equals(unit)) {
            weightKg = quantity.movePointLeft(3);
        } else if ("kg".equals(unit)) {
            weightKg = quantity;
        } else if ("bag".equals(unit)) {
            if (quantity.stripTrailingZeros().scale() > 0) {
                throw new IllegalArgumentException(
                        "Bag quantity must be a whole number");
            }
            weightKg = quantity.multiply(p.bagWeightKg);
        } else {
            throw new IllegalArgumentException(
                    "Select Gram, Kilogram or Bag");
        }

        try {
            weightKg = weightKg.setScale(
                    3, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException(
                    "Minimum supported weight is 1 gram");
        }

        positive(weightKg, "Weight");

        // Keep only complete, unopened bags in stock.
        // Opened bags are represented in looseStockKg.
        BigDecimal loose = p.looseStockKg == null
                ? BigDecimal.ZERO
                : p.looseStockKg;

        if (p.stock == null || p.stock < 0
                || loose.signum() < 0
                || loose.compareTo(p.bagWeightKg) >= 0) {
            throw new IllegalArgumentException(
                    "Invalid fertilizer stock configuration");
        }

        BigDecimal availableKg =
                p.bagWeightKg.multiply(
                        BigDecimal.valueOf(p.stock))
                        .add(loose);

        if (weightKg.compareTo(availableKg) > 0) {
            throw new IllegalArgumentException(
                    "Insufficient stock. Available "
                            + availableKg.stripTrailingZeros()
                            .toPlainString() + " kg");
        }

        if (p.sellingPrice == null
                || p.sellingPrice.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Set a valid selling price for " + p.name);
        }

        // Calculate amount using one full bag price.
        BigDecimal total = p.sellingPrice
                .multiply(weightKg)
                .divide(
                        p.bagWeightKg,
                        2,
                        RoundingMode.HALF_UP);

        BigDecimal paid = money(advance);
        nonnegative(paid, "Paid amount");

        if (paid.compareTo(total) > 0) {
            throw new IllegalArgumentException(
                    "Paid amount exceeds bill total");
        }

        if (paid.signum() > 0) {
            validatePaymentMethod(method);
        }

        BigDecimal remainingLoose =
                loose.subtract(weightKg);

        if (remainingLoose.signum() >= 0) {
            p.looseStockKg = remainingLoose;
        } else {
            BigDecimal needed = remainingLoose.negate();

            int bagsToOpen = needed.divide(
                    p.bagWeightKg,
                    0,
                    RoundingMode.CEILING).intValueExact();

            if (bagsToOpen > p.stock) {
                throw new IllegalArgumentException(
                        "Insufficient bags");
            }

            p.stock -= bagsToOpen;

            p.looseStockKg = p.bagWeightKg
                    .multiply(BigDecimal.valueOf(bagsToOpen))
                    .subtract(needed);
        }

        Customer customer = saveCustomer(
                customerName,
                customerPhone,
                customerVillage,
                "");

        products.save(p);

        Sale sale = new Sale();

        sale.product = p;
        sale.customer = customer;

        // Existing reports require an integer quantity.
        // Actual weight is saved separately.
        sale.quantity = 1;

        sale.weightKg = weightKg;
        sale.weightUnit = unit;

        sale.pricePerKg = p.sellingPrice.divide(
                p.bagWeightKg,
                2,
                RoundingMode.HALF_UP);

        sale.unitPrice = total;
        sale.total = total;
        sale.paid = paid;
        sale.createdAt = LocalDateTime.now();

        sale = sales.save(sale);

        if (paid.signum() > 0) {
            savePayment(
                    sale, paid, method, reference);
        }

        return sale;
    }

    // =====================================
    // INVOICE MANAGEMENT
    // =====================================

    public List<Sale> invoiceItems(Sale anchor) {
        return anchor.billGroup == null
                ? List.of(anchor)
                : sales.findByBillGroupOrderByIdAsc(
                        anchor.billGroup);
    }

    @Transactional
    public void payInvoice(
            Long anchorId,
            BigDecimal amount,
            String method,
            String reference) {

        Sale anchor = sales.findById(anchorId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Bill not found"));

        List<Sale> lines = invoiceItems(anchor);

        BigDecimal value = money(amount);
        positive(value, "Payment amount");

        BigDecimal available = lines.stream()
                .map(Sale::getDue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (value.compareTo(available) > 0) {
            throw new IllegalArgumentException(
                    "Payment exceeds outstanding invoice balance");
        }

        validatePaymentMethod(method);

        for (Sale row : lines) {
            BigDecimal applied = value.min(row.getDue());

            if (applied.signum() > 0) {
                row.paid = row.paid.add(applied);
                sales.save(row);

                savePayment(
                        row, applied, method, reference);

                value = value.subtract(applied);
            }
        }
    }

    // =====================================
    // PAYMENT VALIDATION
    // =====================================

    private void validatePaymentMethod(String method) {
        if (!("Cash".equals(method)
                || "UPI (verified offline)".equals(method)
                || "Bank Transfer (verified offline)".equals(method))) {

            throw new IllegalArgumentException(
                    "Invalid payment method");
        }
    }

    // =====================================
    // INVOICE SUMMARY
    // =====================================

    public static class InvoiceSummary {

        private final Long id;
        private final LocalDateTime createdAt;
        private final String customerName;
        private final String products;
        private final String status;
        private final int quantity;
        private final BigDecimal total;
        private final BigDecimal paid;
        private final BigDecimal due;

        InvoiceSummary(List<Sale> lines) {

            Sale first = lines.get(0);

            this.id = first.id;
            this.createdAt = first.createdAt;
            this.customerName = first.customer.name;

            this.products = lines.size() == 1
                    ? first.product.name
                    : lines.size() + " products";

            this.quantity = lines.stream()
                    .mapToInt(x -> x.quantity)
                    .sum();

            this.total = lines.stream()
                    .map(x -> x.total)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            this.paid = lines.stream()
                    .map(x -> x.paid)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            this.due = total.subtract(paid);

            this.status = due.signum() == 0
                    ? "Paid"
                    : (paid.signum() == 0
                            ? "Unpaid" : "Partial");
        }

        public Long getId() { return id; }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public String getCustomerName() {
            return customerName;
        }

        public String getProducts() {
            return products;
        }

        public String getStatus() {
            return status;
        }

        public int getQuantity() {
            return quantity;
        }

        public BigDecimal getTotal() {
            return total;
        }

        public BigDecimal getPaid() {
            return paid;
        }

        public BigDecimal getDue() {
            return due;
        }
    }

    public List<InvoiceSummary> invoiceSummaries() {

        Map<String, List<Sale>> groups =
                new LinkedHashMap<>();

        for (Sale row :
                sales.findAllByOrderByCreatedAtDesc()) {

            String key = row.billGroup == null
                    ? "legacy-" + row.id
                    : row.billGroup;

            groups.computeIfAbsent(
                    key, k -> new ArrayList<>()).add(row);
        }

        List<InvoiceSummary> result =
                new ArrayList<>();

        for (List<Sale> lines : groups.values()) {
            lines.sort(
                    Comparator.comparing(x -> x.id));

            result.add(new InvoiceSummary(lines));
        }

        return result;
    }

    // =====================================
    // PAYMENTS
    // =====================================

    @Transactional
    public Payment pay(
            Long saleId,
            BigDecimal amount,
            String method,
            String reference) {

        Sale s = sales.findById(saleId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Sale not found"));

        BigDecimal a = money(amount);
        positive(a, "Payment amount");

        if (a.compareTo(s.getDue()) > 0) {
            throw new IllegalArgumentException(
                    "Payment exceeds outstanding balance");
        }

        s.paid = s.paid.add(a);
        sales.save(s);

        return savePayment(
                s, a, method, reference);
    }

    private Payment savePayment(
            Sale s,
            BigDecimal amount,
            String method,
            String reference) {

        validatePaymentMethod(method);

        Payment p = new Payment();

        p.sale = s;
        p.amount = amount;
        p.method = method;
        p.reference = reference;
        p.paidAt = LocalDateTime.now();

        return payments.save(p);
    }

    // =====================================
    // DASHBOARD TOTALS
    // =====================================

    public BigDecimal totalSales() {
        return sales.findAll().stream()
                .map(s -> s.total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal totalPaid() {
        return sales.findAll().stream()
                .map(s -> s.paid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal totalDue() {
        return totalSales().subtract(totalPaid());
    }

    public BigDecimal inventoryValue() {
        return products.findAll().stream()
                .map(p -> {
                    BigDecimal bags = p.purchasePrice.multiply(
                            BigDecimal.valueOf(p.stock));

                    if (p.bagWeightKg != null
                            && p.bagWeightKg.signum() > 0
                            && p.looseStockKg != null) {

                        BigDecimal looseValue =
                                p.purchasePrice.multiply(
                                        p.looseStockKg)
                                .divide(
                                        p.bagWeightKg,
                                        2,
                                        RoundingMode.HALF_UP);

                        return bags.add(looseValue);
                    }

                    return bags;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
