package io.vertx.wamp;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.wamp.messages.EventMessage;
import io.vertx.wamp.messages.PublishMessage;
import io.vertx.wamp.util.NonDuplicateRandomIdGenerator;
import io.vertx.wamp.util.RandomIdGenerator;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

// the realm manages all subscriptions and publications in it
public class Realm {

  private final Uri uri;

  private final List<Subscription> subscriptions = new ArrayList<>();
  private final List<Subscription> registrations = new ArrayList<>();
  private final NonDuplicateRandomIdGenerator subscriptionIdGenerator = new NonDuplicateRandomIdGenerator();
  private final RandomIdGenerator publicationIdGenerator = new RandomIdGenerator();

  public Realm(Uri uri) {
    this.uri = uri;
  }

  public Future<Long> publishMessage(PublishMessage msg) {
    return publishMessage(msg.getTopic(), msg.getOptions(), msg.getArguments(),
        msg.getArgumentsKw());
  }

  public Future<Long> publishMessage(Uri topic,
                                     Map<String, Object> options,
                                     List<Object> arguments,
                                     Map<String, Object> argumentsKw) {
    final long publicationId = publicationIdGenerator.next();
    List<Future> publishFutures = getSubscriptions(topic).parallelStream()
                                                         .map(subscription -> deliverEventMessage(subscription,
                                                             MessageFactory.createEvent(
                                                                 subscription.id,
                                                                 publicationId,
                                                                 options,
                                                                 arguments,
                                                                 argumentsKw))).collect(Collectors.toList());
    return CompositeFuture.all(publishFutures).map(publicationId);
  }

  public Future<AbstractMap.SimpleImmutableEntry<List<Object>, Map<String, Object>>> callProcedure(Uri procedure,
                                                                                                   List<Object> arguments,
                                                                                                   Map<String, Object> argumentsKw) {
    return Future.future(promise -> {
      final Optional<Subscription> registrationResult = registrations.stream().filter((r) -> r.topic.equals(procedure)).findFirst();
      if (registrationResult.isEmpty()) {
        promise.fail(new NoSuchElementException());
        return;
      }
      final long invocationId = publicationIdGenerator.next();
      final Subscription registration = registrationResult.get();
      registration.consumer.invokeRegistration(MessageFactory.createInvocationMessage(invocationId,
          registration.id, arguments, argumentsKw))
                           .onComplete((result) -> {
                             if (result.failed()) {
                               promise.fail(result.cause());
                             } else {
                               promise.complete(result.result());
                             }
                           });
    });

  }

  private Future<Void> deliverEventMessage(Subscription subscription, EventMessage message) {
    if (isPublishAuthorized(subscription, message)) {
      return subscription.consumer.sendMessage(message);
    } else {
      return Future.failedFuture("Unauthorized to publish this message");
    }
  }

  private boolean isPublishAuthorized(Subscription subscription, EventMessage message) {
    final SecurityPolicy.ClientInfo clientInfo = subscription.consumer.getClientInfo();
    return clientInfo == null || clientInfo.getPolicy()
                                           .authorizeEvent(clientInfo, subscription.topic, message);
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
    final long subscriptionId = subscriptionIdGenerator.next();
    this.subscriptions.add(new Subscription(session,
        subscriptionId,
        topic));
    return subscriptionId;
  }

  public synchronized long addRegistration(WampSession session, Uri topic) {
    if (this.registrations.parallelStream()
                          .anyMatch((subscription) -> subscription.topic.equals(topic))) {
      return -1;
    }
    // routers are free to choose a generation strategy - let's re-use the same sequence as for subscriptions
    final long registrationId = subscriptionIdGenerator.next();
    // but still store them separately
    this.registrations.add(new Subscription(session,
        registrationId,
        topic));
    return registrationId;
  }

  public synchronized void removeSession(WampSession session) {
    Predicate<Subscription> checkId = (Subscription s) -> {
      if (s.consumer == session) {
        subscriptionIdGenerator.release(s.id);
        return true;
      } else {
        return false;
      }
    };
    this.subscriptions.removeIf(checkId);
    this.registrations.removeIf(checkId);
  }

  private boolean removeEntry(List<Subscription> list, WampSession session, long entryId) {
    final Iterator<Subscription> it = list.iterator();
    while (it.hasNext()) {
      final Subscription s = it.next();
      if (s.id == entryId && s.consumer == session) {
        it.remove();
        return true;
      }
    }
    return false;
  }

  // pass in the session so that adversarial or buggy clients can't unsubscribe s/o else
  public synchronized boolean removeSubscription(WampSession session, long subscriptionId) {
    return removeEntry(subscriptions, session, subscriptionId);
  }

  public synchronized boolean removeRegistration(WampSession session, long registrationId) {
    return removeEntry(registrations, session, registrationId);
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

  // TODO: rename this - it's re-used for both subscription and registration
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

    @Override
    public int hashCode() {
      return Objects.hash(id);
    }
  }
}
