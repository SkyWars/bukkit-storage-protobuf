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
package net.daboross.bukkitdev.bukkitstorageprotobuf;

import com.sk89q.worldedit.world.AbstractWorld;
import net.daboross.bukkitdev.bukkitstorageprotobuf.operations.AreaClearOperation;
import net.daboross.bukkitdev.bukkitstorageprotobuf.operations.WorldEditAreaClearOperation;
import org.bukkit.World;

public class AreaClearing {

    public MultiPartOperation clearMultiPart(World world, int zeroX, int zeroY, int zeroZ, int lengthX, int lengthY, int lengthZ, int operationSize) {
        return new AreaClearOperation(operationSize, world, zeroX, zeroY, zeroZ, lengthX, lengthY, lengthZ);
    }

    public MultiPartOperation clearMultiPartWorldEdit(World world, AbstractWorld editWorld, int zeroX, int zeroY, int zeroZ, int lengthX, int lengthY, int lengthZ, int operationSize) {
        return new WorldEditAreaClearOperation(operationSize, world, editWorld, zeroX, zeroY, zeroZ, lengthX, lengthY, lengthZ);
    }
}
