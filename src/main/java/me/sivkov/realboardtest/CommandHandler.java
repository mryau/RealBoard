/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.sivkov.realboardtest;

import me.sivkov.messages.CmdMessage;
import me.sivkov.messages.Envelope;

/**
 *
 * @author ssp
 */
public interface CommandHandler {
    public String getName();
    public String getDescription();
    public boolean handleCmdMessage(final CmdMessage message, EnvelopeHandler eh);
}
