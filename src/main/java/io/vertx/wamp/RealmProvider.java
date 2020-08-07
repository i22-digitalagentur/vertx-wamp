package io.vertx.wamp;

import java.util.List;

public interface RealmProvider {
  List<Realm> getRealms();
}
