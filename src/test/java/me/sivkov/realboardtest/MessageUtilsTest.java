/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.sivkov.realboardtest;

import java.nio.ByteBuffer;
import java.util.Arrays;
import me.sivkov.messages.AuthMessage;
import me.sivkov.messages.Envelope;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author ssp
 */
public class MessageUtilsTest {
    
    public MessageUtilsTest() {
    }

    /**
     * Test all methods of class MessageUtils.
     */
    @Test
    public void testMessageUtils() {
        Envelope e = MessageUtils.createAuthMessage("login", "passwd");
        ByteBuffer[] msg = MessageUtils.serializeEnvelope(e);
        assertEquals(2, msg.length);
        assertEquals(true, msg[0].hasArray());
        assertEquals(true, msg[1].hasArray());
        assertEquals(2, msg[0].capacity());
        byte[] src = msg[1].array();
        assertEquals(false, MessageUtils.isMsgSent(msg));
        byte[] a = MessageUtils.remainingMsg(msg);
        byte[] es = Arrays.copyOfRange(a, 2, a.length);
        assertArrayEquals(src, es);
        assertEquals(true, MessageUtils.isMsgSent(msg));
        MessageUtils.rewind(msg);
        assertEquals(false, MessageUtils.isMsgSent(msg));
        Envelope e2 = MessageUtils.deserializeEnvelope(es);
        assertEquals(e, e2);
        assertEquals(a.length-2, msg[1].capacity());
    }
}
