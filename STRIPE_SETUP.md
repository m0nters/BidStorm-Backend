# Stripe Setup Guide for Localhost Development

Quick guide to set up Stripe for testing the payment system locally.

## Prerequisites
- Spring Boot application running on `localhost:8080`
- Stripe CLI installed

## Step 1: Create Stripe Account

1. Go to https://stripe.com and sign up
2. Verify your email
3. **Enable Test Mode** (toggle at top right of dashboard)

## Step 2: Get API Keys

1. Dashboard → **Developers** → **API keys**
2. Copy both keys:
   - **Publishable key**: `pk_test_...` (for frontend)
   - **Secret key**: `sk_test_...` (for backend - click "Reveal test key")

## Step 3: Install Stripe CLI

**Windows:**
1. Download from: https://github.com/stripe/stripe-cli/releases/latest
2. Extract `stripe.exe`
3. Add to PATH or run from the folder

**Verify installation:**
```bash
stripe --version
```

## Step 4: Login to Stripe CLI

```bash
stripe login
```
- This opens your browser for authentication
- Follow the prompts to authorize the CLI

## Step 5: Forward Webhooks to Localhost

Start the webhook listener:
```bash
stripe listen --forward-to localhost:8080/api/v1/webhooks/stripe
```

**Important:** Keep this terminal running while developing!

The output will show:
```
Ready! Your webhook signing secret is whsec_abc123...
```

**Copy the webhook signing secret** (starts with `whsec_`)

## Step 6: Configure Application

Add to `src/main/resources/application.properties`:

```properties
# Stripe API Keys
stripe.api.key=sk_test_YOUR_SECRET_KEY_HERE
stripe.publishable.key=pk_test_YOUR_PUBLISHABLE_KEY_HERE
stripe.webhook.secret=whsec_YOUR_WEBHOOK_SECRET_FROM_CLI
stripe.currency=USD
```

Replace with your actual keys from Steps 2 and 5.

## Step 7: Add Maven Dependency

Add to `pom.xml`:
```xml
<dependency>
    <groupId>com.stripe</groupId>
    <artifactId>stripe-java</artifactId>
    <version>24.0.0</version>
</dependency>
```

Then run:
```bash
mvn clean install
```

## Testing

### Test Payment Card
Use this card in Stripe test mode:
- **Card Number**: `4242 4242 4242 4242`
- **Expiry**: Any future date (e.g., `12/34`)
- **CVC**: Any 3 digits (e.g., `123`)

### Trigger Test Webhook
In a separate terminal:
```bash
stripe trigger payment_intent.succeeded
```

Check your application logs to verify the webhook is received.

## Common Commands

```bash
# Login to Stripe
stripe login

# Listen for webhooks
stripe listen --forward-to localhost:8080/api/v1/webhooks/stripe

# Trigger test events
stripe trigger payment_intent.succeeded

# View Stripe logs
stripe logs tail
```

## Troubleshooting

**Webhook not working?**
- Make sure Spring Boot is running on port 8080
- Ensure Stripe CLI listener is running
- Check webhook secret matches in application.properties

**API errors?**
- Verify Test Mode is ON in dashboard
- Check API keys are correct (start with `sk_test_` and `pk_test_`)

## Done!

✅ Stripe account created  
✅ API keys configured  
✅ Webhook listener running  
✅ Ready to test payments
