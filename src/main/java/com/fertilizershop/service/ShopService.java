
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

    // =====================================
    // VALIDATION HELPERS
    // =====================================

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

    private void validateCustomer(
            String name, String phone, String village) {
        if (name == null || name.isBlank()
                || name.trim().length() > 120) {
            throw new IllegalArgumentException(
                    "Enter a valid customer name");
        }
        if (phone != null && phone.length() > 20) {
            throw new IllegalArgumentException(
                    "Mobile number is too long");
        }
        if (village != null && village.length() > 200) {
            throw new IllegalArgumentException(
                    "Village or address is too long");
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

        if (cleanBarcode != null && cleanBarcode.isEmpty()) {
            cleanBarcode = null;
        }

        if (cleanBarcode != null) {
            if (cleanBarcode.length() > 80
                    || !cleanBarcode.matches("[A-Za-z0-9._-]+")) {
                throw new IllegalArgumentException(
                        "Invalid product barcode");
            }

            var duplicate = products.findByBarcode(cleanBarcode);

            if (duplicate.isPresent()
                    && !duplicate.get().id.equals(p.id)) {
                throw new IllegalArgumentException(
                        "Barcode already assigned to another product");
            }
        }

        positive(bagWeightKg, "Bag weight");

        BigDecimal weight;
        try {
            weight = bagWeightKg.setScale(
                    3, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException(
                    "Bag weight supports up to 3 decimal places");
        }

        BigDecimal purchasePrice = money(buy);
        BigDecimal sellingPrice = money(sell);

        nonnegative(purchasePrice, "Purchase price");
        nonnegative(sellingPrice, "Selling price");

        if (p.bagWeightKg != null
                && p.bagWeightKg.compareTo(weight) != 0
                && (p.stock != null && p.stock > 0
                    || p.looseStockKg != null
                    && p.looseStockKg.signum() > 0)) {
            throw new IllegalArgumentException(
                    "Cannot change bag weight while stock exists");
        }

        p.barcode = cleanBarcode;
        p.name = name.trim();
        p.brand = brand;
        p.category = category;
        p.unit = unit;
        p.purchasePrice = purchasePrice;
        p.sellingPrice = sellingPrice;
        p.bagWeightKg = weight;
        p.reorderLevel = reorder == null ? 10 : reorder;

        if (p.reorderLevel < 0) {
            throw new IllegalArgumentException(
                    "Reorder level invalid");
        }

        p.description = desc;

        if (p.looseStockKg == null) {
            p.looseStockKg = BigDecimal.ZERO;
        }

        return products.save(p);
    }

    // =====================================
    // SAFE PRODUCT DELETE (SOFT DELETE)
    // =====================================

    @Transactional
    public void deleteProduct(Long id) {

        if (id == null) {
            throw new IllegalArgumentException(
                    "Invalid product ID");
        }

        Product product = products.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Product not found"));

        if (!product.active) {
            throw new IllegalArgumentException(
                    "Product is already deleted");
        }

        // Hide product without deleting its history.
        product.active = false;

        products.save(product);
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

        validateCustomer(name, phone, village);

        String last4 = aadhaarLast4 == null
                ? "" : aadhaarLast4.trim();

        if (!last4.isEmpty()
                && !last4.matches("[0-9]{4}")) {
            throw new IllegalArgumentException(
                    "Enter exactly 4 Aadhaar digits");
        }

        Customer c = new Customer();
        c.name = name.trim();
        c.phone = phone;
        c.village = village;
        c.address = address;
        c.aadhaarLast4 =
                last4.isEmpty() ? null : last4;

        return customers.save(c);
    }

    // Keep old callers compatible.
    @Transactional
    public Customer saveCustomer(
            String name,
            String phone,
            String village,
            String address) {
        return saveCustomer(
                name, phone, village, address, null);
    }

    // =====================================
    // SUPPLIER MANAGEMENT
    // =====================================

    @Transactional
    public Supplier saveSupplier(
            String name, String phone, String address) {

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
    // LEGACY SINGLE PRODUCT SALE
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

        if (p.stock == null || p.stock < quantity) {
            throw new IllegalArgumentException(
                    "Stock insufficient");
        }

        BigDecimal price = money(unitPrice);
        BigDecimal paid = money(advance);

        positive(price, "Selling price");
        nonnegative(paid, "Advance");

        BigDecimal total = price.multiply(
                BigDecimal.valueOf(quantity));

        if (paid.compareTo(total) > 0) {
            throw new IllegalArgumentException(
                    "Paid amount exceeds bill total");
        }

        if (paid.signum() > 0) {
            validatePaymentMethod(method);
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

        sale = sales.save(sale);

        if (paid.signum() > 0) {
            savePayment(sale, paid, method, reference);
        }

        return sale;
    }

    // =====================================
    // LEGACY SALE TO NEW CUSTOMER
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

        validateCustomer(
                customerName, customerPhone, customerVillage);

        qty(quantity);

        Product p = products.findById(productId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Product not found"));

        if (!p.active || p.stock < quantity) {
            throw new IllegalArgumentException(
                    "Insufficient product stock");
        }

        BigDecimal price = money(unitPrice);
        BigDecimal paid = money(advance);

        positive(price, "Selling price");
        nonnegative(paid, "Advance");

        BigDecimal total = price.multiply(
                BigDecimal.valueOf(quantity));

        if (paid.compareTo(total) > 0) {
            throw new IllegalArgumentException(
                    "Paid amount exceeds bill total");
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
                productId, c.id, quantity,
                price, paid, method, reference);
    }

    // =====================================
    // LEGACY MULTI-PRODUCT BAG SALE
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

        validateCustomer(name, phone, village);

        if (productIds == null
                || quantities == null
                || unitPrices == null
                || productIds.isEmpty()
                || productIds.size() > 50
                || productIds.size() != quantities.size()
                || productIds.size() != unitPrices.size()) {
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

            Product p = products.findById(productIds.get(i))
                    .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Product not found"));

            if (!p.active
                    || p.stock == null
                    || p.stock < quantities.get(i)) {
                throw new IllegalArgumentException(
                        "Insufficient stock for " + p.name);
            }

            BigDecimal price = money(unitPrices.get(i));
            positive(price, "Selling price");

            selected.add(p);
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
                    "Paid amount exceeds invoice total");
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

            BigDecimal lineTotal = prices.get(i).multiply(
                    BigDecimal.valueOf(amount));

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
    // WEIGHT BILLING HELPERS
    // =====================================

    private BigDecimal toKilograms(
            BigDecimal quantity,
            String unit,
            BigDecimal bagWeightKg) {

        positive(quantity, "Quantity");
        positive(bagWeightKg, "Bag weight");

        BigDecimal kg;

        if ("g".equals(unit)) {
            kg = quantity.movePointLeft(3);

        } else if ("kg".equals(unit)) {
            kg = quantity;

        } else if ("bag".equals(unit)) {
            if (quantity.stripTrailingZeros().scale() > 0) {
                throw new IllegalArgumentException(
                        "Bag quantity must be a whole number");
            }

            kg = quantity.multiply(bagWeightKg);

        } else {
            throw new IllegalArgumentException(
                    "Select Gram, Kilogram or Bag");
        }

        try {
            kg = kg.setScale(
                    3, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException(
                    "Minimum supported weight is 1 gram");
        }

        positive(kg, "Weight");
        return kg;
    }

    private BigDecimal availableKg(Product p) {
        if (p.bagWeightKg == null
                || p.bagWeightKg.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Set bag weight for " + p.name);
        }

        BigDecimal loose = p.looseStockKg == null
                ? BigDecimal.ZERO : p.looseStockKg;

        if (p.stock == null || p.stock < 0
                || loose.signum() < 0
                || loose.compareTo(p.bagWeightKg) >= 0) {
            throw new IllegalArgumentException(
                    "Invalid stock configuration for "
                            + p.name);
        }

        return p.bagWeightKg.multiply(
                BigDecimal.valueOf(p.stock)).add(loose);
    }

    private BigDecimal weightPrice(
            Product p, BigDecimal kg) {

        if (p.sellingPrice == null
                || p.sellingPrice.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Set selling price for " + p.name);
        }

        BigDecimal result = p.sellingPrice
                .multiply(kg)
                .divide(
                        p.bagWeightKg,
                        2,
                        RoundingMode.HALF_UP);

        positive(result, "Calculated bill amount");
        return result;
    }

    private void deductWeight(
            Product p, BigDecimal kg) {

        BigDecimal available = availableKg(p);

        if (kg.compareTo(available) > 0) {
            throw new IllegalArgumentException(
                    "Insufficient stock for " + p.name
                    + ". Available: " + available + " kg");
        }

        BigDecimal loose = p.looseStockKg == null
                ? BigDecimal.ZERO : p.looseStockKg;

        BigDecimal remainingLoose = loose.subtract(kg);

        if (remainingLoose.signum() >= 0) {
            p.looseStockKg = remainingLoose;

        } else {
            BigDecimal needed = remainingLoose.negate();

            int bagsToOpen = needed.divide(
                    p.bagWeightKg,
                    0,
                    RoundingMode.CEILING
            ).intValueExact();

            if (bagsToOpen > p.stock) {
                throw new IllegalArgumentException(
                        "Insufficient bags for " + p.name);
            }

            p.stock -= bagsToOpen;

            p.looseStockKg = p.bagWeightKg
                    .multiply(
                            BigDecimal.valueOf(bagsToOpen))
                    .subtract(needed);
        }

        products.save(p);
    }

    // =====================================
    // SINGLE PRODUCT GRAM / KG / BAG SALE
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

        validateCustomer(
                customerName,
                customerPhone,
                customerVillage);

        Product p = products.findById(productId)
                .orElseThrow(() ->
                    new IllegalArgumentException(
                            "Product not found"));

        if (!p.active) {
            throw new IllegalArgumentException(
                    "Product inactive");
        }

        BigDecimal kg = toKilograms(
                quantity, unit, p.bagWeightKg);

        if (kg.compareTo(availableKg(p)) > 0) {
            throw new IllegalArgumentException(
                    "Insufficient stock for " + p.name);
        }

        BigDecimal total = weightPrice(p, kg);
        BigDecimal paid = money(advance);

        nonnegative(paid, "Paid amount");

        if (paid.compareTo(total) > 0) {
            throw new IllegalArgumentException(
                    "Paid amount exceeds bill total");
        }

        if (paid.signum() > 0) {
            validatePaymentMethod(method);
        }

        Customer customer = saveCustomer(
                customerName,
                customerPhone,
                customerVillage,
                "");

        deductWeight(p, kg);

        Sale sale = new Sale();
        sale.product = p;
        sale.customer = customer;
        sale.quantity = "bag".equals(unit)
                ? quantity.intValueExact() : 1;

        sale.weightKg = kg;
        sale.weightUnit = unit;
        sale.pricePerKg = p.sellingPrice.divide(
                p.bagWeightKg, 2, RoundingMode.HALF_UP);

        sale.unitPrice = total;
        sale.total = total;
        sale.paid = paid;
        sale.createdAt = LocalDateTime.now();

        sale = sales.save(sale);

        if (paid.signum() > 0) {
            savePayment(sale, paid, method, reference);
        }

        return sale;
    }

    // =====================================
    // NEW MULTI-PRODUCT WEIGHT BILLING
    // =====================================

    @Transactional
    public Sale sellMixedWeightInvoice(
            String customerName,
            String customerPhone,
            String customerVillage,
            String aadhaarLast4,
            List<Long> productIds,
            List<BigDecimal> quantities,
            List<String> units,
            BigDecimal advance,
            String method,
            String reference) {

        validateCustomer(
                customerName,
                customerPhone,
                customerVillage);

        if (productIds == null
                || quantities == null
                || units == null
                || productIds.isEmpty()
                || productIds.size() > 50
                || productIds.size() != quantities.size()
                || productIds.size() != units.size()) {
            throw new IllegalArgumentException(
                    "Enter 1 to 50 valid product rows");
        }

        if (new HashSet<>(productIds).size()
                != productIds.size()) {
            throw new IllegalArgumentException(
                    "Select each product only once");
        }

        List<Product> selected = new ArrayList<>();
        List<BigDecimal> weights = new ArrayList<>();
        List<BigDecimal> totals = new ArrayList<>();

        BigDecimal grandTotal = BigDecimal.ZERO;

        // First validate every product and its stock.
        for (int i = 0; i < productIds.size(); i++) {

            Product p = products.findById(productIds.get(i))
                    .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Product not found"));

            if (!p.active) {
                throw new IllegalArgumentException(
                        "Inactive product: " + p.name);
            }

            BigDecimal kg = toKilograms(
                    quantities.get(i),
                    units.get(i),
                    p.bagWeightKg);

            if (kg.compareTo(availableKg(p)) > 0) {
                throw new IllegalArgumentException(
                        "Insufficient stock for " + p.name);
            }

            BigDecimal lineTotal = weightPrice(p, kg);

            selected.add(p);
            weights.add(kg);
            totals.add(lineTotal);

            grandTotal = grandTotal.add(lineTotal);
        }

        BigDecimal paid = money(advance);
        nonnegative(paid, "Paid amount");

        if (paid.compareTo(grandTotal) > 0) {
            throw new IllegalArgumentException(
                    "Paid amount exceeds invoice total");
        }

        if (paid.signum() > 0) {
            validatePaymentMethod(method);
        }

        // Create the customer only after validation.
        Customer customer = saveCustomer(
                customerName,
                customerPhone,
                customerVillage,
                "",
                aadhaarLast4);

        String groupId = UUID.randomUUID().toString();

        LocalDateTime now = LocalDateTime.now();
        BigDecimal remainingPaid = paid;

        Sale firstSale = null;

        for (int i = 0; i < selected.size(); i++) {

            Product p = selected.get(i);
            BigDecimal kg = weights.get(i);
            BigDecimal lineTotal = totals.get(i);
            String unit = units.get(i);

            deductWeight(p, kg);

            BigDecimal linePaid =
                    remainingPaid.min(lineTotal);

            remainingPaid =
                    remainingPaid.subtract(linePaid);

            Sale sale = new Sale();
            sale.product = p;
            sale.customer = customer;

            sale.quantity = "bag".equals(unit)
                    ? quantities.get(i).intValueExact()
                    : 1;

            sale.weightKg = kg;
            sale.weightUnit = unit;

            sale.pricePerKg = p.sellingPrice.divide(
                    p.bagWeightKg, 2, RoundingMode.HALF_UP);

            sale.unitPrice = lineTotal;
            sale.total = lineTotal;
            sale.paid = linePaid;
            sale.createdAt = now;
            sale.billGroup = groupId;

            sale = sales.save(sale);

            if (linePaid.signum() > 0) {
                savePayment(
                        sale, linePaid, method, reference);
            }

            if (firstSale == null) {
                firstSale = sale;
            }
        }

        return firstSale;
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
    // SALES REGISTER SUMMARY
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
                    : paid.signum() == 0
                        ? "Unpaid" : "Partial";
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

        validatePaymentMethod(method);

        s.paid = s.paid.add(a);
        sales.save(s);

        return savePayment(s, a, method, reference);
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
