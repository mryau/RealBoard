/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.sivkov.realboardtest;

import java.util.Queue;
import me.sivkov.messages.Envelope;

/**
 *
 * @author sivkovs
 */
interface EnvelopeStorage {
    public Queue<Envelope> getEnvelopesToBroadcast();
    
}
