/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.sivkov.realboardtest;

import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import java.nio.ByteBuffer;
import me.sivkov.messages.AuthMessage;
import me.sivkov.messages.Envelope;
import me.sivkov.messages.SysMessage;

/**
 *
 * @author ssp
 */
public class MessageUtils {
    public static final int MAX_MSG_SIZE = 2048;
    private static final Schema<Envelope> envelopeSchema = Envelope.getSchema();

    public static final Envelope createAuthMessage(String login, String pass) {
        AuthMessage msg = new AuthMessage(login);
        msg.setPasswd(pass);
        Envelope e = new Envelope();
        e.setAuth(msg);
        return e;
    }

    public static final Envelope createSysMessage(String msg) {
        SysMessage s = new SysMessage(msg);
        Envelope e = new Envelope();
        e.setErr(s);
        return e;
    }

    public static final ByteBuffer[] serializeEnvelope(Envelope env) {
        byte[] a = ProtostuffIOUtil.toByteArray(env, envelopeSchema, LinkedBuffer.allocate(MAX_MSG_SIZE));
        ByteBuffer msgBuf = ByteBuffer.wrap(a);
        byte[] hdr = new byte[2];
        ByteBuffer hdrBuf = ByteBuffer.wrap(hdr);
        assert a.length < Short.MAX_VALUE && a.length < MAX_MSG_SIZE;
        hdrBuf.putShort((short)a.length);
        hdrBuf.rewind();
        return new ByteBuffer[] {hdrBuf, msgBuf};
    }

    public static final Envelope deserializeEnvelope(byte[] m) {
        Envelope r = envelopeSchema.newMessage();
        ProtostuffIOUtil.mergeFrom(m, r, envelopeSchema);
        return r;
    }

    public static final boolean isMsgSent(ByteBuffer[] msg) {
        for (ByteBuffer bb : msg) {
            if (bb.hasRemaining())
                return false;
        }
        return true;
    }

    public static final int msgLength(ByteBuffer[] msg) {
        int len = 0;
        for (ByteBuffer bb : msg) {
            len += bb.limit();
        }
        return len;
    }

    public static final byte[] remainingMsg(ByteBuffer[] msg) {
        int len = 0;
        for (ByteBuffer bb : msg) {
            if (bb.hasRemaining())
                len += bb.remaining();
        }
        byte[] a = new byte[len];
        ByteBuffer r = ByteBuffer.wrap(a);
        for (ByteBuffer bb : msg) {
            if (bb.hasRemaining())
                r.put(bb);
        }
        return a;
    }

    public static final void rewind(ByteBuffer[] msg) {
        for (ByteBuffer bb : msg) {
            bb.rewind();
        }
    }
  
}
