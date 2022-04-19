package com.telenordigital.sms.smpp.pdu;

public interface RequestPdu<R extends ResponsePdu> extends Pdu {
  default R createResponse() {
    return this.createResponse(0);
  }

  default R createGenericFailureResponse() {
    // ESME_RSYSERR (System Error)
    return this.createResponse(0x08);
  }

  default R createInvalidTlvResponse() {
    // ESME_RINVOPTPARAMVAL
    return this.createResponse(0xc4);
  }

  R createResponse(int commandStatus);
}
