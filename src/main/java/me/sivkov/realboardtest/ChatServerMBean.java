/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.sivkov.realboardtest;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;

/**
 *
 * @author ssp
 */
public interface ChatServerMBean {
    public List<String> getRegisteredCommandHandlersInfo();
    public List<String> getRegisteredProtocolHandlersInfo();
    public Map<String, Integer> getActiveProtocolHandlersInfo();
    public void useProtocolHandler(String name, String host, int port);
    public int getConnectionsCount();
    public int getCoresCount();
}
