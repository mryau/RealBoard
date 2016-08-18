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
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import me.sivkov.messages.ChatMessage;
import me.sivkov.messages.CmdMessage;
import me.sivkov.messages.Envelope;
import me.sivkov.messages.SysMessage;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

/**
 *
 * @author ssp
 */
public class ChatClient {
    @Option(name="-host", metaVar = "IP|NAME", usage="ip address of name of ChatServer host")
    private String host = "127.0.0.1";
    @Option(name="-port", metaVar = "NUMBER", usage="port of ChatServer host")
    private int port = 1234;
    @Option(name="-user", metaVar = "STRING", required = true, usage="your chat nickname")
    private String user;
    @Option(name="-pass", metaVar = "STRING", usage="your password")
    private String pass;

    private final InetAddress addr;
    private final Thread toServerThread = new Thread(getToServerThread());
    private final Thread fromServerThread = new Thread(getFromServerThread());
    private final BlockingQueue<Envelope> fromServer = new LinkedBlockingQueue<>();
    private final BlockingQueue<Envelope> toServer = new LinkedBlockingQueue<>();
    private SocketChannel ch;


    private ChatClient() throws UnknownHostException {
        addr = InetAddress.getByName(host);
        toServerThread.setDaemon(true);
        fromServerThread.setDaemon(true);
    }

    private Runnable getToServerThread() {
        return () -> {
            try {
                while (true) {                
                    try {
                        Envelope e = toServer.take();
                        ByteBuffer[] msg = MessageUtils.serializeEnvelope(e);
                        int written = (int)ch.write(msg);
                        if (written != MessageUtils.msgLength(msg))
                            break;
                    } catch (InterruptedException ex) {
                        Logger.getLogger(ChatClient.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                ch.close();
            } catch (IOException ex) {
                Logger.getLogger(ChatClient.class.getName()).log(Level.SEVERE, null, ex);
             } finally {
                System.err.println("Please, restart chat due to network problems.");
            }
        };
    }

    private Runnable getFromServerThread() {
        return () -> {
            try {
                while (true) {                
                    ByteBuffer reHdr = ByteBuffer.allocate(2);
                    if (ch.read(reHdr) == 2) {
                        int reMsgLen = reHdr.getShort(0);
                        if (reMsgLen > 0 && reMsgLen < MessageUtils.MAX_MSG_SIZE) {
                            ByteBuffer reMsg = ByteBuffer.allocate(reMsgLen);
                            if (ch.read(reMsg) == reMsgLen)
                                fromServer.add(MessageUtils.deserializeEnvelope(reMsg.array()));
                            else
                                break;
                        }
                    }
                }
                ch.close();
            } catch (IOException ex) {
                Logger.getLogger(ChatClient.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                System.err.println("Please, restart chat due to network problems.");
            }
        };
    }

    private void printMessage(ChatMessage m, String from) {
        if (m != null) {
            StringBuilder sb = new StringBuilder("[")
                    .append(from);
            if (m.getReplyTo() != null)
                sb.append(" -> ")
                        .append(m.getReplyTo());
            sb.append("]: ")
                    .append(m.getMsg());
            System.out.println(sb);
        }
    }

    private void printMessage(SysMessage m) {
        if (m != null)
            System.out.println("SYSTEM MESSAGE: " + m.getMsg());
    }

    private void printMessage(CmdMessage m) {
        if (m != null)
            System.out.println("SERVER ANSWER: ");
    }

    private void printMessage(String m) {
        System.out.println("[" + user +"]: " + m);
    }

    private void session() throws IOException {
        ch = SocketChannel.open();
        ch.connect(new InetSocketAddress(addr, port));
        toServerThread.start();
        fromServerThread.start();
        Envelope auth = MessageUtils.createAuthMessage(user, pass);
        toServer.add(auth);
        List<String> history = new LinkedList<>();
        byte[] inBuf = new byte[1024];
        StringBuilder sb = new StringBuilder();
        while (true) {
            for (Envelope e = fromServer.poll(); e != null; e = fromServer.poll()) {
                printMessage(e.getErr());
                printMessage(e.getChat(), e.getFrom());
                printMessage(e.getCmd());
            }
            int n = System.in.available();
            if (n > 0) {
                if (-1 == System.in.read(inBuf, 0, n))
                    return;
                sb.append(new String(inBuf, 0, n));
                int i = sb.indexOf("\n");
                if (i != -1) {
                    String m = sb.substring(0, i).trim();
                    printMessage(m);
                    if (m.length() > 0) {
                        if (m.charAt(0) == '/')
                            toServer.add(MessageUtils.createCmdMessage(user, m));
                        else
                            toServer.add(MessageUtils.createChatMessage(user, m));
                    }
                    sb.replace(0, i+1, "");
                }
            }
        }
    }

    public static void main(String[] args) throws UnknownHostException, IOException {
        ChatClient client = new ChatClient();
        CmdLineParser parser = new CmdLineParser(client);
        try {
                parser.parseArgument(args);
                client.session();
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
        }
    }    

}
