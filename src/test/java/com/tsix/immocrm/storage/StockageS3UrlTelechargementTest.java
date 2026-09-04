package com.tsix.immocrm.storage;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * En-têtes portés par l'URL présignée. Test unitaire sans contexte Spring et <b>sans réseau</b> :
 * la signature SigV4 est un calcul local, aucun appel n'est émis vers MinIO.
 *
 * <p>Il existe parce que {@link StockageMemoire} rend les tests de contrôleur aveugles à ces
 * en-têtes — ils n'assertent que la clé portée par la redirection. C'est ce trou qui a laissé
 * passer un {@code attachment} en dur cassant la visualisation des pièces jointes.
 */
class StockageS3UrlTelechargementTest {

    private static final String CLE = "prod/pieces-jointes/dossier/e6cae990/a44e2955.pdf";

    private static StockageS3 stockage() {
        StockageProperties properties = new StockageProperties();
        properties.setBucket("immo-test");
        properties.setEndpoint("https://s3.exemple.test");
        properties.setRegion("us-east-1");
        properties.setAccessKey("cle-de-test");
        properties.setSecretKey("secret-de-test");

        S3Presigner presigner = S3Presigner.builder()
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey())))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .endpointOverride(URI.create(properties.getEndpoint()))
                .build();

        // urlTelechargement() ne touche jamais au S3Client — d'où le null assumé ici.
        return new StockageS3(null, presigner, properties);
    }

    /** URL présignée, décodée une fois pour annuler l'encodage de query string du signataire. */
    private static String urlDecodee(String nomTelechargement) {
        URI url = stockage().urlTelechargement(CLE, nomTelechargement, "application/pdf");
        return URLDecoder.decode(url.toString(), StandardCharsets.UTF_8);
    }

    @Test
    void imposeInlinePourQueLeNavigateurAfficheSansTelecharger() {
        // Régression : un "attachment" en dur forçait le téléchargement des PDF et des images,
        // alors que le backend les servait en inline avant le passage à S3.
        assertThat(urlDecodee("Contrat de vente"))
                .contains("response-content-disposition=inline;")
                .doesNotContain("attachment");
    }

    @Test
    void porteLeContentTypeEtLeNomAccentueEnUtf8AvecSonRepliAscii() {
        String url = urlDecodee("Pièce d'identité");

        assertThat(url).contains("response-content-type=application/pdf");
        // Repli ASCII pour les clients anciens...
        assertThat(url).contains("filename=\"Pi_ce_d_identit_\"");
        // ...et nom réel en RFC 5987, un en-tête HTTP ne transportant pas d'UTF-8 brut.
        assertThat(url).contains("filename*=UTF-8''Pi%C3%A8ce%20d%27identit%C3%A9");
    }
}
