package io.vertx.wamp;

import io.vertx.wamp.messages.EventMessage;
import io.vertx.wamp.messages.PublishMessage;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// the realm manages all subscriptions and publications in it
public class Realm {
    private final Uri uri;

    private final List<Subscription> subscriptions = new ArrayList<>();
    private final Random sessionIdGenerator = new Random();

    public Realm(Uri uri) {
        this.uri = uri;
    }

    public void publishMessage(PublishMessage msg) {
        getSubscriptions(msg.getTopic()).forEach(subscription ->
                deliverEventMessage(subscription,
                        MessageFactory.createEvent(subscription.id,
                                msg.getId(),
                                msg.getOptions(),
                                msg.getArguments(),
                                msg.getArgumentsKw())));
    }

    public void publishMessage(long id,
                               Uri topic,
                               Map<String, Object> options,
                               List<Object> arguments,
                               Map<String, Object> argumentsKw) {
        getSubscriptions(topic).forEach(subscription -> {
            deliverEventMessage(subscription,
                    MessageFactory.createEvent(
                            subscription.id,
                            id,
                            options,
                            arguments,
                            argumentsKw));
        });
    }

    private void deliverEventMessage(Subscription subscription, EventMessage message) {
        if (isPublishAuthorized(subscription, message)) {
            subscription.consumer.sendMessage(message);
        }
    }

    private boolean isPublishAuthorized(Subscription subscription, EventMessage message) {
        SecurityPolicy.ClientInfo clientInfo = subscription.consumer.getClientInfo();
        return clientInfo == null || clientInfo.getPolicy().authorizeEvent(clientInfo, subscription.topic, message);
    }

    public Uri getUri() {
        return uri;
    }

    private synchronized Collection<Subscription> getSubscriptions(Uri pattern) {
        // a stream will raise an exception if the underlying data is changed while
        // it's being used so make a temporary copy
       return subscriptions.parallelStream()
                           // pattern matching is part of the advanced profile only
                           .filter(subscription -> subscription.topic.equals(pattern))
                           .collect(Collectors.toUnmodifiableList());
    }

    public synchronized long addSubscription(WampSession session, Uri topic) {
        long subscriptionId = generateSubscriptionId();
        this.subscriptions.add(new Subscription(session,
                subscriptionId,
                topic));
        return subscriptionId;
    }

    public synchronized void removeSession(WampSession session) {
        this.subscriptions.removeIf(s -> s.consumer == session);
    }

    // pass in the session so that adversarial or buggy clients can't unsubscribe s/o else
    public synchronized void removeSubscription(WampSession session, long subscriptionId) {
        Iterator<Subscription> it = this.subscriptions.iterator();
        while (it.hasNext()) {
            Subscription s = it.next();
            if (s.id == subscriptionId && s.consumer == session) {
                it.remove();
                return;
            }
        }
    }

    private long generateSubscriptionId() {
        // as stupid & simple as possible for now
        while (true) {
            long retVal = this.sessionIdGenerator.nextInt(Integer.MAX_VALUE);
            if (subscriptions.stream().noneMatch(
                    subscription -> subscription.id == retVal)) {
                return retVal;
            }
        }
    }

    @Override
    public boolean equals(Object other) {
        return (other instanceof Realm && ((Realm) other).uri.equals(uri));
    }

    @Override
    public int hashCode() {
        return uri.hashCode();
    }

    @Override
    public String toString() {
        return uri.toString();
    }

    static class Subscription {
        final Uri topic;
        final long id;
        final WampSession consumer;

        Subscription(WampSession consumer, long id, Uri topic) {
            this.consumer = consumer;
            this.id = id;
            this.topic = topic;
        }

        @Override
        public boolean equals(Object obj) {
            return (obj instanceof Subscription && ((Subscription) obj).id == id);
        }
    }
}
