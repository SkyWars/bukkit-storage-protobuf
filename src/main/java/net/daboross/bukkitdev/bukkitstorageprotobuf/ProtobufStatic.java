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

public class ProtobufStatic {
    private static Logger debugLogger = null;

    /**
     * Sets the debug logger to use for debug logging.
     * <p>
     * If null, debug logging will be disabled.
     *
     * @param debugLogger Logger to debug to, or null.
     */
    public static void setDebugLogger(Logger debugLogger) {
        ProtobufStatic.debugLogger = debugLogger;
    }

    public static void debug(String message, Object... args) {
        if (debugLogger != null) {
            debugLogger.log(Level.INFO, String.format(message, args));
        }
    }

}
