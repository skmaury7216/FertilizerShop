# FertilizerShop — मौर्या खाद भंडार

A complete **starter web application** for fertilizer/agriculture retail shop management with Java 17, Spring Boot 3.2.7, Thymeleaf, Spring Security, H2, Docker and Jenkins.

## Screens
- `/login` admin login
- `/dashboard` sales + inventory overview
- `/products` add/edit products and view current stock
- `/purchases` buy stock from supplier, increases inventory
- `/customers` customer directory + customer-specific bills and dues
- `/suppliers` supplier directory
- `/sales` new sale and bills; stock decreases on sale
- `/sales/{id}` printable bill and partial/offline payment registration
- `/payments` customer dues and payment history
- `/reports` sales and inventory reports

## Required environment variables
There is intentionally **no built-in admin password**. Set your own password of at least 12 characters. Never commit credentials to Git.

**Windows VS Code PowerShell:**
```powershell
$env:SHOP_ADMIN_USER='shopadmin'
$env:SHOP_ADMIN_PASSWORD='PUT_A_LONG_UNIQUE_PASSWORD_HERE'
$env:SHOP_DEMO_DATA='true'  # optional sample product/customer records for testing only
mvn clean verify
java -jar target/fertilizershop-1.0.0.jar --server.port=8090
```
Then open `http://127.0.0.1:8090/login` on the same PC. Windows port 8090 must be free.

**Ubuntu shell:**
```bash
export SHOP_ADMIN_USER='shopadmin'
export SHOP_ADMIN_PASSWORD='PUT_A_LONG_UNIQUE_PASSWORD_HERE'
export SHOP_DEMO_DATA=true # optional only
mvn clean verify
java -jar target/fertilizershop-1.0.0.jar --server.port=8090
```

## Docker Compose
Copy `.env.example` to `.env`, replace the password, and run:
```bash
mvn clean verify
docker compose up -d --build
```
Website: `http://127.0.0.1:8090/login`.

## Jenkins + Ubuntu VirtualBox
1. Create separate GitHub repo for this application, e.g. `FertilizerShop`.
2. Put the project source including `Jenkinsfile`, `Dockerfile` in the repo.
3. In Jenkins: Credentials -> create **Username with password** credential ID `fertilizershop-admin`, with shop username and a long unique admin password. Do not enter these in Jenkinsfile.
4. Create a **Pipeline script from SCM** Jenkins job targeting the new repository, branch `*/main`, Script Path `Jenkinsfile`.
5. Ensure Jenkins can run Docker and Java 17/Maven are installed on the Ubuntu VM.
6. Run Build Now. Docker container binds **Ubuntu 127.0.0.1:8090** to container port 8080; this avoids conflicts with the older Student Portal on 8080 and Jenkins on 8081.
7. In VirtualBox NAT port forwarding add TCP Host IP `127.0.0.1`, Host Port `8090`, Guest IP blank, Guest Port `8090`. This makes it reachable only from your Windows host browser via `http://127.0.0.1:8090/login`.
8. Jenkins Docker named volume `fertilizershop_data` preserves H2 database files across container replacement. **Back it up regularly.**

## Important restrictions
- This is a **local development / learning system**, not a production-certified fertilizer retail or accounting platform.
- Sample demo products/prices are fictional and do not represent current government prices or licensed stock. Set `SHOP_DEMO_DATA=true` only to seed your test database.
- Cash, UPI and bank-transfer **payment records are manual entries only**, not real online collections. Enter UPI/Bank only after independently verifying the receipt. There is no payment gateway or automatic payment reconciliation.
- No GST invoice automation, fertilizer-specific statutory register, tax calculations, e-invoice, permissions audit log, inventory lot/expiry tracking or multi-user role model yet. Verify local business and agricultural regulations before real retail use.
- Before exposing the app to the public internet, use HTTPS, a more robust database (PostgreSQL), real account management, CSRF/session hardening, input validation, rate limiting, backup/restore, security testing, and legal compliance. Do not expose H2 directly.
- Product stock is integer units (bags, kg, packets); decimal kg or multi-line invoices require enhancement.

## Rebranding / migration note
The project artifact, Java package, Docker image/container, and database filename are now `fertilizershop` rather than `khadshop`. The visible shop name is **मौर्या खाद भंडार**.
This is a *new* storage path (`./data/fertilizershop`) and Docker volume (`fertilizershop_data`); existing `khadshop` data is NOT migrated automatically. Back up the old `khadshop` H2 database / Docker volume before switching.
The Jenkins credential ID has changed to `fertilizershop-admin`; create it in Jenkins before building.
