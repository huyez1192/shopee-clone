# Mini Shopee backend

## Local configuration

The backend reads database, JWT, SePay, and optional Cloudinary configuration from environment variables. Copy the names from `.env.example`, but do not commit real values. Spring Boot does not load `.env` files automatically.

Required variables:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `SEPAY_BANK_CODE`
- `SEPAY_ACCOUNT_NUMBER`
- `SEPAY_ACCOUNT_NAME`
- `SEPAY_WEBHOOK_SECRET`

Cloudinary is optional. Product, shop, and user APIs continue to accept regular image URLs when it is disabled.

To use URL fields only:

```text
CLOUDINARY_ENABLED=false
```

To enable file uploads, set all four variables:

- `CLOUDINARY_ENABLED=true`
- `CLOUDINARY_CLOUD_NAME`
- `CLOUDINARY_API_KEY`
- `CLOUDINARY_API_SECRET`

Startup fails with a clear configuration error if Cloudinary is explicitly enabled while any credential is missing.

### IntelliJ IDEA

Open the Spring Boot run configuration, select **Modify options > Environment variables**, and add the variables above. Store values in the local run configuration only; do not share or commit that configuration.

### Maven / terminal

PowerShell example for the current terminal session:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/product_crud_db"
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "your-local-password"
$env:JWT_SECRET = "your-long-random-secret"
$env:SEPAY_BANK_CODE = "your-bank-code"
$env:SEPAY_ACCOUNT_NUMBER = "your-account-number"
$env:SEPAY_ACCOUNT_NAME = "your-account-name"
$env:SEPAY_WEBHOOK_SECRET = "your-webhook-secret"
$env:CLOUDINARY_ENABLED = "false"
.\mvnw.cmd spring-boot:run
```

For file uploads, change `CLOUDINARY_ENABLED` to `true` and also set the three `CLOUDINARY_*` credential variables shown above.

On macOS/Linux, export the same variables before running `./mvnw spring-boot:run`.

An optional `src/main/resources/application-local.properties` may be used for local-only overrides. It is ignored by Git; activate it explicitly with `SPRING_PROFILES_ACTIVE=local`. Never put real credentials in a tracked properties file.

### Docker

This repository currently has no Dockerfile or Compose configuration. If Docker support is added later, pass the same variable names through the container environment or an untracked env file.
