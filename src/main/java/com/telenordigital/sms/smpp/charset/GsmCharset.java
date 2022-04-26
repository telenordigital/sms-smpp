/*
Copyright (c) 2005, OpenSmpp Project
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

 *  Redistributions of source code must retain the above copyright notice,
    this list of conditions and the following disclaimer.

 *  Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

 *  Neither the name of the OpenSmpp Project nor the names of its contributors
    may be used to endorse or promote products derived from this software
    without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

This software was originally issued under the Logica Open Source License Version 1.0,
but was subsequently put in the public domain under the current BSD licence, which was
deemed closest to the spirit of the original licence.
 */
package com.telenordigital.sms.smpp.charset;

/*
 * %%Ignore-License
 */

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.util.HashMap;

@SuppressWarnings({"java:S3776", "java:S135"})
public class GsmCharset extends Charset {
  public static final String CHARSET_NAME = "X-Gsm7Bit";
  public static final GsmCharset GSM = new GsmCharset();

  // HashMap's used for encoding and decoding
  private static final HashMap<Character, Byte> defaultEncodeMap = new HashMap<>();
  private static final HashMap<Byte, Character> defaultDecodeMap = new HashMap<>();
  private static final HashMap<Character, Byte> extEncodeMap = new HashMap<>();
  private static final HashMap<Byte, Character> extDecodeMap = new HashMap<>();

  // Data to populate the hashmaps with
  private static final Object[][] gsmCharacters = {
    {'@', (byte) 0x00},
    {'£', (byte) 0x01},
    {'$', (byte) 0x02},
    {'¥', (byte) 0x03},
    {'è', (byte) 0x04},
    {'é', (byte) 0x05},
    {'ù', (byte) 0x06},
    {'ì', (byte) 0x07},
    {'ò', (byte) 0x08},
    {'Ç', (byte) 0x09},
    {'\n', (byte) 0x0a},
    {'Ø', (byte) 0x0b},
    {'ø', (byte) 0x0c},
    {'\r', (byte) 0x0d},
    {'Å', (byte) 0x0e},
    {'å', (byte) 0x0f},
    {'\u0394', (byte) 0x10},
    {'_', (byte) 0x11},
    {'\u03A6', (byte) 0x12},
    {'\u0393', (byte) 0x13},
    {'\u039B', (byte) 0x14},
    {'\u03A9', (byte) 0x15},
    {'\u03A0', (byte) 0x16},
    {'\u03A8', (byte) 0x17},
    {'\u03A3', (byte) 0x18},
    {'\u0398', (byte) 0x19},
    {'\u039E', (byte) 0x1a},
    {'\u001B', (byte) 0x1b}, // 27 is Escape character
    {'Æ', (byte) 0x1c},
    {'æ', (byte) 0x1d},
    {'ß', (byte) 0x1e},
    {'É', (byte) 0x1f},
    {'\u0020', (byte) 0x20},
    {'!', (byte) 0x21},
    {'\'', (byte) 0x22},
    {'#', (byte) 0x23},
    {'¤', (byte) 0x24},
    {'%', (byte) 0x25},
    {'&', (byte) 0x26},
    {'\'', (byte) 0x27},
    {'(', (byte) 0x28},
    {')', (byte) 0x29},
    {'*', (byte) 0x2a},
    {'+', (byte) 0x2b},
    {',', (byte) 0x2c},
    {'-', (byte) 0x2d},
    {'.', (byte) 0x2e},
    {'/', (byte) 0x2f},
    {'0', (byte) 0x30},
    {'1', (byte) 0x31},
    {'2', (byte) 0x32},
    {'3', (byte) 0x33},
    {'4', (byte) 0x34},
    {'5', (byte) 0x35},
    {'6', (byte) 0x36},
    {'7', (byte) 0x37},
    {'8', (byte) 0x38},
    {'9', (byte) 0x39},
    {':', (byte) 0x3a},
    {';', (byte) 0x3b},
    {'<', (byte) 0x3c},
    {'=', (byte) 0x3d},
    {'>', (byte) 0x3e},
    {'?', (byte) 0x3f},
    {'¡', (byte) 0x40},
    {'A', (byte) 0x41},
    {'B', (byte) 0x42},
    {'C', (byte) 0x43},
    {'D', (byte) 0x44},
    {'E', (byte) 0x45},
    {'F', (byte) 0x46},
    {'G', (byte) 0x47},
    {'H', (byte) 0x48},
    {'I', (byte) 0x49},
    {'J', (byte) 0x4a},
    {'K', (byte) 0x4b},
    {'L', (byte) 0x4c},
    {'M', (byte) 0x4d},
    {'N', (byte) 0x4e},
    {'O', (byte) 0x4f},
    {'P', (byte) 0x50},
    {'Q', (byte) 0x51},
    {'R', (byte) 0x52},
    {'S', (byte) 0x53},
    {'T', (byte) 0x54},
    {'U', (byte) 0x55},
    {'V', (byte) 0x56},
    {'W', (byte) 0x57},
    {'X', (byte) 0x58},
    {'Y', (byte) 0x59},
    {'Z', (byte) 0x5a},
    {'Ä', (byte) 0x5b},
    {'Ö', (byte) 0x5c},
    {'Ñ', (byte) 0x5d},
    {'Ü', (byte) 0x5e},
    {'§', (byte) 0x5f},
    {'¿', (byte) 0x60},
    {'a', (byte) 0x61},
    {'b', (byte) 0x62},
    {'c', (byte) 0x63},
    {'d', (byte) 0x64},
    {'e', (byte) 0x65},
    {'f', (byte) 0x66},
    {'g', (byte) 0x67},
    {'h', (byte) 0x68},
    {'i', (byte) 0x69},
    {'j', (byte) 0x6a},
    {'k', (byte) 0x6b},
    {'l', (byte) 0x6c},
    {'m', (byte) 0x6d},
    {'n', (byte) 0x6e},
    {'o', (byte) 0x6f},
    {'p', (byte) 0x70},
    {'q', (byte) 0x71},
    {'r', (byte) 0x72},
    {'s', (byte) 0x73},
    {'t', (byte) 0x74},
    {'u', (byte) 0x75},
    {'v', (byte) 0x76},
    {'w', (byte) 0x77},
    {'x', (byte) 0x78},
    {'y', (byte) 0x79},
    {'z', (byte) 0x7a},
    {'ä', (byte) 0x7b},
    {'ö', (byte) 0x7c},
    {'ñ', (byte) 0x7d},
    {'ü', (byte) 0x7e},
    {'à', (byte) 0x7f}
  };

  private static final Object[][] gsmExtensionCharacters = {
    {'\n', (byte) 0x0a},
    {'^', (byte) 0x14},
    {' ', (byte) 0x1b}, // reserved for future extensions
    {'{', (byte) 0x28},
    {'}', (byte) 0x29},
    {'\\', (byte) 0x2f},
    {'[', (byte) 0x3c},
    {'~', (byte) 0x3d},
    {']', (byte) 0x3e},
    {'|', (byte) 0x40},
    {'€', (byte) 0x65}
  };

  // static section that populates the encode and decode HashMap objects
  static {
    // default alphabet
    for (final Object[] map : gsmCharacters) {
      defaultEncodeMap.put((Character) map[0], (Byte) map[1]);
      defaultDecodeMap.put((Byte) map[1], (Character) map[0]);
    }

    // extended alphabet
    for (final Object[] map : gsmExtensionCharacters) {
      extEncodeMap.put((Character) map[0], (Byte) map[1]);
      extDecodeMap.put((Byte) map[1], (Character) map[0]);
    }
  }

  public boolean canRepresent(final String string) {
    for (int i = 0; i < string.length(); i++) {
      final var c = string.charAt(i);
      if (!defaultEncodeMap.containsKey(c)) {
        return false;
      }
    }
    return true;
  }

  public GsmCharset() {
    this(CHARSET_NAME, null);
  }

  /**
   * Constructor for the Gsm7Bit charset. Call the superclass constructor to pass along the name(s)
   * we'll be known by. Then save a reference to the delegate Charset.
   */
  protected GsmCharset(final String canonical, final String[] aliases) {
    super(canonical, aliases);
  }

  // ----------------------------------------------------------

  /**
   * Called by users of this Charset to obtain an encoder. This implementation instantiates an
   * instance of a private class (defined below) and passes it an encoder from the base Charset.
   */
  public CharsetEncoder newEncoder() {
    return new GsmEncoder(this);
  }

  /**
   * Called by users of this Charset to obtain a decoder. This implementation instantiates an
   * instance of a private class (defined below) and passes it a decoder from the base Charset.
   */
  public CharsetDecoder newDecoder() {
    return new GsmDecoder(this);
  }

  /** This method must be implemented by concrete Charsets. We always say no, which is safe. */
  public boolean contains(final Charset cs) {
    return false;
  }

  /**
   * The encoder implementation for the Gsm7Bit Charset. This class, and the matching decoder class
   * below, should also override the "impl" methods, such as implOnMalformedInput() and make
   * passthrough calls to the baseEncoder object. That is left as an exercise for the hacker.
   */
  private static class GsmEncoder extends CharsetEncoder {

    /**
     * Constructor, call the superclass constructor with the Charset object and the encodings sizes
     * from the delegate encoder.
     */
    GsmEncoder(final Charset cs) {
      super(cs, 1, 2);
    }

    /** Implementation of the encoding loop. */
    protected CoderResult encodeLoop(final CharBuffer cb, final ByteBuffer bb) {
      CoderResult cr = CoderResult.UNDERFLOW;

      while (cb.hasRemaining()) {
        if (!bb.hasRemaining()) {
          cr = CoderResult.OVERFLOW;
          break;
        }
        char ch = cb.get();

        // first check the default alphabet
        Byte b = defaultEncodeMap.get(ch);
        if (b == null) {
          // check extended alphabet
          b = extEncodeMap.get(ch);
          if (b != null) {
            // since the extended character set takes two bytes
            // we have to check that there is enough space left
            if (bb.remaining() < 2) {
              // go back one step
              cb.position(cb.position() - 1);
              cr = CoderResult.OVERFLOW;
              break;
            }
            // all ok, add it to the buffer
            bb.put((byte) 0x1b);
          } else {
            // no match found, send a ?
            b = (byte) 0x3F;
          }
        }
        bb.put(b);
      }
      return cr;
    }
  }

  // --------------------------------------------------------

  /** The decoder implementation for the Gsm 7Bit Charset. */
  private static class GsmDecoder extends CharsetDecoder {

    /**
     * Constructor, call the superclass constructor with the Charset object and pass alon the
     * chars/byte values from the delegate decoder.
     */
    GsmDecoder(final Charset cs) {
      super(cs, 1, 1);
    }

    /** Implementation of the decoding loop. */
    protected CoderResult decodeLoop(final ByteBuffer bb, final CharBuffer cb) {
      CoderResult cr = CoderResult.UNDERFLOW;

      while (bb.hasRemaining()) {
        if (!cb.hasRemaining()) {
          cr = CoderResult.OVERFLOW;
          break;
        }
        byte b = bb.get();

        // first check the default alphabet
        Character s = defaultDecodeMap.get(b);
        if (s != null) {
          char ch = s;
          if (ch != '\u001B') {
            cb.put(ch);
          } else {
            // check the extended alphabet
            if (bb.hasRemaining()) {
              b = bb.get();
              s = extDecodeMap.get(b);
              if (s != null) {
                ch = s;
                cb.put(ch);
              } else {
                cb.put('?');
              }
            }
          }
        } else {
          cb.put('?');
        }
      }
      return cr;
    }
  }
}
