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

import net.daboross.bukkitdev.bukkitstorageprotobuf.ChestProvider;
import net.daboross.bukkitdev.bukkitstorageprotobuf.MemoryBlockArea;
import net.daboross.bukkitdev.bukkitstorageprotobuf.MultiPartOperation;
import org.bukkit.World;

public class MemoryBlockAreaCopyOperation implements MultiPartOperation {

    protected final MemoryBlockArea area;
    protected final World targetWorld;
    protected final int targetZeroX;
    protected final int targetZeroY;
    protected final int targetZeroZ;
    /**
     * lengthX == area.lengthX. It's nice to have this separate though for testing purposes!
     */
    private final int lengthX;
    private final int lengthY;
    private final int lengthZ;
    protected final ChestProvider chestProvider;
    private final int operationSize;
    private int operationsLeft;
    private int nextOperationStartX;
    private int nextOperationStartY;
    private int nextOperationStartZ;

    public MemoryBlockAreaCopyOperation(MemoryBlockArea area, int operationSize, World targetWorld,
                                        int targetZeroX, int targetZeroY, int targetZeroZ, final ChestProvider chestProvider) {
        this.area = area;
        this.operationSize = operationSize;
        this.targetWorld = targetWorld;
        this.targetZeroX = targetZeroX;
        this.targetZeroY = targetZeroY;
        this.targetZeroZ = targetZeroZ;
        this.lengthX = area.lengthX;
        this.lengthY = area.lengthY;
        this.lengthZ = area.lengthZ;
        this.chestProvider = chestProvider;
        int totalBlocks = lengthX * lengthY * lengthZ;
        this.operationsLeft = (int) Math.ceil(((double) totalBlocks) / ((double) operationSize));
        this.nextOperationStartX = 0;
        this.nextOperationStartY = 0;
        this.nextOperationStartZ = 0;
    }

    /**
     * Testing use only!
     */
    protected MemoryBlockAreaCopyOperation(MemoryBlockArea area, int operationSize, World targetWorld, int targetZeroX, int targetZeroY, int targetZeroZ, final ChestProvider chestProvider,
                                           int lengthX, int lengthY, int lengthZ) {
        this.area = area;
        this.operationSize = operationSize;
        this.targetWorld = targetWorld;
        this.targetZeroX = targetZeroX;
        this.targetZeroY = targetZeroY;
        this.targetZeroZ = targetZeroZ;
        this.lengthX = lengthX;
        this.lengthY = lengthY;
        this.lengthZ = lengthZ;
        this.chestProvider = chestProvider;
        int totalBlocks = lengthX * lengthY * lengthZ;
        this.operationsLeft = (int) Math.ceil(((double) totalBlocks) / ((double) operationSize));
        this.nextOperationStartX = 0;
        this.nextOperationStartY = 0;
        this.nextOperationStartZ = 0;
    }

    protected void copyBlock(final int x, final int y, final int z) {
        area.copyBlock(targetWorld, targetZeroX, targetZeroY, targetZeroZ, chestProvider, x, y, z);
    }

    @Override
    public int getPartsLeft() {
        return operationsLeft;
    }

    // TODO: Need unit tests on this! Ensuring copyBlock is called once and only once if this is performed totalOperations times.
    @Override
    public void performNextPart() {
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
                    copyBlock(x, y, z);
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
