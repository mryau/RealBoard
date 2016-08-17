/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.sivkov.realboardtest;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 *
 * @author ssp
 */
@Component
@Scope("singleton")
public class RegisteredHandlersHelper {
    @Autowired
    List<ProtocolHandler> protocolHandlers;
    @Autowired
    List<CommandHandler> commandHandlers;
}
