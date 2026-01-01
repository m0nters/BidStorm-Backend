package com.taitrinh.online_auction.dto.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentIntentResponse {

    private String clientSecret;
    private Long amountCents;
    private String currency;
}
