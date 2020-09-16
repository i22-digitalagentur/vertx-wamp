/*
 * Copyright 2020 i22 Digitalagentur GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package examples;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.docgen.Source;
import io.vertx.wamp.Realm;
import io.vertx.wamp.Uri;
import io.vertx.wamp.WAMPWebsocketServer;

import java.util.List;
import java.util.Map;

@Source
@SuppressWarnings("java:S3740")
public class VertxWAMPWebsocketServerExamples {

  /**
   * Example for accepting client connections and sending events
   *
   * @param vertx
   */
  public void example1(Vertx vertx) {
    WAMPWebsocketServer wampServer = WAMPWebsocketServer.create(vertx);
    Realm realm = new Realm(new Uri("example.realm"));
    wampServer.addRealm(realm).listen(8080).onComplete(res -> {
      if (res.succeeded()) {
        res.result().getRealms().get(0).publishMessage(1,
            new Uri("de.i22.topic"),
            Map.of(),
            List.of(),
            Map.of());
      }
    });
  }

  /**
   * Example for shutting down the server
   *
   * @param vertx
   */
  public void example2(Vertx vertx) {
    WAMPWebsocketServer wampServer = WAMPWebsocketServer.create(vertx);
    Realm realm = new Realm(new Uri("example.realm"));
    wampServer.addRealm(realm).listen(8080).onComplete(res -> {
      if (res.succeeded()) {
        Promise promise = Promise.promise();
        promise.future().onComplete(result ->
            LoggerFactory.getLogger(this.getClass().getName()).info("WAMP server started and shut down"));
        res.result().close(promise);
      }
    });
  }

}
