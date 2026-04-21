package edu.classproject.pricing;

import edu.classproject.common.Money;

/**
 * Immutable result returned by the pricing module after all calculations are complete.
 */
public record PriceBreakdown(Money subtotal, Money deliveryFee, Money tax, Money discount, Money total) {
	/**
	 * Formats the final payable amount for simple console or UI output.
	 */
	public String getFormattedTotal() {
		return total.toString();
	}
}
