/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.sivkov.realboardtest;

import me.sivkov.messages.CmdMessage;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 *
 * @author sivkovs
 */
@Component
@Scope("singleton")
public class HelpCommandHandler implements CommandHandler {
    String help;

    @Override
    public String getName() {
        return "Help";
    }

    @Override
    public String getDescription() {
        return "Show all registered chat commands";
    }

    @Override
    public boolean handleCmdMessage(CmdMessage message, EnvelopeHandler eh) {
        if (message.getCmd().equalsIgnoreCase("/help") || message.getCmd().equalsIgnoreCase("/?")) {
            MessageUtils.createSysMessage(help);
            return true;
        }
        return false;
    }

    void addCmdDescription(String help) {
        this.help = help;
    }
    
}
