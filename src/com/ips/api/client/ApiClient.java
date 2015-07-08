package com.ips.api.client;

import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.GenericJson;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;
import com.google.api.client.util.DateTime;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * IPS Api client. used to send receive and modify IPS messages
 */
public class ApiClient {
    private final String apiAddress;
    private final String participantId;
    private final PrivateKey privateKey;
    private final X509Certificate certificate;
    private Map<String, String> links;
    private boolean fetchLinks = true;
    private final HttpRequestFactory requestFactory = new NetHttpTransport()
            .createRequestFactory(new HttpRequestInitializer() {
                @Override
                public void initialize(HttpRequest httpRequest) throws IOException {
                    httpRequest.setParser(new JsonObjectParser(new com.google.api.client.json.jackson2.JacksonFactory()));
                }
            });

    /**
     * Creates instance of IpsClient
     *
     * @param apiAddress    Api root address
     * @param participantId Participant identifier (BIC)
     * @param privateKey    Participant private key
     * @param certificate   Participant X509 certificate
     */
    public ApiClient(String apiAddress,
                     String participantId,
                     PrivateKey privateKey,
                     X509Certificate certificate) {
        this.apiAddress = apiAddress;
        this.participantId = participantId;
        this.privateKey = privateKey;
        this.certificate = certificate;
    }

    /**
     * Sends IPS message
     *
     * @param receiverCertificate Receivers X509 certificate
     * @param receiver     Receiver identifier (BIC)
     * @param ref          Message reference, used to identify message
     * @param type         Message type
     * @param date         Date
     * @param content      Plant content
     * @param amount       Message amount
     * @param ccy          Currency
     * @throws Exception
     */
    public void send(X509Certificate receiverCertificate,
                     String receiver,
                     String ref,
                     Short type,
                     DateTime date,
                     String content,
                     BigDecimal amount,
                     String ccy) throws Exception {

        signRequest(requestFactory.buildPutRequest(makeUrl(getOutboxMessageUri(ref)),
                new JsonHttpContent(new JacksonFactory(),
                        new Message()
                                .withRef(ref)
                                .withReceiver(receiver)
                                .withType(type)
                                .withDate(date)
                                .withAmount(amount)
                                .withCcy(ccy)
                                .withContentBytes(CryptoUtils.encryptCms(content.getBytes("UTF8"), receiverCertificate)))))
                .execute();
    }

    /**
     * Cancels sent message if it is not processed yet.
     *
     * @param ref    Message reference
     * @param reason The reason of cancellation
     * @throws Exception
     */
    public void cancel(String ref, String reason) throws Exception {
        setMessageState(getOutboxMessageUri(ref), "Cancelled", reason);
    }

    /**
     * Fetches outbox (sent messages)
     *
     * @param uri Outbox uri, optional parameter, returns all items from outbox if uri is null
     *            if uri specified, subset of outbox items will be returned which are modified after specified time.
     * @return The list of outbox items
     * @throws Exception
     */
    public MessageCollection browseOutbox(String uri) throws Exception {
        return browseMessages(uri == null ? getLink("outboxState") : uri);
    }

    /**
     * Fetches inbox (received messages)
     *
     * @param all Optional parameter, returns all inbox items if parameter is true, otherwise only new
     *            (not completed, not rejected) items will be returned
     * @return The list of inbox items
     * @throws Exception
     */
    public MessageCollection browseInbox(boolean all) throws Exception {
        return browseMessages(getLink(all ? "inbox" : "inboxNew"));
    }

    /**
     * Completes (accepts) message
     *
     * @param messageId Message identifier
     * @throws Exception
     */
    public void complete(int messageId) throws Exception {
        setMessageState(getInboxMessageUri(messageId), "Completed", null);
    }

    /**
     * Rejects message
     *
     * @param messageId message identifier
     * @param reason    The reason of rejection
     * @throws Exception
     */
    public void reject(int messageId, String reason) throws Exception {
        setMessageState(getInboxMessageUri(messageId), "Rejected", reason);
    }

    private MessageCollection browseMessages(String uri) throws IOException {

        return this.requestFactory.buildGetRequest(
                new GenericUrl(
                        new StringBuilder(this.apiAddress)
                                .append(uri)
                                .toString()))
                .execute()
                .parseAs(MessageCollection.class);
    }

    private void setMessageState(String messageUri, String state, String description) throws Exception {
        Map<String, String> stateData = new HashMap<String, String>();
        stateData.put("state", state);

        if (description != null) {
            stateData.put("stateDescrip", description);
        }

        HttpRequest request = requestFactory.buildPostRequest(makeUrl(messageUri),
                new JsonHttpContent(new JacksonFactory(), stateData));
        request.getHeaders().put("X-HTTP-Method-Override", "PATCH");
        signRequest(request).execute();
    }

    private String getLink(String rel) throws IOException {
        if (fetchLinks) {
            GenericUrl url = new GenericUrl(this.apiAddress);
            url.appendRawPath("/api/");
            url.appendRawPath(participantId);
            url.set("fields", "links");

            HttpRequest request = requestFactory.buildGetRequest(url);
            HttpResponse response = request.execute();
            this.links = (Map) response.parseAs(GenericJson.class).get("links");
            this.fetchLinks = false;
        }

        return this.links.get(rel);
    }

    private String getInboxMessageUri(int messageId) throws IOException {
        return new StringBuilder(getLink("inbox")).append("/").append(messageId).toString();
    }

    private String getOutboxMessageUri(String ref) throws IOException {
        return new StringBuilder(getLink("outbox"))
                .append("/")
                .append(ref)
                .toString();
    }

    private GenericUrl makeUrl(String uri) {
        return new GenericUrl(new StringBuffer(this.apiAddress).append(uri).toString());
    }

    private HttpRequest signRequest(HttpRequest request) throws Exception {
        request.getHeaders()
                .setAuthorization(new StringBuilder("IPSAuth ")
                        .append(this.participantId)
                        .append(":")
                        .append(Base64.encodeBase64String(
                                CryptoUtils.signCms(
                                        canonicalizeRequest(request).getBytes("UTF8"),
                                        this.privateKey,
                                        this.certificate)))
                        .toString());
        return request;
    }

    private static String canonicalizeRequest(HttpRequest request) throws IOException {
        String method = (String) request.getHeaders().get("X-HTTP-Method-Override");

        if (method == null) {
            method = request.getRequestMethod();
        }

        StringBuilder canonicalizedRequest = new StringBuilder(method);

        List<String> ipsHeaderNames = new ArrayList<String>(request.getHeaders().size());

        for (String headerName : request.getHeaders().keySet()) {
            if (headerName.toLowerCase().startsWith("x-ips-")) {
                ipsHeaderNames.add(headerName);
            }
        }

        Collections.sort(ipsHeaderNames);

        for (String headerName : ipsHeaderNames) {
            List<String> values = request.getHeaders().getHeaderStringValues(headerName);

            if (!values.isEmpty()) {
                canonicalizedRequest
                        .append('\n')
                        .append(headerName.toLowerCase())
                        .append(':');

                boolean separate = false;

                for (String value : values) {
                    if (separate) {
                        canonicalizedRequest.append(',');
                    } else {
                        separate = true;
                    }

                    canonicalizedRequest.append(value);
                }
            }
        }

        canonicalizedRequest
                .append('\n')
                .append(request.getUrl().getRawPath());

        List<String> queryParamNames = new ArrayList<String>(request.getUrl().size());
        Collections.sort(queryParamNames);

        for (String paramName : queryParamNames) {
            canonicalizedRequest
                    .append('\n')
                    .append(paramName)
                    .append(':')
                    .append(request.getUrl().get(paramName));
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try {
            request.getContent().writeTo(output);
            if (output.size() > 0) {
                canonicalizedRequest
                        .append('\n')
                        .append(new String(output.toByteArray(), "UTF8"));
            }
        } finally {
            output.close();
        }

        return canonicalizedRequest.toString();
    }

}
