## Synopsis — Team 7: Pricing & Promotions
**Project:** FoodDeliveryApp-E | **Module:** `edu.classproject.pricing`

---

### Overview

Team 7 is responsible for the **Pricing & Promotions** module of a framework-free Java food delivery application. The module computes the full cost breakdown for a customer's cart — including subtotal, delivery fee, tax, and any applicable promotional discounts — and exposes this as a clean interface consumed by other modules (e.g., Order, Cart).

---

### Architecture & Design

The module follows **interface-driven design** as mandated by the project's extension rules: all inter-module dependencies go through interfaces, not concrete implementations.

**Key classes and their roles:**

- **`PricingService` (Interface)** — The public contract of the module. Exposes a single method: `calculatePrice(cart: Cart, promoCode: String): PriceBreakdown`. All external modules depend only on this interface.

- **`PricingServiceImpl` (Class)** — The concrete implementation. It orchestrates the three collaborators it holds: `TaxCalculator`, `FeeCalculator`, and `PromoRepository`, and creates a `PriceBreakdown` result.

- **`PriceBreakdown` (Class)** — A value object holding `subtotal`, `deliveryFee`, `tax`, `discount`, and `total` as doubles, along with a `getFormattedTotal()` utility method for display.

- **`TaxCalculator` (Class)** — Applies a fixed `TAX_RATE` of 10% to a given amount via `calculate(amount)`.

- **`FeeCalculator` (Class)** — Computes the delivery fee based on cart contents via `calculateDeliveryFee(cart)`.

- **`PromoRepository` (Interface)** — Looks up a promotion by code (`findByCode(code): Optional<Promotion>`). The team owns the in-memory implementation of this.

- **`Promotion` (Abstract Class)** — Defines the contract for all promotions: a `code`, an `expiryDate`, and abstract methods `applyDiscount(subtotal)` and `isValid()`.

- **`PercentagePromotion` & `FixedAmountPromotion` (Classes)** — Concrete promotion types that extend `Promotion` and implement their own discount logic (percentage-off vs. fixed rupee/currency reduction).

---

### Responsibilities

- Implement and test all classes shown in the class diagram.
- Provide an in-memory `PromoRepository` implementation (the interface is team-owned).
- Ensure `PricingServiceImpl` correctly chains: subtotal → delivery fee → tax → promo discount → total.
- Validate promo codes (expiry check via `isValid()`) before applying discounts.
- Register `PricingService` so other teams (e.g., Order, Cart) can depend on it via the interface alone.

---

### Integration Points

| Depends on | Provided by |
|---|---|
| `Cart` (input to pricing) | Cart team |
| `PromoRepository` (in-memory impl) | **Team 7** |

| Exposes | Consumed by |
|---|---|
| `PricingService` interface | Order module, Cart module |
| `PriceBreakdown` value object | Order module (for receipts/display) |

---

### Tech Stack
Java 17+, Maven, JUnit 5, in-memory repositories — no external frameworks.