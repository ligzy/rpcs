package rcps.aeron.infinispan;

import io.aeron.Aeron;
import io.aeron.FragmentAssembler;
import io.aeron.Publication;
import io.aeron.logbuffer.FragmentHandler;
import io.aeron.logbuffer.Header;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import rcps.aeron.infinispan.codec.EmptyDecoder;
import rcps.aeron.infinispan.codec.KeyEncoder;
import rcps.aeron.infinispan.codec.KeyValueEncoder;
import rcps.aeron.infinispan.codec.MessageHeaderDecoder;
import rcps.aeron.infinispan.codec.MessageHeaderEncoder;
import rcps.aeron.infinispan.codec.ValueDecoder;

import java.nio.charset.Charset;

import static rcps.aeron.AeronUtils.offerResult;
import static rcps.aeron.infinispan.Constants.REQ_CHANNEL;
import static rcps.aeron.infinispan.Constants.STREAM_ID;

public class InfinispanAeronClient {

   private static final int FRAGMENT_LIMIT = 256;
   
//   static final int DEFAULT_RETRY_ATTEMPTS = 3;
//
//   static final IdleStrategy RETRY_IDLE_STRATEGY = new BusySpinIdleStrategy();

   static final MessageHeaderEncoder MSG_HEADER_ENCODER = new MessageHeaderEncoder();
   static final MessageHeaderDecoder MSG_HEADER_DECODER = new MessageHeaderDecoder();

   static final KeyValueEncoder KEY_VALUE_ENCODER = new KeyValueEncoder();
   static final KeyEncoder KEY_ENCODER = new KeyEncoder();

   static final EmptyDecoder EMPTY_DECODER = new EmptyDecoder();
   static final ValueDecoder VALUE_DECODER = new ValueDecoder();

   static final FragmentHandler FRAGMENT_HANDLER = new FragmentAssembler(InfinispanAeronClient::onMessage);

   static final Aeron AERON = Aeron.connect(new Aeron.Context());
   static final Publication PUBLICATION = AERON.addPublication(REQ_CHANNEL, STREAM_ID);
//   static Subscription subscription;

   public static void main(String[] args) throws InterruptedException {
//      subscription = AERON.addSubscription(REP_CHANNEL, STREAM_ID);
//      System.out.println("[client] Added subscription to response channel: " + REP_CHANNEL);
//      System.out.println("[client] Subscription connected? " + subscription.isConnected());
//      PUBLICATION = ;
//      System.out.println("[client] Added PUBLICATION to request channel: " + REQ_CHANNEL);
//      System.out.println("[client] Publication connected? " + PUBLICATION.isConnected());

      final Charset ch = Charset.forName("UTF-8");
      final byte[] key = "hello".getBytes(ch);
      final byte[] value = "world".getBytes(ch);

      final ExpandableArrayBuffer buff1 = new ExpandableArrayBuffer(512);

      KEY_VALUE_ENCODER
         .wrapAndApplyHeader(buff1, 0, MSG_HEADER_ENCODER)
         .cacheName("test")
         .putKey(key, 0, key.length)
         .putValue(value, 0, value.length);

      long result = PUBLICATION.offer(buff1, 0, MSG_HEADER_ENCODER.encodedLength() + KEY_VALUE_ENCODER.encodedLength());
      offerResult(result);

//      subscription.poll(FRAGMENT_HANDLER, FRAGMENT_LIMIT);

      Thread.sleep(2000);

//      final ExpandableArrayBuffer buff2 = new ExpandableArrayBuffer(512);
//
//      KEY_ENCODER
//         .wrapAndApplyHeader(buff2, 0, MSG_HEADER_ENCODER)
//         .cacheName("test")
//         .putKey(key, 0, key.length);
//
//      result = PUBLICATION.offer(buff2, (int) result, KEY_VALUE_ENCODER.encodedLength());
//      offerResult(result);
//
//      SUBSCRIPTION.poll(FRAGMENT_HANDLER, FRAGMENT_LIMIT);
   }

   private static void onMessage(DirectBuffer buffer, int offset, int length, Header header) {
      final int msgOffset = MessageHeaderDecoder.ENCODED_LENGTH + offset;
      MSG_HEADER_DECODER.wrap(buffer, offset);
      switch (MSG_HEADER_DECODER.templateId()) {
         case EmptyDecoder.TEMPLATE_ID:
            onEmpty(buffer, msgOffset, MSG_HEADER_DECODER.blockLength(), MSG_HEADER_DECODER.version());
            break;
         case ValueDecoder.TEMPLATE_ID:
            onValue(buffer, msgOffset, MSG_HEADER_DECODER.blockLength(), MSG_HEADER_DECODER.version());
            break;
         default:
            throw new IllegalStateException("Unknown message template");
      }
   }

   private static void onEmpty(DirectBuffer buffer, int offset, int length, int version) {
      EMPTY_DECODER.wrap(buffer, offset, length, version);
      System.out.println("Put responded");
   }

   private static void onValue(DirectBuffer buffer, int offset, int length, int version) {
      VALUE_DECODER.wrap(buffer, offset, length, version);
      final int valueLength = VALUE_DECODER.valueLength();
      final byte[] bytes = new byte[valueLength];
      VALUE_DECODER.getValue(bytes, 0, valueLength);
      System.out.printf("Get responded: %s%n", new String(bytes));
   }

}
