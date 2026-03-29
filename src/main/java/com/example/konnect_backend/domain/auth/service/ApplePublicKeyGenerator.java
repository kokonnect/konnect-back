package com.example.konnect_backend.domain.auth.service;

import com.example.konnect_backend.global.code.status.ErrorStatus;
import com.example.konnect_backend.global.exception.GeneralException;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public class ApplePublicKeyGenerator {

    public static PublicKey generate(Map<String, Object> keys, String kid) {
        List<Map<String, String>> keyList = (List<Map<String, String>>) keys.get("keys");

        Map<String, String> key = keyList.stream()
                .filter(k -> k.get("kid").equals(kid))
                .findFirst()
                .orElseThrow(() -> new GeneralException(ErrorStatus.OAUTH_TOKEN_INVALID));

        byte[] nBytes = Base64.getUrlDecoder().decode(key.get("n"));
        byte[] eBytes = Base64.getUrlDecoder().decode(key.get("e"));

        BigInteger modulus = new BigInteger(1, nBytes);
        BigInteger exponent = new BigInteger(1, eBytes);

        try {
            RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
            KeyFactory factory = KeyFactory.getInstance("RSA");
            return factory.generatePublic(spec);
        } catch (Exception e) {
            throw new GeneralException(ErrorStatus.OAUTH_TOKEN_INVALID);
        }
    }
}