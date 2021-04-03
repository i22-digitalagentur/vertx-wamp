package io.vertx.wamp.messages;

import java.util.List;

public class PublishedMessage extends AbstractWAMPMessage {

  private final long id;
  private final long publication;

  public PublishedMessage(long requestId, long publication) {
    super(Type.PUBLISHED);
    this.id = requestId;
    this.publication = publication;
  }

  @Override
  public List<Object> getPayload() {
    return List.of(id, publication);
  }
}
