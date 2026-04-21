package edu.classproject.pricing;

import java.util.Optional;

/**
 * Repository abstraction for finding promotions by code.
 */
public interface PromoRepository {
    /**
     * Returns an Optional so missing or blank promo codes can be handled safely.
     */
    Optional<Promotion> findByCode(String code);
}
