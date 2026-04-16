package edu.classproject.pricing;

import edu.classproject.common.Money;

public record PriceBreakdown(Money subtotal, Money deliveryFee, Money tax, Money discount, Money total) {
	public String getFormattedTotal() {
		return total.toString();
	}
}
