package pl.extollite.queuewaterdog.listener;

import io.github.waterfallmc.waterfall.event.ProxyQueryEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import pl.extollite.queuewaterdog.config.Config;

public class QueryEventListener implements Listener {
    String version;
    @EventHandler
    public void onPing(ProxyQueryEvent event) {
        if (!Config.customVersion.contains("false")) {
            version = Config.customVersion.replaceAll("&", "§");
            event.getResult().setVersion(version);
        }
    }

}
