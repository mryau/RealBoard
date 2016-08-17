/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.sivkov.realboardtest;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import me.sivkov.messages.Envelope;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

/**
 *
 * @author ssp
 */
public class ChatBot implements Runnable {
    @Option(name="-pool-size", metaVar = "POOLSIZE", usage="start workers in thread pool, as many threads in the pool as this option says")
    private int poolSize = 0;
    @Option(name="-host", metaVar = "IP|NAME", usage="ip address of name of ChatServer host")
    private String host = "127.0.0.1";
    @Option(name="-port", metaVar = "NUMBER", usage="port of ChatServer host")
    private int port = 1234;
    @Option(name="-workers", metaVar = "NUMBER", usage="how many workers will be execute due benchmark")
    private int workers = 1;
    @Option(name="-msg-per-conn", metaVar = "NUMBER", usage="how many messages each worker will send")
    private int msgPerConn = 1;

    private final InetAddress addr;
    private final AtomicInteger uniqueUserId;
    private final AtomicInteger noErrorsSession, errorsConnect, errorsSent, errorsReceive, errorsOtherException;

    
    private ChatBot() throws UnknownHostException {
        addr = InetAddress.getByName(host);
        uniqueUserId = new AtomicInteger();
        noErrorsSession = new AtomicInteger();
        errorsConnect = new AtomicInteger();
        errorsSent = new AtomicInteger();
        errorsReceive = new AtomicInteger();
        errorsOtherException = new AtomicInteger();
    }

    private void benchmark() {
        if (poolSize == 0 && workers == 1) {
            run();
            return;
        }
        ExecutorService service = Executors.newFixedThreadPool(poolSize);
        List<Future<Runnable>> futures = new ArrayList<>();

        for (int i = 0; i < workers; ++i) {
            Future f = service.submit(this);
            futures.add(f);
        }            
        // wait for all tasks to complete before continuing
        for (Future<Runnable> f : futures) {
            try {
                f.get();
            } catch (InterruptedException | ExecutionException ex) {
                errorsOtherException.incrementAndGet();
            }
        }

        //shut down the executor service so that this thread can exit
        service.shutdownNow();
    }

    private static class SocketChannelStore {
        public SocketChannel ch = null;
    }

    Predicate<SocketChannelStore> isConnected() {
        return store -> {
            try {
                store.ch = SocketChannel.open();
                store.ch.connect(new InetSocketAddress(addr, port));
                return true;
            } catch (IOException e) {
                errorsConnect.incrementAndGet();
            }
            return false;
        };
    };

    Predicate<SocketChannelStore> isAuthMsgSent() {
        return store -> {
            try {
                int id = uniqueUserId.incrementAndGet();
                Envelope e = MessageUtils.createAuthMessage("user"+id, "123");
                ByteBuffer[] msg = MessageUtils.serializeEnvelope(e);
                int written = (int)store.ch.write(msg);
                if (written == MessageUtils.msgLength(msg))
                    return true;
            } catch (IOException e) {
            }
            errorsSent.incrementAndGet();
            return false;
        };
    };
    
    Predicate<SocketChannelStore> isMsgReceived() {
        return store -> {
            try {
                ByteBuffer reHdr = ByteBuffer.allocate(2);
                if (store.ch.read(reHdr) == 2) {
                    int reMsgLen = reHdr.getShort(0);
                    if (reMsgLen > 0 && reMsgLen < MessageUtils.MAX_MSG_SIZE) {
                        ByteBuffer reMsg = ByteBuffer.allocate(reMsgLen);
                        if (store.ch.read(reMsg) == reMsgLen) {
                            noErrorsSession.incrementAndGet();
                            return true;
                        }
                    }
                }
                errorsReceive.incrementAndGet();
            } catch (IOException e) {
                errorsConnect.incrementAndGet();
            }
            return false;
        };
    };

    @Override
    public void run() {
        for (int i = 0; i < msgPerConn; ++i) {
            SocketChannelStore ch = new SocketChannelStore();
            isConnected().and(isAuthMsgSent()).and(isMsgReceived()).test(ch);
        }
    }

    public static void main(String[] args) throws UnknownHostException {
        Instant start = Instant.now();
        ChatBot bot = new ChatBot();
        CmdLineParser parser = new CmdLineParser(bot);
        try {
                parser.parseArgument(args);
                if (bot.workers != 1 && bot.poolSize <= 0) {
                    throw new CmdLineException("you should use -pool-model if you want many workers");
                }
                bot.benchmark();
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
            return;
        }
        Instant stop = Instant.now();
        System.out.println("Session duration: " + Duration.between(start, stop));
        System.out.println("Session statistics: ");
        System.out.println("\tpool size: " + bot.poolSize + "; workers: " + bot.workers + "; messages per worker: " + bot.msgPerConn);
        System.out.println("\tnoErrorsSession: " + bot.noErrorsSession.toString());
        System.out.println("\terrorsConnect: " + bot.errorsConnect.toString());
        System.out.println("\terrorsSent: " + bot.errorsSent.toString());
        System.out.println("\terrorsReceive: " + bot.errorsReceive.toString());
        System.out.println("\terrorsOtherException: " + bot.errorsOtherException.toString());
    }
}
