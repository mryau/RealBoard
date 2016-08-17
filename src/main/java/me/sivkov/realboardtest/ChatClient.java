/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.sivkov.realboardtest;

import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
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
    @Option(name="-host", metaVar = "STRING", required = true, usage="your chat nickname")
    private String user;
    @Option(name="-pass", metaVar = "STRING", usage="your password")
    private String pass;

    public static void main(String[] args) throws UnknownHostException {
        Instant start = Instant.now();
        ChatClient client = new ChatClient();
        CmdLineParser parser = new CmdLineParser(client);
        try {
                parser.parseArgument(args);
                client.session();
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
            return;
        }
        Instant stop = Instant.now();
        System.out.println("Session duration: " + Duration.between(start, stop));
    }    

    private void session() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
