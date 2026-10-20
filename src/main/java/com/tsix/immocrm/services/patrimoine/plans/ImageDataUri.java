package com.tsix.immocrm.services.patrimoine.plans;

import com.tsix.immocrm.utils.SignatureImage;

import java.util.Base64;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Image d'un plan : seules les data-URI {@code image/png} et {@code image/jpeg} en base64 dont les octets sont du type
 * déclaré sont acceptées, à l'import comme à la lecture (jamais de SVG resservi).
 */
public final class ImageDataUri {

    private final String contentType;
    private final byte[] octets;

    private ImageDataUri(String contentType, byte[] octets) {
        this.contentType = contentType;
        this.octets = octets;
    }

    public String contentType() {
        return contentType;
    }

    public byte[] octets() {
        return octets;
    }

    /** Vide si la valeur n'est pas une data-URI PNG ou JPEG dont les octets sont du type déclaré. */
    public static Optional<ImageDataUri> lire(String dataUri) {
        if (dataUri == null) {
            return Optional.empty();
        }
        return Stream.of("image/png", "image/jpeg")
                .filter(type -> dataUri.startsWith("data:" + type + ";base64,"))
                .findFirst()
                .flatMap(type -> decoder(dataUri.substring(dataUri.indexOf(',') + 1))
                        .filter(octets -> SignatureImage.type(octets).filter(type::equals).isPresent())
                        .map(octets -> new ImageDataUri(type, octets)));
    }

    private static Optional<byte[]> decoder(String base64) {
        try {
            return Optional.of(Base64.getMimeDecoder().decode(base64));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
