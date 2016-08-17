/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.sivkov.realboardtest;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import me.sivkov.messages.Envelope;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 *
 * @author ssp
 */
public class UserTest {
    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    /**
     * Test of hasHeader method, of class User.
     */
    @Test
    public void testHasHeader() {
        System.out.println("hasHeader");
        User instance = User.createUser(InetAddress.getLoopbackAddress(), 4444);
        assertEquals(false, instance.hasHeader());
        Envelope e = MessageUtils.createAuthMessage("login", "passwd");
        ByteBuffer[] msg = MessageUtils.serializeEnvelope(e);
        instance.getHeaderBuf().put(msg[0]);
        assertEquals(true, instance.hasHeader());
        assertEquals(false, instance.hasCompleteInputMessage());
        assertEquals(null, instance.getMessageOnce());
    }

    /**
     * Test of hasCompleteInputMessage method, of class User.
     */
    @Test
    public void testHasCompleteInputMessage() {
        System.out.println("hasCompleteInputMessage");
        User instance = User.createUser(InetAddress.getLoopbackAddress(), 4444);
        assertEquals(false, instance.hasHeader());
        assertEquals(false, instance.hasCompleteInputMessage());
        Envelope e = MessageUtils.createAuthMessage("login", "passwd");
        ByteBuffer[] msg = MessageUtils.serializeEnvelope(e);
        instance.getHeaderBuf().put(msg[0]);
        assertEquals(true, instance.hasHeader());
        assertEquals(false, instance.hasCompleteInputMessage());
        instance.getPayloadBuf().put(msg[1]);
        assertEquals(true, instance.hasCompleteInputMessage());
        assertArrayEquals(msg[1].array(), instance.getMessageOnce());
    }

    /**
     * Test of getMessageOnce method, of class User.
     */
    @Test
    public void testGetMessageOnce() {
        System.out.println("getMessageOnce");
        User instance = User.createUser(InetAddress.getLoopbackAddress(), 4444);
        Envelope e = MessageUtils.createAuthMessage("login", "passwd");
        ByteBuffer[] msg = MessageUtils.serializeEnvelope(e);
        instance.getHeaderBuf().put(msg[0]);
        instance.getPayloadBuf().put(msg[1]);
        assertEquals(null, instance.getMessageOnce());
        instance.hasHeader();
        assertEquals(null, instance.getMessageOnce());
        instance.hasCompleteInputMessage();
        assertArrayEquals(msg[1].array(), instance.getMessageOnce());
        assertEquals(false, instance.hasHeader());
        assertEquals(false, instance.hasCompleteInputMessage());
        assertEquals(null, instance.getMessageOnce());
    }

    /**
     * Test of isAbleToRead method, of class User.
     */
    @Test
    public void testIsAbleToParseInput() {
        System.out.println("isAbleToParseInput");
        User instance = User.createUser(InetAddress.getLoopbackAddress(), 4444);
        byte[] h = {16,0};
        byte[] m = {1,2,3};
        assertEquals(true, instance.isAbleToParseInput());
        assertEquals(false, instance.isReadOnly());
        instance.getHeaderBuf().put(h);
        assertEquals(false, instance.hasHeader());
        assertEquals(false, instance.isAbleToParseInput());
        instance.getPayloadBuf().put(m);
        assertEquals(false, instance.hasCompleteInputMessage());
        assertEquals(false, instance.isAbleToParseInput());
        assertEquals(false, instance.isReadOnly());
    }

    /**
     * Test of isReadOnly method, of class User.
     */
    @Test
    public void testIsReadOnly() {
        System.out.println("isReadOnly");
        User instance = User.createUser(InetAddress.getLoopbackAddress(), 4444);
        assertEquals(true, instance.isAbleToParseInput());
        assertEquals(false, instance.isReadOnly());
        instance.markReadOnly();
        assertEquals(true, instance.isReadOnly());
        assertEquals(true, instance.isAbleToParseInput());
    }

    /**
     * Test of getName/setName methoda, of class User.
     */
    @Test
    public void testGetSetName() {
        System.out.println("getName/setName");
        User instance = User.createUser(InetAddress.getLoopbackAddress(), 4444);
        assertEquals(null, instance.getName());
        String name = "aaa";
        assertEquals(true, instance.changeName(name));
        assertEquals(name, instance.getName());
        User instance2 = User.createUser(InetAddress.getLoopbackAddress(), 4444);
        assertEquals(false, instance2.changeName("aaa"));
        assertEquals(true, instance.changeName("aaaa"));
        assertEquals(true, instance2.changeName("aaa"));
    }

    /**
     * Test of toString method, of class User.
     */
    @Test
    public void testToString() {
        System.out.println("toString");
        User instance = User.createUser(InetAddress.getLoopbackAddress(), 4444);
        assertEquals(true, instance.toString().contains("not authorised user from "));
        String name = "bbb";
        assertEquals(true, instance.changeName(name));
        assertEquals(name, instance.toString());
    }

    /**
     * Test of *Fifo methods, of class User.
     */
    @Test
    public void testFifo() {
        System.out.println("*Fifo");
        byte[] m1 = {1,2};
        byte[] m2 = {3,4};
        byte[] m3 = {5,6};
        byte[] m4 = {7,8};
        User instance = User.createUser(InetAddress.getLoopbackAddress(), 4444);
        instance.putToOutputFifo(m1);
        assertArrayEquals(m1, instance.getOutputFifoHead());
        assertArrayEquals(m1, instance.getOutputFifoHead());
        instance.removeOutputFifoHead();
        assertArrayEquals(null, instance.getOutputFifoHead());
        instance.putToOutputFifo(m1);
        instance.putToOutputFifo(m2);
        assertArrayEquals(m1, instance.getOutputFifoHead());
        instance.changeOutputFifoHead(m3);
        instance.putToOutputFifo(m4);
        assertArrayEquals(m3, instance.getOutputFifoHead());
        instance.removeOutputFifoHead();
        assertArrayEquals(m2, instance.getOutputFifoHead());
        instance.removeOutputFifoHead();
        assertArrayEquals(m4, instance.getOutputFifoHead());
        instance.removeOutputFifoHead();
        assertArrayEquals(null, instance.getOutputFifoHead());
    }
}
