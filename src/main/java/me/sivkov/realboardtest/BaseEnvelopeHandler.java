/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.sivkov.realboardtest;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import me.sivkov.messages.Envelope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 *
 * @author ssp
 */
@Component
@Scope("singleton")
public class BaseEnvelopeHandler implements EnvelopeHandler, EnvelopeStorage {
    private static final int HISTORY_SIZE = 100;
    private final BlockingQueue<Envelope> toBroadcast = new LinkedBlockingQueue<>();
    private final BlockingDeque<Envelope> history = new LinkedBlockingDeque<>();
    private List<CommandHandler> commandHandlers;

    void setCommandHandlers(List<CommandHandler> commandHandlers) {
        this.commandHandlers = commandHandlers;
    }

    @Override
    public List<Envelope> getHistorySnapshot() {
        List<Envelope> historySnapshot = new LinkedList<>();
        history.stream().forEach(e -> historySnapshot.add(e));
        return historySnapshot;
    }

    @Override
    public boolean handle(Envelope e) {
        boolean result = true;
        parse:
        do {
            if (e.getAuth() != null)
                break;
            if (e.getChat() != null || e.getErr() != null)
                history.addLast(e);
            if (e.getChat()!= null) {
                toBroadcast.add(e);
                break;
            }
            if (e.getCmd() != null) {
                for (CommandHandler c : commandHandlers) {
                    if (c.handleCmdMessage(e.getCmd(), this))
                        break parse;
                }
                result = false;
                Envelope re = MessageUtils.createSysMessage("Can't find handler for your command");
                re.setTo(e.getFrom());
            }
        } while (false);
        while (history.size() > HISTORY_SIZE)
            history.removeFirst();
        return result;
    }
    
    @Override
    public Queue<Envelope> getEnvelopesToBroadcast() {
        return toBroadcast;
    }
}
