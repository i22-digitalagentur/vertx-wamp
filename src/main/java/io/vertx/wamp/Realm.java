package io.vertx.wamp;

import io.vertx.wamp.messages.EventMessage;
import io.vertx.wamp.messages.PublishMessage;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

// the realm manages all subscriptions and publications in it
public class Realm {
  private final Uri uri;

  private final List<Subscription> subscriptions;
  private final Random sessionIdGenerator = new Random();

  public Realm(Uri uri) {
    this.subscriptions = new ArrayList<>();
    this.uri = uri;
  }

  public void publishMessage(PublishMessage msg) {
    getSubscriptions(msg.getTopic()).forEach(subscription -> {
      subscription.consumer.accept(MessageFactory.createEvent(subscription.id,
          msg.getId(),
          msg.getOptions(),
          msg.getArguments(),
          msg.getArgumentsKw()));
    });
  }

  public void publishMessage(long id,
                             Uri topic,
                             Map<String, Object> options,
                             List<Object> arguments,
                             Map<String, Object> argumentsKw) {
    getSubscriptions(topic).forEach(subscription -> {
      subscription.consumer.accept(MessageFactory.createEvent(
          subscription.id,
          id,
          options,
          arguments,
          argumentsKw));
    });
  }

  public Uri getUri() {
    return uri;
  }

  private Stream<Subscription> getSubscriptions(Uri pattern) {
    // pattern matching is part of the advanced profile only
    return subscriptions.parallelStream().filter(subscription -> subscription.topic.equals(pattern));
  }

  public synchronized long addSubscription(Consumer<EventMessage> recipient, Uri topic) {
    long subscriptionId = generateSubscriptionId();
    this.subscriptions.add(new Subscription(recipient,
        subscriptionId,
        topic));
    return subscriptionId;
  }

  public synchronized void removeSubscription(long subscriptionId) {
    Iterator<Subscription> it = this.subscriptions.iterator();
    while (it.hasNext()) {
      Subscription s = it.next();
      if (s.id == subscriptionId) {
        it.remove();
        return;
      }
    }
  }

  private long generateSubscriptionId() {
    // as stupid & simple as possible for now
    while (true) {
      long retVal = this.sessionIdGenerator.nextInt();
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

  static class Subscription {
    final Uri topic;
    final long id;
    final Consumer<EventMessage> consumer;

    Subscription(Consumer<EventMessage> consumer, long id, Uri topic) {
      this.consumer = consumer;
      this.id = id;
      this.topic = topic;
    }
  }
}
