/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.sivkov.realboardtest;

import java.nio.channels.*;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import me.sivkov.messages.Envelope;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 *
 * @author ssp
 */
public class ChatServer implements Runnable, ChatServerMBean {
    public static final int BASE_PORT = 1234;
    private static final int BROADCAST_INTERVAL_MS = 500;
    private final MBeanServer mbs;
    private final Selector acceptSelector;
    private final Map<String, ArrayList<ProtocolHandler>> activeProtocolHandlers = new HashMap<>();
    private final AtomicInteger connAttempts;
    private final EnvelopeHandler envelopeHandler;
    private final EnvelopeStorage broadcastStorage;
    private final RegisteredHandlersHelper registeredHandlers;
    private final ApplicationContext applicationContext;
    private List<Thread> threads = new LinkedList<>();

    ChatServer(ApplicationContext ctx, RegisteredHandlersHelper helper, EnvelopeHandler handler, EnvelopeStorage storage) throws Exception {
        mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName csObjName = new ObjectName("ChatServer:name=mngmt");
        mbs.registerMBean(this, csObjName);
        connAttempts = new AtomicInteger();
        this.registeredHandlers = helper;
        this.acceptSelector = Selector.open();
        this.envelopeHandler = handler;
        this.broadcastStorage = storage;
        this.applicationContext = ctx;
    }

    boolean addProtocolHandler(ProtocolHandler h, InetAddress addr, int port) {
        if (h.register(acceptSelector, envelopeHandler, addr, port)) {
            ArrayList<ProtocolHandler> al;
            if (activeProtocolHandlers.containsKey(h.getName()))
                al = activeProtocolHandlers.get(h.getName());
            else {
                al = new ArrayList<>();
                activeProtocolHandlers.put(h.getName(), al);
            }
            al.add(h);
            Thread t = new Thread(h);
            threads.add(t);
            t.setDaemon(true);
            t.start();
            return true;
        }
        return false;
    }

    @Override
    public void run() {
        try {
            Iterator<SelectionKey> iter;
            SelectionKey key;
            while (true) {
                acceptSelector.select(BROADCAST_INTERVAL_MS);
                iter = acceptSelector.selectedKeys().iterator();
                while (iter.hasNext()) {
                    connAttempts.incrementAndGet();
                    key = iter.next();
                    iter.remove();
                    ProtocolHandler h = (ProtocolHandler)key.attachment();
                    h.handle(key);
                }
                Queue<Envelope> q = broadcastStorage.getEnvelopesToBroadcast();
                for (Envelope m = q.poll(); m != null; m = q.poll()) {
                    for (ArrayList<ProtocolHandler> al : activeProtocolHandlers.values())
                        for (ProtocolHandler h : al)
                            h.broadcast(m);
                }
            }
            
        } catch (IOException e) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
        }
    }

    @Override
    public List<String> getRegisteredProtocolHandlersInfo() {
        List<String> r = new LinkedList<>();
        if (registeredHandlers != null && registeredHandlers.protocolHandlers != null) {
            for (ProtocolHandler h : registeredHandlers.protocolHandlers) {
                r.add(h.getName());
            }
        }
        return r;
    }

    @Override
    public List<String> getRegisteredCommandHandlersInfo() {
        List<String> r = new LinkedList<>();
        if (registeredHandlers != null && registeredHandlers.commandHandlers != null) {
            for (CommandHandler h : registeredHandlers.commandHandlers) {
                r.add(h.getName());
            }
        }
        return r;
    }

    @Override
    public int getConnectionsCount() {
        return connAttempts.intValue();
    }

    @Override
    public int getCoresCount() {
        return Runtime.getRuntime().availableProcessors();
    }

    @Override
    public Map<String, Integer> getActiveProtocolHandlersInfo() {
        Map<String, Integer> p = new TreeMap<>();
        for (ArrayList<ProtocolHandler> al: activeProtocolHandlers.values()) {
            p.put(al.get(0).getName(), al.size());
        }
        return p;
    }

    @Override
    public void useProtocolHandler(String name, String host, int port) {
        if (registeredHandlers != null && registeredHandlers.protocolHandlers != null) {
            for (ProtocolHandler h : registeredHandlers.protocolHandlers) {
                if (0 == h.getName().compareToIgnoreCase(name)) {
                    ProtocolHandler yah = applicationContext.getBean(h.getClass());
                    InetAddress addr;
                    try {
                        addr = InetAddress.getByName(host == null || host.trim().length() == 0 ? null : host);
                    } catch (UnknownHostException ex) {
                        Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                        break;
                    }
                    addProtocolHandler(yah, addr, port);
                    break;
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        ApplicationContext ctx = new AnnotationConfigApplicationContext(ChatServerConfig.class);
        RegisteredHandlersHelper helper = ctx.getBean(RegisteredHandlersHelper.class);
        BaseEnvelopeHandler envelopeHandler = ctx.getBean(BaseEnvelopeHandler.class);
        envelopeHandler.setCommandHandlers(helper.commandHandlers);

        HelpCommandHandler helpCmdHelper = ctx.getBean(HelpCommandHandler.class);
        StringBuilder sb = new StringBuilder("Available commands:\n");
        for (CommandHandler ch : helper.commandHandlers) {
            sb.append("/"+ch.getName()+" "+ch.getDescription()+"\n");
            
        }
        helpCmdHelper.addCmdDescription(sb.toString());
        
        ChatServer server = new ChatServer(ctx, helper, envelopeHandler, envelopeHandler);
        int port = BASE_PORT;
        for (ProtocolHandler ph : helper.protocolHandlers) {
            if (server.addProtocolHandler(ph, InetAddress.getByName(null), port)) {
                System.out.println("Protocol handler '" + ph.getName() +"' listen on port "+Integer.toString(port));
                ++port;
            }
                
        }
        Thread serverThread = new Thread(server);
        serverThread.start();
        while (true) {
            try {
                serverThread.join();
                break;
            } catch (InterruptedException ex) {
                Logger.getLogger(ChatServer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

}
