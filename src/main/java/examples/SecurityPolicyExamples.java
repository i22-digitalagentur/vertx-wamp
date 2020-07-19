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

import io.vertx.core.Vertx;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.docgen.Source;
import io.vertx.wamp.SecurityPolicy;
import io.vertx.wamp.Uri;
import io.vertx.wamp.WAMPWebsocketServer;

@Source
@SuppressWarnings("java:S3740")
public class SecurityPolicyExamples {
    public void example1(Vertx vertx) {
        WAMPWebsocketServer wampServer = WAMPWebsocketServer.create(vertx);
        wampServer.withSecurityPolicy(new ExampleSecurityPolicy()).listen(8080);
    }

    class ExampleClientInfo implements SecurityPolicy.ClientInfo {
        private final SecurityPolicy owner;

        ExampleClientInfo(SecurityPolicy owner) {
            this.owner = owner;
        }

        @Override
        public SecurityPolicy getPolicy() {
            return owner;
        }
    }

    class AuthorizedClientInfo extends ExampleClientInfo {
        public final int accessLevel;

        AuthorizedClientInfo(SecurityPolicy owner, int accessLevel) {
            super(owner);
            this.accessLevel = accessLevel;
        }
    }

    class ExampleSecurityPolicy implements SecurityPolicy<ExampleClientInfo> {
        @Override
        public ExampleClientInfo authenticateConnection(ServerWebSocket webSocket) {
            if (webSocket.headers().contains("API-Key", "foo", true)) {
                return new AuthorizedClientInfo(this, 1);
            }
            return new ExampleClientInfo(this);
        }

        // suppose there is a "public" realm and one where only special clients may connect
        @Override
        public boolean authorizeHello(ExampleClientInfo client, Uri realm) {
            return client instanceof AuthorizedClientInfo ||
                    realm.toString().equalsIgnoreCase("client1Realm");
        }

        // allow subscriptions to a specific topic only
        @Override
        public boolean authorizeSubscribe(ExampleClientInfo client, Uri realm, Uri subscribePattern) {
            return subscribePattern.toString().equalsIgnoreCase("authorized.topic");
        }

        // guests are not allowed to publish
        @Override
        public boolean authorizePublish(ExampleClientInfo client, Uri realm, Uri topic) {
            return client instanceof AuthorizedClientInfo;
        }
    }
}
