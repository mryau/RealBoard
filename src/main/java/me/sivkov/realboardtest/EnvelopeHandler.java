/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.sivkov.realboardtest;

import java.util.List;
import java.util.Queue;
import java.util.function.Function;
import me.sivkov.messages.Envelope;

/**
 *
 * @author ssp
 */
public interface EnvelopeHandler {
    public List<Envelope> getHistorySnapshot();
    public boolean handle(Envelope e, Function<Envelope, Void> directReply);
}
