/*
 * Copyright 2015 ALTA Software.
 */
package com.ips.api.client;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Base64;
import com.google.api.client.util.Key;
import com.google.api.client.util.DateTime;


import java.math.BigDecimal;
import java.security.PrivateKey;

/**
 *  IPS Message
 */
public final class Message extends GenericJson {

    @Key
    private Integer id;

    @Key
    private String ref;

    @Key
    private String sender;

    @Key
    private String receiver;

    @Key
    private Short type;

    @Key
    private DateTime date;

    @Key
    private BigDecimal amount;

    @Key
    private String ccy;

    @Key
    private Byte priority;

    @Key
    private String state;

    @Key
    private DateTime createdAt;

    @Key
    private DateTime lastModifiedAt;

    @Key
    private Integer batchId;

    @Key
    private String content;

    public final Integer getId() {
        return id;
    }

    public final void setId(Integer id) {
        this.id = id;
    }

    public final String getRef() {
        return ref;
    }

    public final void setRef(String ref) {
        this.ref = ref;
    }

    public final String getSender() {
        return sender;
    }

    public final void setSender(String sender) {
        this.sender = sender;
    }

    public final String getReceiver() {
        return receiver;
    }

    public final void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public final Short getType() {
        return type;
    }

    public final void setType(Short type) {
        this.type = type;
    }

    public final DateTime getDate() {
        return date;
    }

    public final void setDate(DateTime date) {
        this.date = date;
    }

    public final BigDecimal getAmount() {
        return amount;
    }

    public final void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public final String getCcy() {
        return ccy;
    }

    public final void setCcy(String ccy) {
        this.ccy = ccy;
    }

    public final Byte getPriority() {
        return priority;
    }

    public final void setPriority(Byte priority) {
        this.priority = priority;
    }

    public final String getState() {
        return state;
    }

    public final void setState(String state) {
        this.state = state;
    }

    public final DateTime getCreatedAt() {
        return createdAt;
    }

    public final void setCreatedAt(DateTime createdAt) {
        this.createdAt = createdAt;
    }

    public final DateTime getLastModifiedAt() {
        return lastModifiedAt;
    }

    public final void setLastModifiedAt(DateTime lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
    }

    public final Integer getBatchId() {
        return batchId;
    }

    public final void setBatchId(Integer batchId) {
        this.batchId = batchId;
    }

    public final String getContent() {
        return content;
    }

    public final void setContent(String content) {
        this.content = content;
    }

    public final byte[] getContentBytes() {
        return Base64.decodeBase64(this.content);
    }

    public Message withId(Integer value) {
        this.id = value;
        return this;
    }

    public Message withRef(String value) {
        this.ref = value;
        return this;
    }

    public Message withSender(String value) {
        this.sender = value;
        return this;
    }

    public Message withReceiver(String value) {
        this.receiver = value;
        return this;
    }

    public Message withType(Short value) {
        this.type = value;
        return this;
    }

    public Message withDate(DateTime value) {
        this.date = value;
        return this;
    }

    public Message withAmount(BigDecimal value) {
        this.amount = value;
        return this;
    }

    public Message withCcy(String value) {
        this.ccy = value;
        return this;
    }

    public Message withContentBytes(byte[] value) {
        this.content = Base64.encodeBase64String(value);
        return this;
    }

    public String decryptContent(PrivateKey privateKey) throws Exception{
        return new String(CryptoUtils.decryptCms(this.getContentBytes(), privateKey), "UTF8");
    }
}
