/*
 * Copyright (C) 2013 Dabo Ross <http://www.daboross.net/>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.daboross.bukkitdev.bukkitstorageprotobuf;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;

public class ProtobufStatic {

    private static final Logger defaultLogger = Bukkit.getLogger();
    private static boolean debug = false;
    private static Logger logger = null;

    private ProtobufStatic() {
    }

    public static void setLogger(Logger logger) {
        ProtobufStatic.logger = logger == null ? defaultLogger : logger;
    }

    public static void setDebug(boolean debug) {
        ProtobufStatic.debug = debug;
    }

    /**
     * @param message Message to log (String.format-type formatting)
     * @param args    arguments to pass to String.format()
     */
    public static void debug(String message, Object... args) {
        if (debug) {
            logger.log(Level.INFO, String.format(message, args));
        }
    }

    /**
     * @param level   Level to log at
     * @param message Message to log (logger.log-type formatting)
     * @param args    arguments to pass to logger.log()
     */
    public static void log(Level level, String message, Object... args) {
        logger.log(level, message, args);
    }

    /**
     * @param level   Level to log at
     * @param message Message to log (no formatting)
     * @param ex      throwable to pass to logger.log()
     */
    public static void log(Level level, String message, Throwable ex) {
        logger.log(level, message, ex);
    }
}
