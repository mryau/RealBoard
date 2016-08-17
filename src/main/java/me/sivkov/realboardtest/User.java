/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.sivkov.realboardtest;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 *
 * @author ssp
 */
public class User {
    private static final int REQ_HDR = 0;
    private static final int REQ_PAYLOAD = 1;
    private static final int SEND_MSG = 2;
    private static final Map<String, User> users = new HashMap<>();
    State[] msgStates = { new State(), new HasHdrState(), new HasMsgState() };
    State msgState = msgStates[REQ_HDR];
    private boolean ableToParseInput = true;
    private boolean readOnly = false;
    private final InetAddress addr;
    private final int port;
    private String name;
    private final byte[] inHdr = new byte[2];
    private final byte[] inPayload = new byte[MessageUtils.MAX_MSG_SIZE];
    private final ByteBuffer inHdrBuf = ByteBuffer.wrap(inHdr);
    private final ByteBuffer inPayloadBuf = ByteBuffer.wrap(inPayload);
    private final LinkedList<byte[]> outFifo = new LinkedList<>();

    
    private class State {
        public boolean hasHeader() {
            if (false == inHdrBuf.hasRemaining()) {
                int r = java.lang.Byte.toUnsignedInt(inHdr[0])*256+java.lang.Byte.toUnsignedInt(inHdr[1]);
                if (r >= inPayload.length) {
                    ableToParseInput = false;
                    inPayloadBuf.clear(); //to read rest from channel
                    return false;
                }
                inPayloadBuf.limit(r);
                nextState(REQ_PAYLOAD);
                return true;
            }
            return false;
        }
        public boolean hasCompleteInputMessage() { return false; }
        public byte[] getMessageOnce() { return null; }
        public void nextState(int i) { msgState = msgStates[i]; }
    }
    
    private class HasHdrState extends State {
        public boolean hasHeader() { return true; }
        public boolean hasCompleteInputMessage() {
            if (false == inPayloadBuf.hasRemaining()) {
                nextState(SEND_MSG);
                return true;
            }
            return false;
        }
    }

    private class HasMsgState extends State {
        public boolean hasHeader() { return true; }
        public boolean hasCompleteInputMessage() { return true; }
        public byte[] getMessageOnce() {
            int sz = inPayloadBuf.position();
            inPayloadBuf.flip();
            byte[] a = new byte[sz];
            inPayloadBuf.get(a);
            inHdrBuf.clear();
            inPayloadBuf.clear();
            nextState(REQ_HDR);
            return a;
        }
    }

    public static final User createUser(InetAddress addr, int port) {
        return new User(addr, port);
    }

    public static final void dropUser(User u) {
        synchronized (users) {
            if (users.containsKey(u.getName())) {
                users.remove(u.getName());
            }
        }
    }

    private User(InetAddress addr, int port) {
        this.addr = addr;
        this.port = port;
    }

    public ByteBuffer getHeaderBuf() {
        return inHdrBuf;
    }

    public ByteBuffer getPayloadBuf() {
        return inPayloadBuf;
    }

    public boolean hasHeader() {
        if (ableToParseInput)
            return msgState.hasHeader();
        return false;
    }

    public boolean hasCompleteInputMessage() {
        if (ableToParseInput)
            return msgState.hasCompleteInputMessage();
        return false;
    }

    public byte[] getMessageOnce() {
        if (ableToParseInput)
            return msgState.getMessageOnce();
        return null;
    }

    boolean isAbleToParseInput() {
        return ableToParseInput;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void markReadOnly() {
        this.readOnly = true;
    }

    public String getName() {
        return name;
    }
    
    public boolean changeName(String name) {
        assert name != null;
        synchronized(users) {
            if (users.containsKey(name)) {
                return this.name != null && this.name.equals(name);
            }
            users.remove(this.name);
            this.name = name;
            users.put(name, this);
        }
        return true;
    }

    @Override
    public String toString() {
        return name != null ? name : "not authorised user from " + addr.toString() + ":" + port;
    }
    
    public void putToOutputFifo(byte[] m) {
        outFifo.offerLast(m);
    }

    public boolean hasUnsentMessages() {
        return false == outFifo.isEmpty();
    }
    
    public byte[] getOutputFifoHead() {
        if (outFifo.isEmpty())
            return null;
        return outFifo.getFirst();
    }
    
    public void removeOutputFifoHead() {
        assert false == outFifo.isEmpty();
        outFifo.removeFirst();
    }

    public void changeOutputFifoHead(byte[] m) {
        assert false == outFifo.isEmpty();
        outFifo.removeFirst();
        outFifo.offerFirst(m);
    }
}
