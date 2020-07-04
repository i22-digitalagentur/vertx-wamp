package io.vertx.wamp;

/*
 * Properly typed URI
 */
public class Uri {
  public final static Uri INVALID_URI = new Uri("wamp.error.invalid_uri");
  public final static Uri NO_SUCH_PROCEDURE = new Uri("wamp.error.no_such_procedure");
  public final static Uri PROCEDURE_ALREADY_EXISTS = new Uri("wamp.error.procedure_already_exists");
  public final static Uri NO_SUCH_REGISTRATION = new Uri("wamp.error.no_such_registration");
  public final static Uri NO_SUCH_SUBSCRIPTION = new Uri("wamp.error.no_such_subscription");
  public final static Uri INVALID_ARGUMENT = new Uri("wamp.error.invalid_argument");
  public final static Uri SYSTEM_SHUTDOWN = new Uri("wamp.close.system_shutdown");
  public final static Uri CLOSE_REALM = new Uri("wamp.close.close_realm");
  public final static Uri GOODBYE_AND_OUT = new Uri("wamp.close.goodbye_and_out");
  public final static Uri PROTOCOL_VIOLATION = new Uri("wamp.error.protocol_violation");
  public final static Uri NOT_AUTHORIZED = new Uri("wamp.error.not_authorized");
  public final static Uri AUTHORIZATION_FAILED = new Uri("wamp.error.authorization_failed");
  public final static Uri NO_SUCH_REALM = new Uri("wamp.error.no_such_realm");
  public final static Uri NO_SUCH_ROLE = new Uri("wamp.error.no_such_role");
  private final String uri;

  public Uri(String string) {
    // advanced profile sometimes allows empty URI components, out of scope for now
    if (!string.matches("^([^\\s\\.#]+\\.)*([^\\s\\.#]+)$")) {
      throw new InvalidUriException(string);
    }
    this.uri = string;
  }

  @Override
  public String toString() {
    return this.uri;
  }

  @Override
  public int hashCode() {
    return uri.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Uri)) {
      return false;
    }
    return uri.equals(((Uri) obj).uri);
  }

  class InvalidUriException extends IllegalArgumentException {
    InvalidUriException(String uri) {
      super(uri + "is not a valid URI");
    }
  }
}
