package dev.ntziks.loyalty.api.dto;

public class LoyaltyQuoteRequest {
    private double fareAmount;
    private String currency;
    private String cabinClass;
    private String customerTier;
    private String promoCode;

    public LoyaltyQuoteRequest(double fareAmount, String currency, String cabinClass, String customerTier, String promoCode) {
        this.fareAmount = fareAmount;
        this.currency = currency;
        this.cabinClass = cabinClass;
        this.customerTier = customerTier;
        this.promoCode = promoCode;
    }

    public LoyaltyQuoteRequest() {
    }

    public double getFareAmount() {
        return fareAmount;
    }

    public void setFareAmount(double fareAmount) {
        this.fareAmount = fareAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getCabinClass() {
        return cabinClass;
    }

    public void setCabinClass(String cabinClass) {
        this.cabinClass = cabinClass;
    }

    public String getCustomerTier() {
        return customerTier;
    }

    public void setCustomerTier(String customerTier) {
        this.customerTier = customerTier;
    }

    public String getPromoCode() {
        return promoCode;
    }

    public void setPromoCode(String promoCode) {
        this.promoCode = promoCode;
    }
}

