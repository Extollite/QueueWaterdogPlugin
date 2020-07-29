package pl.extollite.queuewaterdog.config;

import java.util.List;

/**
 * Lang
 */
public class Config {

    public static String regex,
            serverFullMessage,
            queuePosition,
            kickMessage,
            serverDownKickMessage,
            customVersion,
            queueServer,
            mainServer,
            joinMainServerMessage,
            queueBypassPermission,
            queuePriorityPermission,
            adminPermission;
    public static int mainServerSlots,
            queueServerSlots,
            queueMoveDelay,
            queuePriorityChance;

    public static boolean positionMessageOnHotBar,
            enableKickMessage,
            alwaysQueue;
}
