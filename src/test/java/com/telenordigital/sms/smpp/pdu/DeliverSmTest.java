package com.telenordigital.sms.smpp.pdu;

/*-
 * #%L
 * sms-smpp
 * %%
 * Copyright (C) 2022 Telenor Digital
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import static org.assertj.core.api.Assertions.assertThat;

import com.telenordigital.sms.smpp.charset.GsmCharset;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

public class DeliverSmTest extends PduTest {
  private DeliverSm deserialize(final String encoded, final Charset defaultCharset) {
    final byte[] bytes = ByteBufUtil.decodeHexDump(encoded);

    final var buf = Unpooled.copiedBuffer(bytes);
    // Simulate netty reading the command length and command id
    buf.readBytes(8);

    return DeliverSm.deserialize(buf, defaultCharset);
  }

  @Test
  public void testMoDeserialization() {
    final var encoded =
        "000000400000000500000000000000030002013837363534333231000409343034303400000000000000000000084024232125262F3A000E0001010006000101";

    final var pdu = deserialize(encoded, GsmCharset.GSM);

    assertThat(pdu.sequenceNumber()).isEqualTo(3);
    assertThat(pdu.sender().ton()).isEqualTo(PduConstants.TON_NATIONAL);
    assertThat(pdu.sender().npi()).isEqualTo(PduConstants.NPI_E164);
    assertThat(pdu.sender().address()).isEqualTo("87654321");
    assertThat(pdu.destination().ton()).isEqualTo(PduConstants.TON_SUBSCRIBER);
    assertThat(pdu.destination().npi()).isEqualTo(PduConstants.NPI_PRIVATE);
    assertThat(pdu.destination().address()).isEqualTo("40404");
    assertThat(ByteBufUtil.hexDump(pdu.encodedShortMessage().array()))
        .isEqualTo("4024232125262f3a");
    assertThat(pdu.message()).isEqualTo("¡¤#!%&/:");
  }

  @Test
  public void testCarrierCode() {
    final var encoded =
        "00000117000000050000000000000001000101343637303232373833313600050154656c656e6f72204944000400000000000000007a69643a31383331353031363033207375623a30303120646c7672643a303031207375626d697420646174653a3232313132323132343520646f6e6520646174653a3232313132323132343520737461743a44454c49565244206572723a30303020746578743a36383133343220697320796f757220766572696602030001a002020007a0323430303130186000020001186600113935303230353031343231333030342b00186700113237303731383232333930313030382b00186500113636303731363131353031323030342b000427000102001e0009364432413746323300";
    final var pdu = deserialize(encoded, GsmCharset.GSM);

    assertThat(pdu.mcc()).isEqualTo(240);
    assertThat(pdu.mnc()).isEqualTo(10);
  }

  @Test
  public void testErrorCodeFormatting() {
    final var encoded =
        "0000010c000000050000000000008ac7000101393539373730373134333635000501466f7274756d6f000400000000000000007a69643a31323039353233303537207375623a30303120646c7672643a303030207375626d697420646174653a3231303832353134313320646f6e6520646174653a3231303833303134313320737461743a45585049524544206572723a30333220746578743a596f75207075726368617365642058466967687418600002007e186600113239313232303038333831373030342b00186700113537313030373230343134353030382b00186500113132313033303031353235363030342b00042700010304230003030020001e0009343831374442373100";

    final var pdu = deserialize(encoded, GsmCharset.GSM);

    assertThat(pdu.isDeliveryReceipt()).isTrue();
    assertThat(pdu.networkCode()).isEqualTo("0x030020");
  }

  @Test
  public void charset() {
    final var encoded =
        "0000006f000000050000000000002150000101393233343930303634373633000101353731360000000000000000040028464f5220434247414d4520535a46582c6670692c3431303036303734313531373638392c3830353602020007a034313030363002030007a0313031313034";

    final var pdu = deserialize(encoded, GsmCharset.GSM);

    assertThat(pdu.isDeliveryReceipt()).isFalse();
    assertThat(pdu.message()).isEqualTo("FOR CBGAME SZFX,fpi,410060741517689,8056");
    assertThat(pdu.sender().address()).isEqualTo("923490064763");
    assertThat(pdu.dataCoding()).isEqualTo(PduConstants.DATA_CODING_UNSPECIFIED);
  }

  @Test
  public void testDenmark() {
    final var encoded =
        "0000006f0000000500000000000016fb0001013437393731303632313100010134373539343436353331000000000000000000003050415954455354206d49643d6172676f2d70726f64207249643d72756d4d72497971544e2d65616167715f536c617541000e0001010006000101";

    final var pdu = deserialize(encoded, StandardCharsets.ISO_8859_1);

    assertThat(pdu.destination().address()).isEqualTo("4759446531");
    assertThat(pdu.sender().address()).isEqualTo("4797106211");
    assertThat(pdu.message()).isEqualTo("PAYTEST mId=argo-prod rId=rumMrIyqTN-eaagq_SlauA");
  }

  @Test
  public void testSweden() {
    final var encoded =
        "000000ac0000000500000000131d16650001013437393138343638323600050154656c656e6f72000400000000000000007a69643a30333138373934343333207375623a30303020646c7672643a303030207375626d697420646174653a3231303633303134333920646f6e6520646174653a3231303633303134333920737461743a44454c49565244206572723a30303020746578743a746573742d6d6573736167652073776564656e00";

    final var pdu = deserialize(encoded, GsmCharset.GSM);

    assertThat(pdu.destination().address()).isEqualTo("Telenor");
    assertThat(pdu.sender().address()).isEqualTo("4791846826");
    assertThat(pdu.receiptedMsgId()).isEqualTo("13006AC1");
    assertThat(pdu.state()).isEqualTo((byte) 2);
  }

  @Test
  public void testRejected() {
    final var encoded =
        "0000010B00000005000000000000001D000101383830313732303435333138310005004750204944000400000000000000007A69643A31373434373139353331207375623A30303120646C7672643A303030207375626D697420646174653A3231303632323134313020646F6E6520646174653A3231303632363038313020737461743A52454A45435444206572723A31323520746578743A3136313020697320796F75722076657269666963186000020062186600113130303432313132353730303030382B00186700113236303231393137303431323030342B00186500113136303831323033313934373030342B0004270001080423000308007D001E0009363746453445414200";

    final var pdu = deserialize(encoded, GsmCharset.GSM);

    assertThat(pdu.destination().address()).isEqualTo("GP ID");
    assertThat(pdu.sender().address()).isEqualTo("8801720453181");
    assertThat(pdu.receiptedMsgId()).isEqualTo("67FE4EAB");
  }

  @Test
  public void testDeliveryReceiptDeserialization() {
    final var encoded =
        "000000BA00000005000000000000000200010134343935313336313932300001013430343034000400000000000000006E69643A30303539313133393738207375623A30303120646C7672643A303031207375626D697420646174653A3130303231303137333020646F6E6520646174653A3130303231303137333120737461743A44454C49565244206572723A30303020746578743A4024232125262F3A000E0001010006000101001E000833383630316661000427000102";

    final var pdu = deserialize(encoded, GsmCharset.GSM);

    assertThat(pdu.sequenceNumber()).isEqualTo(2);
    assertThat(pdu.sender().ton()).isEqualTo(PduConstants.TON_INTERNATIONAL);
    assertThat(pdu.sender().npi()).isEqualTo(PduConstants.NPI_E164);
    assertThat(pdu.sender().address()).isEqualTo("44951361920");
    assertThat(pdu.destination().ton()).isEqualTo(PduConstants.TON_INTERNATIONAL);
    assertThat(pdu.destination().npi()).isEqualTo(PduConstants.NPI_E164);
    assertThat(pdu.destination().address()).isEqualTo("40404");
    assertThat(pdu.esmClass()).isEqualTo((byte) 0x04);
    assertThat(pdu.isDeliveryReceipt()).isTrue();
    assertThat(ByteBufUtil.hexDump(pdu.encodedShortMessage().array()))
        .isEqualTo(
            "69643a30303539313133393738207375623a30303120646c7672643a303031207375626d697420646174653a3130303231303137333020646f6e6520646174653a3130303231303137333120737461743a44454c49565244206572723a30303020746578743a4024232125262f3a");
    assertThat(pdu.message())
        .isEqualTo(
            "id:0059113978 sub:001 dlvrd:001 submit date:1002101730 done date:1002101731 stat:DELIVRD err:000 text:¡¤#!%&/:");
    assertThat(pdu.receiptedMsgId()).isEqualTo("38601fa");
    assertThat(pdu.state()).isEqualTo((byte) 2);
  }
}
