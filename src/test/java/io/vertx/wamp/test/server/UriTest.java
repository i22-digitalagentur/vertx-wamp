package io.vertx.wamp.test.server;

import io.vertx.wamp.Uri;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class UriTest {
  @Test
  @DisplayName("It accepts valid URIs as constructor argument")
  public void testConstructorValidUri() {
    new Uri("valid.uri123");
  }

  @Test
  @DisplayName("It rejects URIs with spaces")
  public void testUriWithSpaceError() {
    assertThrows(Uri.InvalidUriException.class, () -> {
      new Uri("foo bar");
    });
  }
}
