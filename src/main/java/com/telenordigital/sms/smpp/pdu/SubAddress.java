package com.telenordigital.sms.smpp.pdu;

/**
 * For source_subaddress or dest_subaddress in deliver_sm
 *
 * @param mcc Mobile country code
 * @param mnc Mobile network code
 */
public record SubAddress(int mcc, int mnc) {}
