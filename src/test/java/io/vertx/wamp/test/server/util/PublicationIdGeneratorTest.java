package io.vertx.wamp.test.server.util;

import io.vertx.wamp.util.PublicationIdGenerator;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PublicationIdGeneratorTest {
  @Test
  public void testGeneratorWithoutSeed() {
    final PublicationIdGenerator generator = new PublicationIdGenerator();
    assertEquals(5954828377277147L, generator.next());
    assertEquals(5024234331678047L, generator.next());
    assertEquals(1246299574623534L, generator.next());
  }

  @Test
  public void testGeneratorWitSeed() {
    final PublicationIdGenerator generator = new PublicationIdGenerator(987654321L);
    assertEquals(2766166271244974L, generator.next());
    assertEquals(1308432609132356L, generator.next());
    assertEquals(5999195527213519L, generator.next());
  }

  @Test
  public void testMultiply() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    final PublicationIdGenerator generator = new PublicationIdGenerator();
    Method method = PublicationIdGenerator.class.getDeclaredMethod("multiply", long.class, long.class);
    method.setAccessible(true);
    assertEquals(83634854045723L, method.invoke(generator, 83634854045723L, 1));
    assertEquals(0L, method.invoke(generator, 37584595037454L, 0));
    assertEquals(68173825L, method.invoke(generator, 9345L, 9345L));
  }
}
