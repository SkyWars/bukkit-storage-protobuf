/*
 * Copyright (C) 2016 Dabo Ross <http://www.daboross.net/>
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
package net.daboross.bukkitdev.bukkitstorageprotobuf.operations;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.AbstractWorld;
import java.util.logging.Level;
import net.daboross.bukkitdev.bukkitstorageprotobuf.ProtobufStatic;
import org.bukkit.World;

public class WorldEditAreaClearOperation extends AreaClearOperation {

    private final AbstractWorld editWorld;

    public WorldEditAreaClearOperation(final int operationSize, final World targetWorld, final AbstractWorld editWorld, final int targetZeroX, final int targetZeroY, final int targetZeroZ, final int lengthX, final int lengthY, final int lengthZ) {
        super(operationSize, targetWorld, targetZeroX, targetZeroY, targetZeroZ, lengthX, lengthY, lengthZ);
        this.editWorld = editWorld;
    }

    @Override
    protected void clearBlock(final int x, final int y, final int z) {
        // 0 = hardcoded Material.AIR
        try {
            editWorld.setBlock(new Vector(targetZeroX + x, targetZeroY + y, targetZeroZ + z), new BaseBlock(0), false);
        } catch (WorldEditException ex) {
            if (ProtobufStatic.isDebug()) {
                ProtobufStatic.log(Level.WARNING, "Failed to clear block", ex);
            }
        }
    }
}
