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

import net.daboross.bukkitdev.bukkitstorageprotobuf.MultiPartOperation;
import net.daboross.bukkitdev.bukkitstorageprotobuf.compiled.BlockStorage;
import org.apache.commons.lang.ObjectUtils;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public class AreaClearOperation implements MultiPartOperation {

    protected final World targetWorld;
    protected final int targetZeroX;
    protected final int targetZeroY;
    protected final int targetZeroZ;
    private final int lengthX;
    private final int lengthY;
    private final int lengthZ;
    private final int operationSize;
    private int operationsLeft;
    private int nextOperationStartX;
    private int nextOperationStartY;
    private int nextOperationStartZ;
    private boolean newSetTypeSupported;

    public AreaClearOperation(int operationSize, World targetWorld, int targetZeroX, int targetZeroY, int targetZeroZ,
                              final int lengthX, final int lengthY, final int lengthZ) {
        this.operationSize = operationSize;
        this.targetWorld = targetWorld;
        this.targetZeroX = targetZeroX;
        this.targetZeroY = targetZeroY;
        this.targetZeroZ = targetZeroZ;
        this.lengthX = lengthX;
        this.lengthY = lengthY;
        this.lengthZ = lengthZ;
        int totalBlocks = lengthX * lengthY * lengthZ;
        this.operationsLeft = (int) Math.ceil(((double) totalBlocks) / ((double) operationSize));
        this.nextOperationStartX = 0;
        this.nextOperationStartY = 0;
        this.nextOperationStartZ = 0;
        try {
            this.newSetTypeSupported = Block.class.getMethod("setType", Material.class, boolean.class) != null;
        } catch (NoSuchMethodException e) {
            this.newSetTypeSupported = false;
        }
    }

    protected void clearBlock(final int x, final int y, final int z) {
        if (this.newSetTypeSupported) {
            targetWorld.getBlockAt(targetZeroX + x, targetZeroY + y, targetZeroZ + z).setType(Material.AIR, false);
        } else {
            // this will only be called on 1.7 servers
            targetWorld.getBlockAt(targetZeroX + x, targetZeroY + y, targetZeroZ + z).setTypeId(0, false);
        }
    }

    @Override
    public int getPartsLeft() {
        return operationsLeft;
    }

    @Override
    public void performNextPart() {
        if (operationsLeft <= 0) {
            return;
        }
        int endX = -1;
        int endZ = -1;
        int endY = -1;
        int blocksLeft = operationSize;
        outer_loop:
        for (int x = nextOperationStartX; x < lengthX; x++) {
            int startZ;
            if (x == nextOperationStartX) {
                startZ = nextOperationStartZ;
            } else {
                startZ = 0;
            }
            for (int z = startZ; z < lengthZ; z++) {
                int startY;
                if (x == nextOperationStartX && z == nextOperationStartZ) {
                    startY = nextOperationStartY;
                } else {
                    startY = 0;
                }
                for (int y = startY; y < lengthY; y++) {
                    if (blocksLeft <= 0) {
                        endY = y;
                        endZ = z;
                        endX = x;
                        break outer_loop;
                    }
                    clearBlock(x, y, z);
                    blocksLeft -= 1;
                }
            }
        }
        if (endX == -1) {
            operationsLeft = 0;
        } else {
            operationsLeft -= 1;
        }
        nextOperationStartX = endX;
        nextOperationStartY = endY;
        nextOperationStartZ = endZ;
    }
}
