/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.prosicraft.ultrachat;

import com.prosicraft.ultrachat.util.MLog;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 *
 * @author prosicraft
 */
public class UCListener implements Listener {     
        
        public UltraChat parent = null;
        
        public UCListener (UltraChat prnt) {
                parent = prnt;
        }
        
        @EventHandler(priority=EventPriority.LOW)
        public void onPlayerChat (AsyncPlayerChatEvent e)
        {                
                if (parent != null) {
                        
                        if ( !parent.enabled ) return;
                        
                        String msg = parent.parseformat(e.getMessage(), e.getPlayer());
                        msg = msg.replaceAll( "%", "Prozent" );
			e.setFormat(msg);
                        if ( MLog.real(e.getMessage()).trim().equalsIgnoreCase("") && 
                                !parent.perms.has(e.getPlayer(), "ultrachat.emtpymessage") &&
                                !e.getPlayer().isOp())
                                
                                e.setCancelled(true);
                }
        }
}
