package com.voltra.platform.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Refund;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class RazorpayGatewayService {
    private final String keyId;
    private final String keySecret;
    private final String webhookSecret;

    public RazorpayGatewayService(@Value("${voltra.razorpay.key-id:}") String keyId,
                                  @Value("${voltra.razorpay.key-secret:}") String keySecret,
                                  @Value("${voltra.razorpay.webhook-secret:}") String webhookSecret) {
        this.keyId = keyId;
        this.keySecret = keySecret;
        this.webhookSecret = webhookSecret;
    }

    public String getKeyId() {
        return keyId;
    }

    public boolean isConfigured() {
        return keyId != null && !keyId.isBlank() && keySecret != null && !keySecret.isBlank();
    }

    public String createOrder(BigDecimal amountInr, String receiptRef) {
        if (!isConfigured()) {
            throw new IllegalStateException("Razorpay gateway credentials (RAZORPAY_KEY_ID / RAZORPAY_KEY_SECRET) are not configured on the server.");
        }
        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);
            long paise = amountInr.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValue();
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", paise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", receiptRef);
            Order order = client.orders.create(orderRequest);
            return order.get("id");
        } catch (RazorpayException e) {
            throw new IllegalStateException("Payment gateway order creation failed: " + e.getMessage());
        }
    }

    public boolean verifyCheckoutSignature(String orderId, String paymentId, String signature) {
        if (!isConfigured()) {
            throw new IllegalStateException("Razorpay gateway credentials are not configured.");
        }
        try {
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", orderId);
            attributes.put("razorpay_payment_id", paymentId);
            attributes.put("razorpay_signature", signature);
            return Utils.verifyPaymentSignature(attributes, keySecret);
        } catch (RazorpayException e) {
            return false;
        }
    }

    public boolean verifyWebhookSignature(String rawPayload, String signatureHeader) {
        if (webhookSecret == null || webhookSecret.isBlank() || signatureHeader == null || signatureHeader.isBlank()) {
            return false;
        }
        try {
            return Utils.verifyWebhookSignature(rawPayload, signatureHeader, webhookSecret);
        } catch (RazorpayException e) {
            return false;
        }
    }

    public String issueFullRefund(String paymentId) {
        if (!isConfigured()) {
            throw new IllegalStateException("Razorpay gateway credentials are not configured for refund processing.");
        }
        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);
            Refund refund = client.payments.refund(paymentId, new JSONObject());
            return refund.get("id");
        } catch (RazorpayException e) {
            throw new IllegalStateException("Payment gateway refund failed: " + e.getMessage());
        }
    }
}
