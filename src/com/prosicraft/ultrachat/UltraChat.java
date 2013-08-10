/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.prosicraft.ultrachat;

import com.prosicraft.ultrachat.util.MConfiguration;
import com.prosicraft.ultrachat.util.MLog;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author prosicraft
 */
public class UltraChat extends JavaPlugin {

        public PluginDescriptionFile pdfFile = null;  
        public MConfiguration config = null;     
        public Permission perms = null;
        public String format = "&8$world $pre&7$nm &8: &f$suf$msg";
        public Map<String,String> prefixes = new HashMap<>();        
        public Map<String,String> suffixes = new HashMap<>(); 
        public boolean trim = true;
        public boolean freshconfig = false;
        public boolean enabled = true;
        
        // =====================================================================
        //      Load all stuff
        @Override
        public void onEnable () {                
                pdfFile = this.getDescription();
                
                if ( !setupPermissions() )
                        MLog.w("Couldn't hook into permissions yet. Trying on use.. (Have you installed Vault?)");                
                
                getServer().getPluginManager().registerEvents(new UCListener(this), this);                                
                
                initConfig();
                if ( freshconfig ) save(); else { load(); save(); }
                
                MLog.i("Enabled UltraChat Version " + pdfFile.getVersion() + "." );
        }
        
        public void load () {
                config.load();
                enabled = config.getBoolean("enabled", enabled);
                prefixes = new HashMap<>();
                suffixes = new HashMap<>();
                format = config.getString("global-format", format);
                for (String s : config.getKeys("prefixes")) {
                        prefixes.put(s, config.getString("prefixes." + s));
                }
                for (String s : config.getKeys("suffixes")) {
                        suffixes.put(s, config.getString("suffixes." + s));
                }                
                MLog.i("Settings loaded");
        }
        
        public void save () {
                config.set("enabled", enabled);                
                config.set("global-format", format);
                for (String gs : prefixes.keySet()) {
                        config.set("prefixes." + gs, prefixes.get(gs));
                }
                for (String gs : suffixes.keySet()) {
                        config.set("suffixes." + gs, suffixes.get(gs));
                }
                config.save();
                MLog.i("Settings saved");
        }
	
	public String parserainbow (String message)
	{
		String msgb = message;
		String msg = msgb;
		if( msgb.contains("#gpride") && msgb.length() > 7 )
		{
		    msg = msgb.substring( msgb.indexOf("#gpride") + 7 );                            

		    String orig = msg;
		    String newmsg = "";
		    String[] colors = { "" + ChatColor.RED,
					"" + ChatColor.GOLD,
					"" + ChatColor.YELLOW,
					"" + ChatColor.GREEN,
					"" + ChatColor.BLUE,
					"" + ChatColor.DARK_PURPLE};
		    for( int i=0;i<orig.length();i++ )
			newmsg += ( colors[i%6] + orig.charAt(i)  );
		    msg = msgb.substring(0, msgb.indexOf("#gpride")) + newmsg;
		}
		return msg;
	}
        
        public String parseformat (String message, Player p)
        {
                if ( message == null || message.isEmpty() ) return "";
                                
                String pFormat = format;
                String msg,msgb;
                if (p.hasPermission("ultrachat.colors")) {
                        msgb = MLog.real(message);
                        
                        if( msgb.contains("#rainbow#") )
                        {
                            msg = msgb.replaceAll( "#rainbow#", ChatColor.BLACK + "" );
                        }
                        else
                            msg = msgb;
                } else
                        msg = message;		
                pFormat = replaceAll(pFormat, "$nm", p.getName());
                pFormat = replaceAll(pFormat, "$world", p.getWorld().getName());                
                                
                try {
                        pFormat = replaceAll(pFormat, "$prpre", prefixes.get(perms.getPrimaryGroup(p)));
                        pFormat = replaceAll(pFormat, "$prsuf", suffixes.get(perms.getPrimaryGroup(p)));
                        if (pFormat.contains("$pre")) {
                                String pre = "";
                                for ( String gs : perms.getPlayerGroups(p) )
                                        if (prefixes.containsKey(gs))
                                                pre += prefixes.get(gs);            
                                pFormat = replaceAll(pFormat, "$pre", pre);
                        }
                        if (pFormat.contains("$suf")) {
                                String suf = "";
                                for ( String gs : perms.getPlayerGroups(p) )
                                        if (suffixes.containsKey(gs))
                                                suf += suffixes.get(gs);            
                                pFormat = replaceAll(pFormat, "$suf", suf);
                        }
                } catch (UnsupportedOperationException uoex) {
                        MLog.w("Groups were not supported by your permissions system!");                        
                }                
                
                pFormat = MLog.real(pFormat);
                msg = parserainbow( msg );
		pFormat = replaceAll(pFormat, "$msg", msg);                                
		pFormat = parserainbow( pFormat );
                return pFormat;
                
                //return (trim) ? MLog.real(pFormat).trim() : MLog.real(pFormat);                
        }
        
        public String antiSpam (String msg)
        {
                String res = msg;
                
                int c1 = 0;
                while ( c1 < 10 && res.contains("!!") ) {
                        c1++;
                        res.replaceAll("!!", "!");
                }                
                Pattern p = Pattern.compile("([A-Z])(\\1{1,2})\\1*");
                Matcher m = p.matcher(res);
                res = m.replaceAll("$1$2"); 
                
                
                
                return res;
        }
        
        public String replaceAll (String src, String needle, String re) {
                String repl = (re != null && !re.isEmpty() && !re.equalsIgnoreCase("null")) ? re : "";
                String s = src;
                int c = 0;
                while ( s.contains(needle) ) {
                        s = s.replace(needle, repl);
                        c++;
                        if ( c > 100 ) return "Overflow";
                }
                return s;
        }
        
        // =====================================================================
        //      Initalize the Configuration file
        public void initConfig ()
        {
                
                if (this.config != null) return;
                
                if ( !this.getDataFolder().exists() && !getDataFolder().mkdirs() )
                        MLog.e("Can't create missing configuration Folder for UltraChat");

                File cf = new File(this.getDataFolder(),"config.yml");

                if (!cf.exists()) {
                        try {
                                MLog.w("Configuration File doesn't exist. Trying to recreate it...");
                                if (!cf.createNewFile() || !cf.exists())
                                {
                                        MLog.e("Placement of Plugin might be wrong or has no Permissions to access configuration file.");
                                }
                                freshconfig = true;
                        } catch (IOException iex) {
                                MLog.e("Can't create unexisting configuration file");
                        }
                }

                config = new MConfiguration (YamlConfiguration.loadConfiguration(cf), cf);

                config.load();
                
        }                
        
        // =====================================================================
        //      Save and disable all stuff
        @Override
        public void onDisable () {
                MLog.i("Disabled UltraChat.");
        }
        
        // =====================================================================
        //      Load Vault permissions
        private boolean setupPermissions()
        {
                RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
                if (permissionProvider != null) {
                        perms = permissionProvider.getProvider();
                }
                if (perms != null)
                        MLog.i("Hooked into permissions");
                return (perms != null);
        }
        
        // =====================================================================
        //      Handle commands
        @Override
        public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {                
                
                if ( sender instanceof Player )
                {
                        Player p = (Player) sender;                        
                        if ( cmd.getName().equalsIgnoreCase("ultrachat") )
                        {
                                if ( args.length == 0 )
                                {
                                        p.sendMessage("UltraChat is an effective Chat Plugin.");
                                        p.sendMessage("  made by prosicraft");
                                        p.sendMessage(" Command: /uc format [format] - Changes the global format");
                                        p.sendMessage(" Command: /uc prefix <group> [text] - Changes a groups prefix");
                                        p.sendMessage(" Command: /uc suffix <group> [text] - Changes a groups suffix");
                                        p.sendMessage(" Command: /uc user <user> - User summary");
                                        p.sendMessage(" Command: /uc reload - Reload Configuration");
                                        return true;
                                }
                                else if ( args.length >= 2 )
                                {
                                        if ( args[0].equalsIgnoreCase("format") )
                                        {
                                                if ( !p.hasPermission("ultrachat.format.change") ) return np(p);
                                                                                                        
                                                // fix unwanted parse of spaces
                                                String thef = args[1];
                                                for ( int i=2;i<args.length;i++ )
                                                        thef += " " + args[i];
                                                
                                                if (thef.equalsIgnoreCase("clear")) {
                                                        format = "";
                                                        p.sendMessage(ChatColor.DARK_GREEN + "Cleared global format.");
                                                        save();
                                                        return true;
                                                }

                                                // notificate
                                                p.sendMessage("Setting format to '" + thef + "'.");
                                                format = thef;
                                                save();
                                                return true;
                                        }
                                        else if ( args[0].equalsIgnoreCase("user") && args.length == 2 )
                                        {
                                                if ( !p.hasPermission("ultrachat.user") ) return np(p);
                                                p.sendMessage (ChatColor.DARK_GRAY + "User summary of " + ChatColor.LIGHT_PURPLE + p.getName() + ChatColor.DARK_GRAY + ":");
                                                p.sendMessage (ChatColor.DARK_GRAY + "Preview: " + this.parseformat("This is &bthe Preview.", p));
                                                try
                                                {
                                                        String thegs = "";
                                                        for (String gs : perms.getPlayerGroups(p))
                                                        {
                                                                if (prefixes.containsKey(gs) && suffixes.containsKey(gs))
                                                                        thegs += ChatColor.WHITE + gs + " ";
                                                                else if (prefixes.containsKey(gs))
                                                                        thegs += ChatColor.GRAY + gs + " ";
                                                                else
                                                                        thegs += ChatColor.DARK_GRAY + gs + " ";
                                                        }
                                                        p.sendMessage (ChatColor.DARK_GRAY + "Groups: " + thegs);
                                                        
                                                } catch (UnsupportedOperationException uoex) {
                                                        p.sendMessage (ChatColor.DARK_GRAY + "Groups not supported.");
                                                }
                                                return true;
                                        }
                                        else if ( args[0].equalsIgnoreCase("prefix") && args.length >= 3 )
                                        {
                                                if ( !p.hasPermission("ultrachat.prefix.change") ) return np(p);
                                                // fix unwanted parse of spaces
                                                String theg = args[1];
                                                String thef = args[2];
                                                for ( int i=3;i<args.length;i++ )
                                                        thef += " " + args[i];
                                                
                                                if (thef.equalsIgnoreCase("clear")) {
                                                        if (prefixes.containsKey(theg))
                                                                prefixes.remove(theg);
                                                        p.sendMessage(ChatColor.DARK_GREEN + "Cleared prefix of group " + ChatColor.GRAY + theg + ChatColor.GREEN + ".");
                                                        save();
                                                        return true;
                                                }
                                                
                                                // notificate
                                                p.sendMessage("Setting prefix of group '" + theg + "' to '" + thef + "'.");
                                                if ( prefixes.containsKey(theg) )
                                                        prefixes.remove(theg);
                                                prefixes.put(theg, thef);
                                                save();
                                                return true;
                                        }
                                        else if ( args[0].equalsIgnoreCase("prefix") && args.length == 2 )
                                        {
                                                if ( !p.hasPermission("ultrachat.prefix.show") ) return np(p);
                                                String theg = args[1];
                                                if ( prefixes.containsKey(theg) )                                                
                                                        p.sendMessage(ChatColor.DARK_GRAY + "Prefix of group " + ChatColor.GRAY + theg + ChatColor.DARK_GRAY + ": " + ChatColor.WHITE + prefixes.get(theg));
                                                else
                                                        p.sendMessage(ChatColor.RED + "Prefix of group " + ChatColor.GRAY + theg + ChatColor.RED + " not set!");
                                                return true;                                                
                                        }
                                        else if ( args[0].equalsIgnoreCase("suffix") && args.length >= 3 )
                                        {
                                                if ( !p.hasPermission("ultrachat.suffix.change") ) return np(p);
                                                // fix unwanted parse of spaces
                                                String theg = args[1];
                                                String thef = args[2];
                                                for ( int i=3;i<args.length;i++ )
                                                        thef += " " + args[i];
                                                
                                                if (thef.equalsIgnoreCase("clear")) {
                                                        if (suffixes.containsKey(theg))
                                                                suffixes.remove(theg);
                                                        p.sendMessage(ChatColor.DARK_GREEN + "Cleared suffix of group " + ChatColor.GRAY + theg + ChatColor.GREEN + ".");
                                                        save();
                                                        return true;
                                                }
                                                
                                                // notificate
                                                p.sendMessage("Setting suffix of group '" + theg + "' to '" + thef + "'.");
                                                if ( suffixes.containsKey(theg) )
                                                        suffixes.remove(theg);
                                                suffixes.put(theg, thef);
                                                save();
                                                return true;
                                        }
                                        else if ( args[0].equalsIgnoreCase("suffix") && args.length == 2 )
                                        {
                                                if ( !p.hasPermission("ultrachat.suffix.show") ) return np(p);
                                                String theg = args[1];
                                                if ( suffixes.containsKey(theg) )                                                
                                                        p.sendMessage(ChatColor.DARK_GRAY + "Suffix of group " + ChatColor.GRAY + theg + ChatColor.DARK_GRAY + ": " + ChatColor.WHITE + suffixes.get(theg));
                                                else
                                                        p.sendMessage(ChatColor.RED + "Suffix of group " + ChatColor.GRAY + theg + ChatColor.RED + " not set!");
                                                return true;                                                
                                        }
                                }
                                else if ( args.length == 1 )
                                {
                                        if ( args[0].equalsIgnoreCase("format") )
                                        {
                                                if ( !p.hasPermission("ultrachat.format.show") ) return np(p);
                                                p.sendMessage (ChatColor.DARK_GRAY + "Global Format: " + ChatColor.WHITE + format);
                                                return true;
                                        }
                                        else if ( args[0].equalsIgnoreCase("reload") )
                                        {
                                                if ( !p.hasPermission("ultrachat.reload") ) return np(p);
                                                load();
                                                p.sendMessage (ChatColor.DARK_GRAY + "Configuration reload... " + ChatColor.GREEN + "DONE");
                                                return true;
                                        }
                                        else if ( args[0].equalsIgnoreCase("enable") )
                                        {
                                                if ( !p.hasPermission("ultrachat.enabledisable") ) return np(p);
                                                this.enabled = true;
                                                p.sendMessage (ChatColor.DARK_GRAY + "Enabling UltraChat... " + ChatColor.GREEN + "DONE");
                                                return true;
                                        }
                                        else if ( args[0].equalsIgnoreCase("disable") )
                                        {
                                                if ( !p.hasPermission("ultrachat.enabledisable") ) return np(p);
                                                this.enabled = false;
                                                p.sendMessage (ChatColor.DARK_GRAY + "Disabling UltraChat... " + ChatColor.GREEN + "DONE");
                                                return true;
                                        }
                                }
                        }
                }
                
                return false;
        }
        
        public boolean np (Player p) {
                p.sendMessage (ChatColor.DARK_GRAY + "Permission " + ChatColor.RED + "DENIED" + ChatColor.DARK_GRAY + "!"); return true;
        }
}
