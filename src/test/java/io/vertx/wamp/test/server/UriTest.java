package io.vertx.wamp.test.server;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.vertx.wamp.Uri;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UriTest {

  @Test
  @DisplayName("It accepts valid URIs as constructor argument")
  void testConstructorValidUri() {
    assertDoesNotThrow(() -> {
      new Uri("valid.uri123");
    });
  }

  @Test
  @DisplayName("It rejects URIs with spaces")
  void testUriWithSpaceError() {
    assertThrows(Uri.InvalidUriException.class, () -> {
      new Uri("foo bar");
    });
  }
}
