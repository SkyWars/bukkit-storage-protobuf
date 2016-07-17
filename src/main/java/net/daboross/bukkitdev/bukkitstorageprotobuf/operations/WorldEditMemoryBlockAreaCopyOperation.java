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

import com.sk89q.worldedit.world.AbstractWorld;
import net.daboross.bukkitdev.bukkitstorageprotobuf.ChestProvider;
import net.daboross.bukkitdev.bukkitstorageprotobuf.MemoryBlockArea;
import org.bukkit.World;

public class WorldEditMemoryBlockAreaCopyOperation extends MemoryBlockAreaCopyOperation {

    private final AbstractWorld editWorld;

    public WorldEditMemoryBlockAreaCopyOperation(final MemoryBlockArea area, final int operationSize, final World targetWorld, final AbstractWorld editWorld, final int targetZeroX, final int targetZeroY, final int targetZeroZ, final ChestProvider chestProvider) {
        super(area, operationSize, targetWorld, targetZeroX, targetZeroY, targetZeroZ, chestProvider);
        this.editWorld = editWorld;
    }

    @Override
    protected void copyBlock(final int x, final int y, final int z) {
        area.copyBlockWorldEdit(targetWorld, editWorld, targetZeroX, targetZeroY, targetZeroZ, chestProvider, x, y, z);
    }
}
