package me.sivkov.realboardtest;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import me.sivkov.messages.ChatMessage;
import me.sivkov.messages.Envelope;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 *
 * @author ssp
 */
@Component
@Scope("prototype")
public class RawTcpProtocolHandler implements ProtocolHandler {
    private int port;
    private InetAddress addr;
    private Selector rwSelector;
    private ServerSocketChannel ssc;
    private final ByteBuffer buf = ByteBuffer.allocate(1024);
    private EnvelopeHandler envelopeHandler;

    @Override
    public final int getPort() {
        return port;
    }

    @Override
    public final InetAddress getInetAddress() {
        return addr;
    }

    @Override
    public boolean register(Selector acceptSelector, EnvelopeHandler envelopeHandler, InetAddress addr, int port) {
        try {
            this.addr = addr;
            this.port = port;
            this.envelopeHandler = envelopeHandler;
            ssc = ServerSocketChannel.open();
            ssc.configureBlocking(false);
            ssc.socket().bind(new InetSocketAddress(getInetAddress(), getPort()));
            ssc.register(acceptSelector, SelectionKey.OP_ACCEPT)
                    .attach(this);
            rwSelector = Selector.open();
            return true;
        } catch (IOException e) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
            return false;
        }
    }

    @Override
    public void handle(SelectionKey key) {
        if (key.isValid()) {
            try {
                if (key.isAcceptable())
                    handleAccept(key);
                if (key.isReadable())
                    this.handleRead(key);
                if (key.isWritable())
                    this.handleWrite(key);
            } catch (IOException e) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
                User u = (User)key.attachment();
                User.dropUser(u);
                key.cancel();
            }
        }
    }
    
    private void handleAccept(SelectionKey key) throws IOException {
        SocketChannel sc = ((ServerSocketChannel) key.channel()).accept();
        sc.configureBlocking(false);
        User user = User.createUser(sc.socket().getInetAddress(), sc.socket().getPort());
        synchronized (this) {
            rwSelector.wakeup();
            sc.register(rwSelector, SelectionKey.OP_READ, user);
        }
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel ch = (SocketChannel) key.channel();
        User u = (User)key.attachment();
        if (u.isReadOnly()) {
            assert false; //impossible state
        } else if (false == u.isAbleToParseInput()) {
            ByteBuffer b = u.getPayloadBuf();
            while (ch.read(b) > 0)
                b.clear();
            u.markReadOnly();
            ch.shutdownInput();
            envelopeHandler.handle(MessageUtils.createSysMessage("User " + u + " moderated to readonly mode for spam activity"), null);
            return;
        }
        buf.clear();
        while (false == u.hasHeader()) {
            if (ch.read(u.getHeaderBuf()) == 0)
                break;
        }
        if (u.hasHeader()) {
            while (ch.read(u.getPayloadBuf()) > 0) {
                if (u.hasCompleteInputMessage()) {
                    byte[] b = u.getMessageOnce();
                    Envelope env = MessageUtils.deserializeEnvelope(b);
                    if (u.getName() == null) { //new user should firstly authentificate itself
                        if (env.getAuth() == null || u.changeName(env.getAuth().getLogin()) == false) {
                            Envelope err = MessageUtils.createSysMessage("Please firstly authentificate himself");
                            ByteBuffer[] msg = MessageUtils.serializeEnvelope(err);
                            sendMessageToUser(key, u, msg);
                        } else {
                            Envelope welcome = MessageUtils.createSysMessage("Welcome to chat!\nUse /help command to see list of available commands.");
                            ByteBuffer[] msg = MessageUtils.serializeEnvelope(welcome);
                            sendMessageToUser(key, u, msg);
                        }
                    } else {
                        Function<Envelope, Void> directReply = e -> {
                            ByteBuffer[] msg = MessageUtils.serializeEnvelope(e);
                            sendMessageToUser(key, u, msg);
                            return null;
                        };
                    
                        envelopeHandler.handle(env, directReply);
                    }
                }
            }
        }
    }

    private void handleWrite(SelectionKey key) throws IOException {
        SocketChannel ch = (SocketChannel) key.channel();
        User u = (User)key.attachment();
        byte[] a = u.getOutputFifoHead();
        if (a == null)
            key.interestOps(SelectionKey.OP_READ);
        else {
            ByteBuffer msgBuf = ByteBuffer.wrap(a);
            ch.write(msgBuf);
            if (msgBuf.remaining() > 0) {
                a = new byte[msgBuf.remaining()];
                u.changeOutputFifoHead(a);
                key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            } else {
                u.removeOutputFifoHead();
                handleWrite(key);
            }
        }
    }
    
    private void sendMessageToUser(SelectionKey key, User u, ByteBuffer[] msg) {
        try {
            if (false == u.hasUnsentMessages()) {
                SocketChannel ch = (SocketChannel) key.channel();
                ch.write(msg);
            }
            if (false == MessageUtils.isMsgSent(msg)) {
                u.putToOutputFifo(MessageUtils.remainingMsg(msg));
                key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            }
        } catch (IOException e) {
        }
    }

    @Override
    public void broadcast(Envelope env) {
        ByteBuffer[] msg = MessageUtils.serializeEnvelope(env);
        ChatMessage chatMsg = env.getChat();
        for (SelectionKey key : rwSelector.keys()) {
                User u = (User)key.attachment();
                if (u.getName()== null || (chatMsg != null && env.getFrom().equals(u.getName())))
                    continue;
                sendMessageToUser(key, u, msg);
                MessageUtils.rewind(msg);
        }
    }

    @Override
    public String getName() {
        return "Raw TCP Plugin";
    }

    @Override
    public void run() {
        try {
            Iterator<SelectionKey> iter;
            SelectionKey key;
            while (true) {
                synchronized (this) {}; //due to we use different threads to accept/read-write
                rwSelector.select();
                iter = rwSelector.selectedKeys().iterator();
                while (iter.hasNext()) {
                    key = iter.next();
                    iter.remove();
                    User u = (User)key.attachment();
                    handle(key);
                }
            }
        } catch (IOException e) {
            System.out.println("error: " + getName() + "/" + getPort() + " terminating. Stack trace:");
            e.printStackTrace();
        }
    }
}
