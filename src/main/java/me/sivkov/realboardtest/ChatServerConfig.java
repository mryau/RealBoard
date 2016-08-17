/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.sivkov.realboardtest;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 *
 * @author ssp
 */
@Configuration
@Scope("singleton")
@ComponentScan({ "me.sivkov.realboardtest", "com.realboard.plugins" })
public class ChatServerConfig {
}
