package com.tsix.immocrm.utils;

import java.util.Arrays;
import java.util.Optional;

/** Type d'une image reconnu sur ses premiers octets (jamais sur le nom ni le type déclaré) : PNG ou JPEG seulement. */
public final class SignatureImage {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    private SignatureImage() {
    }

    /** {@code image/png}, {@code image/jpeg}, ou vide. */
    public static Optional<String> type(byte[] octets) {
        if (commencePar(octets, PNG)) {
            return Optional.of("image/png");
        }
        return commencePar(octets, JPEG) ? Optional.of("image/jpeg") : Optional.empty();
    }

    private static boolean commencePar(byte[] octets, byte[] signature) {
        return octets.length >= signature.length && Arrays.equals(octets, 0, signature.length, signature, 0, signature.length);
    }
}
