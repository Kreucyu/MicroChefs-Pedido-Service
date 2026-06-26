package com.service.pedidos.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PixUtils {

    public static String gerarPixCopiaECola(Long orderId, BigDecimal valor) {
        StringBuilder sb = new StringBuilder();

        // 00: Payload Format Indicator (01)
        sb.append("000201");

        // 26: Merchant Account Information (Pix Key)
        String merchantKey = "financeiro@microchefs.com.br";
        String merchantInfo = "0014br.gov.bcb.pix" + String.format("01%02d%s", merchantKey.length(), merchantKey);
        sb.append(String.format("26%02d%s", merchantInfo.length(), merchantInfo));

        // 52: Merchant Category Code (5812)
        sb.append("52045812");

        // 53: Transaction Currency (986 - BRL)
        sb.append("5303986");

        // 54: Transaction Amount
        String valorStr = valor.setScale(2, RoundingMode.HALF_UP).toString();
        sb.append(String.format("54%02d%s", valorStr.length(), valorStr));

        // 58: Country Code (BR)
        sb.append("5802BR");

        // 59: Merchant Name (MicroChefs)
        sb.append("5910MicroChefs");

        // 60: Merchant City (SAO PAULO)
        sb.append("6009SAO PAULO");

        // 62: Additional Data Field (TxID)
        String txid = String.format("MC%08d", orderId);
        String additionalData = String.format("05%02d%s", txid.length(), txid);
        sb.append(String.format("62%02d%s", additionalData.length(), additionalData));

        // 63: CRC16
        sb.append("6304");

        String pixWithoutCrc = sb.toString();
        String crc = calcularCRC16(pixWithoutCrc);
        return pixWithoutCrc + crc;
    }

    private static String calcularCRC16(String str) {
        int crc = 0xFFFF;
        int polynomial = 0x1021;

        for (byte b : str.getBytes()) {
            for (int i = 0; i < 8; i++) {
                boolean bit = ((b >> (7 - i) & 1) == 1);
                boolean c15 = ((crc >> 15 & 1) == 1);
                crc <<= 1;
                if (c15 ^ bit) {
                    crc ^= polynomial;
                }
            }
        }

        crc &= 0xFFFF;
        return String.format("%04X", crc);
    }
}
