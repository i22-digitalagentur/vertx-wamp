package io.vertx.wamp.util;

import static io.vertx.wamp.util.SessionIdGenerator.MAX_ID;

/**
 * Generates a non-repeating pseudo-random sequence of 53bit numbers.
 * The module works by enumerating the (cyclic) multiplicative group of the
 * galois field GF(2^53).
 * Note: The sequence is NOT SUITABLE for cryptographic applications, because
 * it is easy to guess. Use a block cipher and a counter for a similar sequence
 * with cryptographic strength.
 * The minimal polynomial of our galois field encoded as bit sequence.
 * x^53 + x^52 + x^50 + x^49 + x^48 + x^42 + x^41 + x^40
 * + x^37 + x^36 + x^34 + x^33 + x^32 + x^10 + x^9 + x^8
 * + x^5 + x^4 + x^2 + x + 1
 *
 * All math courtesy of Philipp Lay - https://phlay.de
 */
public class PublicationIdGenerator {
  private final static long minimalPolynomial = 15489056523683639L;
  private final static byte degree = 53;

  /**
   * a primitive root is a field element, such that it's powers run through
   * all non-zero elements of our field.
   * x^52 + x^50 + x^48 + x^45 + x^42 + x^41 + x^40 + x^39
   * + x^38 + x^37 + x^33 + x^31 + x^26 + x^25 + x^21 + x^14
   * + x^9 + x^7 + x^6 + x^4 + x^3 + x + 1
   */

  private final static long primitiveRoot = 5954828377277147L;
  private final long generator;
  private long state = 1;

  public PublicationIdGenerator() {
    this(1L);
  }

  public PublicationIdGenerator(Long seed) {
    final long cleanedSeed = cleanSeed(seed);
    this.generator = power(primitiveRoot, cleanedSeed);
  }

  public synchronized long next() {
    this.state = multiply(state, generator);
    return state;
  }

  private long cleanSeed(Long seed) {
    seed = seed % MAX_ID;
    if (seed % 6361 == 0) {
      seed = (seed + 695257795013021L) % MAX_ID;
    }
    if (seed % 69431 == 0) {
      seed = (seed + 6094917765641302L) % MAX_ID;
    }
    if (seed % 20394401 == 0) {
      seed = (seed + 2217023694086669L) % MAX_ID;
    }
    return seed;
  }

  /**
   * Implement multiplication in galois field GF(2^53) resp. the minimal
   * polynomial given above using only shift and xor
   */
  private long multiply(long a, long b) {
    long result = 0L;
    while (b != 0L) {
      if ((b & 1L) != 0) { // is odd
        result ^= a;
      }
      b >>= 1;
      a <<= 1;
      if ((a & (1L << degree)) != 0) {
        a ^= minimalPolynomial;
      }
    }
    return result;
  }

  /**
   * returns base^n in GF(2,53)
   */
  private long power(long base, long n) {
    long result = 1L;
    while (n > 0) {
      if ((n & 1) != 0) {
        result = multiply(result, base);
      }
      base = multiply(base, base);
      n >>= 1;
    }
    return result;
  }
}
