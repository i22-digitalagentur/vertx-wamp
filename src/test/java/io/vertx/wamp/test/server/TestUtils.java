package io.vertx.wamp.test.server;

import io.vertx.wamp.SecurityPolicy;
import org.mockito.Mockito;

public class TestUtils {
  public static SecurityPolicy.ClientInfo buildMockClientInfo() {
    SecurityPolicy.ClientInfo clientInfo = Mockito.mock(SecurityPolicy.ClientInfo.class);
    SecurityPolicy securityPolicy = Mockito.spy(Mockito.mock(SecurityPolicy.class));
    Mockito.when(clientInfo.getPolicy()).thenReturn(securityPolicy);
    return clientInfo;
  }
}
