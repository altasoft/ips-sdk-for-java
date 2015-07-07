package com.ips.api.client;

import org.bouncycastle.cms.*;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoGeneratorBuilder;
import org.bouncycastle.cms.jcajce.JceCMSContentEncryptorBuilder;
import org.bouncycastle.cms.jcajce.JceKeyTransEnvelopedRecipient;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientInfoGenerator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Collection;

/**
 * Cryptographic utilities for signing and encrypting data.
 *
 */
public class CryptoUtils {
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * Computes CMS(PKCS#7) signature of message
     * @param content The content of message
     * @param privateKey Private key for signing
     * @param cert X509 Certificate
     * @return Signature bytes
     * @throws Exception
     */
    public static byte[] signCms(byte[] content, PrivateKey privateKey, X509Certificate cert) throws Exception {

        CMSSignedDataGenerator gen = new CMSSignedDataGenerator();

        gen.addSignerInfoGenerator(new JcaSimpleSignerInfoGeneratorBuilder()
                .setDirectSignature(true)
                .build("SHA1withRSA", privateKey, cert));

        return gen.generate(new CMSProcessableByteArray(content), false).getEncoded();
    }

    /**
     * Encrypts message
     * @param content  The content of message
     * @param recipientCert The recipient certificate
     * @return  Encrypted message
     * @throws Exception
     */
    public static byte[] encryptCms(byte[] content, X509Certificate recipientCert) throws Exception {

        CMSEnvelopedDataGenerator g = new CMSEnvelopedDataGenerator();
        g.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator(recipientCert));

        return g.generate(new CMSProcessableByteArray(content),
                new JceCMSContentEncryptorBuilder(CMSAlgorithm.DES_EDE3_CBC)
                        .build())
                .getEncoded();
    }

    /**
     * Decrypts CMS enveloped message
     * @param content The encrypted content
     * @param privateKey The private key
     * @return Bytes of decrypted content
     * @throws Exception
     */
    public static byte[] decryptCms(byte[] content, PrivateKey privateKey) throws Exception {

        Collection<RecipientInformation> recipients = new CMSEnvelopedData(content)
                .getRecipientInfos()
                .getRecipients();

        if (recipients.isEmpty()) {
            throw new RuntimeException("Recipient information is not provided");
        }

        return recipients.iterator().next().getContent(new JceKeyTransEnvelopedRecipient(privateKey));

    }
}
