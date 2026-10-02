package com.golda.patchertiktok;

import org.junit.Test;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.*;

public class StreakSendProtocolTest {
    public static class Message {
        public boolean isSuccessStatus() { return true; }
        public String getConversationId() { return "1"; }
        public String getUuid() { return "uuid"; }
        public long getSender() { return 101; }
        public long getMsgId() { return 1001; }
    }
    public interface Callback {
        void renamedSuccess(Object conversation, Message message);
        void renamedFailure(Object conversation, Message message, Object error);
        void renamedBatch(Object conversation, List<Message> messages, Map<Message, Object> errors);
    }
    public static class Service {
        public void renamedSend(String type, String conversation, String peer, Map<?, ?> first, Map<?, ?> second,
                Object reference, Object attachment, Object data, Integer flag, Callback callback, Object header) { }
    }
    public static class Ambiguous extends Service {
        public void anotherSend(String type, String conversation, String peer, Map<?, ?> first, Map<?, ?> second,
                Object reference, Object attachment, Object data, Integer flag, Callback callback, Object header) { }
    }
    public interface InvalidCallback { boolean unrelated(); }

    @Test public void bindsBySignatureNotObfuscatedNames() throws Exception {
        assertEquals("renamedSend", StreakSendProtocol.sendMethod(Service.class).getName());
        assertEquals(Message.class, StreakSendProtocol.callbackMessageType(Callback.class));
    }
    @Test(expected = NoSuchMethodException.class) public void rejectsAmbiguousSender() throws Exception {
        StreakSendProtocol.sendMethod(Ambiguous.class);
    }
    @Test(expected = NoSuchMethodException.class) public void rejectsUnknownCallback() throws Exception {
        StreakSendProtocol.callbackMessageType(InvalidCallback.class);
    }
}
