package pl.extollite.queuewaterdog.config;

import java.util.List;

/**
 * Lang
 */
public class Config {

    public static String
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
            queueMoveDelay,
            queuePriorityChance;

    public static boolean positionMessageOnHotBar,
            enableKickMessage,
            alwaysQueue;
}
