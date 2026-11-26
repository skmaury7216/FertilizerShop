
document.addEventListener("DOMContentLoaded", () => {
    "use strict";

    const $ = (selector, root = document) =>
        root.querySelector(selector);

    const $$ = (selector, root = document) =>
        Array.from(root.querySelectorAll(selector));

    const money = value =>
        "₹" + (
            Number.isFinite(value) ? value : 0
        ).toFixed(2);

    const round2 = value =>
        Math.round((value + Number.EPSILON) * 100) / 100;

    const weightText = kg =>
        Number.isFinite(kg) && kg > 0
            ? kg.toFixed(3) + " kg"
            : "0 kg";

    // =====================================
    // DATE AND SEARCH
    // =====================================

    const today = $("#today");

    if (today) {
        today.textContent = new Date().toLocaleDateString(
            "en-IN",
            {
                day: "numeric",
                month: "short",
                year: "numeric"
            }
        );
    }

    $$("[data-filter]").forEach(input => {
        input.addEventListener("input", () => {
            const table = document.getElementById(
                input.dataset.filter
            );

            if (!table) return;

            const query = input.value.trim().toLowerCase();

            $$("tbody tr", table).forEach(row => {
                row.style.display =
                    row.textContent.toLowerCase().includes(query)
                        ? ""
                        : "none";
            });
        });
    });

    // =====================================
    // SHARED PRODUCT / WEIGHT CALCULATOR
    // =====================================

    function productInfo(select) {
        const option = select?.selectedOptions?.[0];

        if (!select || !select.value || !option) {
            return null;
        }

        const price = Number(option.dataset.price);
        const bagKg = Number(option.dataset.weight);
        const stockKg = Number(option.dataset.stock);

        return {
            price,
            bagKg,
            stockKg,
            valid:
                Number.isFinite(price) &&
                price > 0 &&
                Number.isFinite(bagKg) &&
                bagKg > 0 &&
                Number.isFinite(stockKg) &&
                stockKg >= 0
        };
    }

    function calculateLine(product, quantity, unit) {
        const info = productInfo(product);

        if (!info || !info.valid) {
            return {
                ok: false,
                info,
                error: "Select a product with bag weight and price."
            };
        }

        const entered = Number(quantity.value);

        if (
            quantity.value.trim() === "" ||
            !Number.isFinite(entered) ||
            entered <= 0
        ) {
            return {
                ok: false,
                info,
                error: "Enter a valid quantity."
            };
        }

        let kg;

        if (unit.value === "g") {
            kg = entered / 1000;
        } else if (unit.value === "kg") {
            kg = entered;
        } else if (unit.value === "bag") {
            kg = entered * info.bagKg;
        } else {
            kg = NaN;
        }

        if (
            !Number.isFinite(kg) ||
            kg <= 0 ||
            Math.abs(kg * 1000 - Math.round(kg * 1000)) > 0.0000001
        ) {
            return {
                ok: false,
                info,
                kg,
                error: "Minimum weight increment is 1 gram."
            };
        }

        if (
            unit.value === "bag" &&
            !Number.isInteger(entered)
        ) {
            return {
                ok: false,
                info,
                kg,
                error: "Enter a whole number of bags."
            };
        }

        if (kg > info.stockKg + 0.00000001) {
            return {
                ok: false,
                info,
                kg,
                error:
                    "Insufficient stock. Available " +
                    info.stockKg.toFixed(3) +
                    " kg."
            };
        }

        const amount = round2(
            kg * info.price / info.bagKg
        );

        if (amount <= 0) {
            return {
                ok: false,
                info,
                kg,
                error: "Calculated amount must exceed ₹0."
            };
        }

        return {
            ok: true,
            info,
            kg,
            amount,
            error: ""
        };
    }

    // =====================================
    // SINGLE PRODUCT WEIGHT BILLING
    // =====================================

    const weightForm = $("#weight-sale-form");

    if (weightForm) {
        const product = $("#weightProduct");
        const quantity = $("#weightQuantity");
        const unit = $("#weightUnit");
        const paid = $("#weightPaid");
        const submit = $("#weightSubmit");
        const validation = $("#weightValidation");

        function recalcWeight() {
            const result = calculateLine(
                product,
                quantity,
                unit
            );

            const info = result.info;

            $("#weightStock").textContent =
                info?.valid
                    ? info.stockKg.toFixed(3) + " kg"
                    : "—";

            $("#selectedBagWeight").textContent =
                info?.valid
                    ? info.bagKg + " kg"
                    : "Not configured";

            $("#selectedBagPrice").textContent =
                info?.valid
                    ? money(info.price)
                    : "₹0.00";

            $("#selectedKgPrice").textContent =
                info?.valid
                    ? money(info.price / info.bagKg)
                    : "₹0.00";

            $("#calculatedWeight").textContent =
                weightText(result.kg);

            $("#weightTotal").textContent =
                money(result.amount);

            const payment = Number(paid.value);

            const validPayment =
                paid.value.trim() !== "" &&
                Number.isFinite(payment) &&
                payment >= 0 &&
                payment <= (result.amount ?? 0);

            $("#weightDue").textContent = money(
                Math.max(
                    0,
                    (result.amount ?? 0) -
                    (Number.isFinite(payment) ? payment : 0)
                )
            );

            submit.disabled =
                !(result.ok && validPayment);

            validation.textContent =
                !result.ok
                    ? result.error
                    : !validPayment
                        ? "Paid amount cannot exceed bill total."
                        : "Ready to generate invoice.";

            return !submit.disabled;
        }

        [product, quantity, unit, paid].forEach(element => {
            element.addEventListener("change", recalcWeight);
            element.addEventListener("input", recalcWeight);
        });

        weightForm.addEventListener("submit", event => {
            if (!recalcWeight()) {
                event.preventDefault();
                alert(validation.textContent);
            }
        });

        recalcWeight();
    }

    // =====================================
    // MULTI-PRODUCT WEIGHT BILLING
    // =====================================

    const multiForm = $("#multi-sale-form");
    const lines = $("#invoice-lines");

    if (multiForm && lines) {
        const addButton = $("#add-invoice-product");
        const paid = $("#invoice-paid");
        const submit = $("#multi-sale-submit");
        const validation = $("#multi-sale-validation");
        const scan = $("#sale-barcode-scan");
        const scanStatus = $("#sale-barcode-status");

        const rows = () =>
            $$(".invoice-line", lines);

        function updateRow(row) {
            const result = calculateLine(
                $(".invoice-product", row),
                $(".invoice-quantity", row),
                $(".invoice-unit", row)
            );

            const info = result.info;

            $(".invoice-bag-weight", row).textContent =
                info?.valid ? info.bagKg + " kg" : "—";

            $(".invoice-bag-price", row).textContent =
                info?.valid ? money(info.price) : "₹0.00";

            $(".invoice-stock", row).textContent =
                info?.valid
                    ? info.stockKg.toFixed(3) + " kg"
                    : "—";

            $(".invoice-weight", row).textContent =
                weightText(result.kg);

            $(".invoice-line-total", row).textContent =
                money(result.amount);

            $(".invoice-row-warning", row).textContent =
                result.error;

            return result;
        }

        function recalcMulti() {
            let total = 0;
            let valid = true;
            let error = "";

            const selected = new Set();

            for (const row of rows()) {
                const result = updateRow(row);
                const id = $(".invoice-product", row).value;

                if (!result.ok) {
                    valid = false;

                    if (!error) {
                        error = result.error;
                    }
                } else {
                    total += result.amount;
                }

                if (id && selected.has(id)) {
                    valid = false;
                    error =
                        "Each product can appear only once. " +
                        "Increase its quantity instead.";
                }

                if (id) {
                    selected.add(id);
                }
            }

            total = round2(total);

            const payment = Number(paid.value);

            if (
                paid.value.trim() === "" ||
                !Number.isFinite(payment) ||
                payment < 0 ||
                payment > total
            ) {
                valid = false;

                if (!error) {
                    error = "Invalid Paid Now amount.";
                }
            }

            if (total <= 0) {
                valid = false;
            }

            $("#sale-total").textContent = money(total);

            $("#invoice-due").textContent = money(
                Math.max(
                    0,
                    total -
                    (Number.isFinite(payment) ? payment : 0)
                )
            );

            submit.disabled = !valid;

            validation.textContent = valid
                ? "Ready to generate single invoice."
                : error || "Select a valid product and quantity.";

            return valid;
        }

        function addRow() {
            if (rows().length >= 50) {
                alert("Maximum 50 products per invoice.");
                return null;
            }

            const first = rows()[0];

            if (!first) return null;

            const row = first.cloneNode(true);

            $(".invoice-product", row).value = "";
            $(".invoice-quantity", row).value = "1";
            $(".invoice-unit", row).value = "kg";
            $(".invoice-row-warning", row).textContent = "";

            lines.appendChild(row);

            recalcMulti();

            return row;
        }

        addButton.addEventListener("click", () => {
            const row = addRow();

            if (row) {
                $(".invoice-product", row).focus();
            }
        });

        lines.addEventListener("click", event => {
            const remove =
                event.target.closest(".invoice-remove");

            if (!remove) return;

            if (rows().length <= 1) {
                alert("At least one product is required.");
                return;
            }

            remove.closest(".invoice-line").remove();

            recalcMulti();
        });

        lines.addEventListener("change", recalcMulti);
        lines.addEventListener("input", recalcMulti);

        paid.addEventListener("input", recalcMulti);

        // Barcode scanner adds one full bag.
        function scanBarcode() {
            if (!scan) return;

            const code = scan.value.trim();

            if (!code) return;

            const option = $$(".invoice-product option", lines)
                .find(item =>
                    item.value &&
                    item.dataset.barcode === code
                );

            if (!option) {
                scanStatus.textContent =
                    "Barcode not found in Products.";

                scanStatus.style.color = "#b42318";
                return;
            }

            let row = rows().find(item =>
                $(".invoice-product", item).value === option.value
            );

            if (row) {
                const unit = $(".invoice-unit", row);
                const quantity = $(".invoice-quantity", row);

                if (unit.value !== "bag") {
                    scanStatus.textContent =
                        "Product is already listed by weight. " +
                        "Change quantity manually.";

                    scanStatus.style.color = "#b42318";
                    return;
                }

                quantity.value =
                    (Number(quantity.value) || 0) + 1;

            } else {
                row = rows().find(item =>
                    !$(".invoice-product", item).value
                );

                if (!row) {
                    row = addRow();
                }

                if (!row) return;

                $(".invoice-product", row).value = option.value;
                $(".invoice-quantity", row).value = "1";
                $(".invoice-unit", row).value = "bag";
            }

            scanStatus.textContent =
                "Added one bag: " + option.textContent.trim();

            scanStatus.style.color = "#16856c";

            scan.value = "";

            recalcMulti();
        }

        if (scan && scanStatus) {
            scan.addEventListener("keydown", event => {
                if (event.key === "Enter") {
                    event.preventDefault();
                    scanBarcode();
                }
            });
        }

        multiForm.addEventListener("submit", event => {
            if (!recalcMulti()) {
                event.preventDefault();
                alert(validation.textContent);
            }
        });

        recalcMulti();
    }

    // =====================================
    // STOCK PURCHASE BARCODE SCANNER
    // =====================================

    const purchaseScan = $("#purchase-barcode-scan");
    const purchaseProduct = $("#purchase-product");
    const purchaseStatus = $("#purchase-barcode-status");

    if (
        purchaseScan &&
        purchaseProduct &&
        purchaseStatus
    ) {
        function scanPurchase() {
            const code = purchaseScan.value.trim();

            if (!code) return;

            const option = $$("option", purchaseProduct)
                .find(item =>
                    item.dataset.barcode === code
                );

            if (!option) {
                purchaseStatus.textContent =
                    "Barcode not found.";

                purchaseStatus.style.color = "#b42318";
                return;
            }

            purchaseProduct.value = option.value;

            purchaseProduct.dispatchEvent(
                new Event("change", {
                    bubbles: true
                })
            );

            purchaseStatus.textContent =
                "Selected: " + option.textContent.trim();

            purchaseStatus.style.color = "#16856c";

            purchaseScan.value = "";
        }

        purchaseScan.addEventListener(
            "keydown",
            event => {
                if (event.key === "Enter") {
                    event.preventDefault();
                    scanPurchase();
                }
            }
        );

        purchaseScan.addEventListener(
            "change",
            scanPurchase
        );
    }
});
