/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.sivkov.realboardtest;

import java.net.InetAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import me.sivkov.messages.Envelope;

/**
 *
 * @author ssp
 */
public interface ProtocolHandler extends Runnable {
    String getName();
    int getPort();
    InetAddress getInetAddress();
    //attach this handler to external selector
    boolean register(Selector selector, EnvelopeHandler envelopeHandler, InetAddress addr, int port);
    //handle external selector's event
    void handle(SelectionKey key);
    void broadcast(Envelope m);
}
