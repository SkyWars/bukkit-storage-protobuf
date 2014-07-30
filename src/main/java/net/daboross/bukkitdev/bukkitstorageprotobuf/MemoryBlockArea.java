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

import com.sk89q.worldedit.LocalWorld;
import com.sk89q.worldedit.Vector;
import java.io.IOException;
import net.daboross.bukkitdev.bukkitstorageprotobuf.compiled.BlockStorage;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.InventoryHolder;

public class MemoryBlockArea {

    public final int lengthX;
    public final int lengthY;
    public final int lengthZ;
    public final BlockStorage.Block[][][] blocks;

    public MemoryBlockArea(BlockStorage.BlockArea area) throws InvalidBlockAreaException {
        this.lengthY = area.getPlainCount();
        this.lengthX = area.getPlain(0).getRowCount();
        this.lengthZ = area.getPlain(0).getRow(0).getBlockCount();
        blocks = new BlockStorage.Block[lengthY][lengthX][lengthZ];
        for (int y = 0; y < area.getPlainCount(); y++) {
            BlockStorage.BlockPlain storedPlain = area.getPlain(y);
            if (storedPlain.getRowCount() != this.lengthX) {
                throw new InvalidBlockAreaException("Inconsistent x length");
            }
            for (int x = 0; x < storedPlain.getRowCount(); x++) {
                BlockStorage.BlockRow storedRow = storedPlain.getRow(x);
                if (storedRow.getBlockCount() != this.lengthZ) {
                    throw new InvalidBlockAreaException("Inconsistent z length");
                }
                for (int z = 0; z < storedRow.getBlockCount(); z++) {
                    blocks[y][x][z] = storedRow.getBlock(z);
                }
            }
        }
    }

    public void apply(World world, int zeroX, int zeroY, int zeroZ) {
        for (int y = 0; y < lengthY; y++) {
            for (int x = 0; x < lengthX; x++) {
                for (int z = 0; z < lengthZ; z++) {
                    BlockStorage.Block storedBlock = blocks[y][x][z];
                    if (storedBlock.getId() != 0) {
                        Block block = world.getBlockAt(zeroX + x, zeroY + y, zeroZ + z);
                        if (storedBlock.hasData()) {
                            //noinspection deprecation
                            block.setTypeIdAndData(storedBlock.getId(), (byte) storedBlock.getData(), false);
                        } else {
                            //noinspection deprecation
                            block.setTypeId(storedBlock.getId(), false);
                        }
                        if (storedBlock.hasInventory()) {
                            BlockState state = block.getState();
                            if (state instanceof InventoryHolder) {
                                ((InventoryHolder) state).getInventory().setContents(ProtobufStorage.decodeInventory(storedBlock.getInventory()));
                            }
                            // TODO: Should we print a warning here if there's a stored inventory on a non-inventory-holder block?
                        }
                    }
                }
            }
        }
    }

    public void applyWorldEdit(World bukkitWorld, LocalWorld editWorld, int zeroX, int zeroY, int zeroZ) {
        for (int y = 0; y < lengthY; y++) {
            for (int x = 0; x < lengthX; x++) {
                for (int z = 0; z < lengthZ; z++) {
                    BlockStorage.Block storedBlock = blocks[y][x][z];
                    if (storedBlock.getId() != 0) {
                        Vector position = new Vector(zeroX + x, zeroY + y, zeroZ + z);
                        if (storedBlock.hasData()) {
                            //noinspection deprecation
                            editWorld.setTypeIdAndDataFast(position, storedBlock.getId(), (byte) storedBlock.getData());
                        } else {
                            //noinspection deprecation
                            editWorld.setBlockTypeFast(position, storedBlock.getId());
                        }
                        if (storedBlock.hasInventory()) {
                            Block block = bukkitWorld.getBlockAt(zeroX + x, zeroY + y, zeroZ + z);
                            BlockState state = block.getState();
                            if (state instanceof InventoryHolder) {
                                ((InventoryHolder) state).getInventory().setContents(ProtobufStorage.decodeInventory(storedBlock.getInventory()));
                            }
                            // TODO: Should we print a warning here if there's a stored inventory on a non-inventory-holder block?
                        }
                    }
                }
            }
        }
    }

    private class InvalidBlockAreaException extends IOException {

        public InvalidBlockAreaException(final String str) {
            super(str);
        }
    }
}
