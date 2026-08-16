package com.luckydraw.gateway.error;

/**
 * Gateway 自產錯誤的 envelope body（gateway-service.yaml ErrorEnvelope）。
 * 僅承載 gateway 自產碼（A0202/A0203/A0500/A0501）；業務碼由下游產生。
 */
public record ErrorEnvelopeBody(String code, String message, Object data) {
}
