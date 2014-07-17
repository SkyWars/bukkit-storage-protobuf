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

import net.daboross.bukkitdev.bukkitstorageprotobuf.compiled.BlockStorage;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class ProtobufStorage {

    public BlockStorage.BlockArea encode(World world, int zeroX, int zeroY, int zeroZ, int xLength, int yLength, int zLength, boolean saveBlockInventories) {
        BlockStorage.BlockArea.Builder areaBuilder = BlockStorage.BlockArea.newBuilder();
        for (int y = 0; y < yLength; y++) {
            BlockStorage.BlockPlain.Builder plainBuilder = BlockStorage.BlockPlain.newBuilder();
            for (int x = 0; x < xLength; x++) {
                BlockStorage.BlockRow.Builder rowBuilder = BlockStorage.BlockRow.newBuilder();
                for (int z = 0; z < zLength; z++) {
                    Block block = world.getBlockAt(zeroX + x, zeroY + y, zeroZ + z);

                    //noinspection deprecation
                    BlockStorage.Block.Builder blockBuilder = BlockStorage.Block.newBuilder()
                            .setId(block.getTypeId())
                            .setData(block.getData());

                    if (saveBlockInventories) {
                        BlockState state = block.getState();
                        if (state instanceof InventoryHolder) {
                            ItemStack[] contents = ((InventoryHolder) state).getInventory().getContents();
                            blockBuilder.setInventory(encodeInventory(contents));
                        }
                    }
                    rowBuilder.addBlock(blockBuilder);
                }
                plainBuilder.addRow(rowBuilder);
            }
            areaBuilder.addPlain(plainBuilder);
        }
        return areaBuilder.build();
    }

    public void apply(BlockStorage.BlockArea storedArea, World world, int zeroX, int zeroY, int zeroZ) {
        for (int y = 0; y < storedArea.getPlainCount(); y++) {
            BlockStorage.BlockPlain storedPlain = storedArea.getPlain(y);
            for (int x = 0; x < storedPlain.getRowCount(); x++) {
                BlockStorage.BlockRow storedRow = storedPlain.getRow(x);
                for (int z = 0; z < storedRow.getBlockCount(); z++) {
                    BlockStorage.Block storedBlock = storedRow.getBlock(z);
                    if (storedBlock.getId() != 0) {
                        Block block = world.getBlockAt(zeroX + x, zeroY + y, zeroZ + z);
                        if (storedBlock.hasData()) {
                            //noinspection deprecation
                            block.setTypeIdAndData(storedBlock.getId(), (byte) storedBlock.getData(), false);
                        } else {
                            //noinspection deprecation
                            block.setTypeId(storedBlock.getId(), false);
                        }
                    }
                }
            }
        }
    }

    public BlockStorage.BlockInventory encodeInventory(ItemStack[] contents) {
        BlockStorage.BlockInventory.Builder inventoryBuilder = BlockStorage.BlockInventory.newBuilder();
        inventoryBuilder.setLength(contents.length);

        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (stack == null) {
                System.out.println("Stack was null in contents!");
                continue;
            }
            BlockStorage.InventoryItem.Builder itemBuilder =
                    BlockStorage.InventoryItem.newBuilder()
                            .setId(stack.getTypeId())
                            .setLocation(i);
            int amount = stack.getAmount();
            if (amount != 1) {
                itemBuilder.setAmount(amount);
            }
            byte data = stack.getData().getData();
            if (data != 0) {
                itemBuilder.setData(data);
            }
            inventoryBuilder.addItem(itemBuilder);
        }
        return inventoryBuilder.build();
    }

    public ItemStack[] decodeInventory(BlockStorage.BlockInventory storedInventory) {
        ItemStack[] result = new ItemStack[storedInventory.getLength()];

        for (BlockStorage.InventoryItem storedItem : storedInventory.getItemList()) {
            ItemStack itemStack = new ItemStack(storedItem.getId());
            if (storedItem.hasAmount()) {
                itemStack.setAmount(storedItem.getAmount());
            }
            if (storedItem.hasData()) {
                itemStack.getData().setData((byte) storedItem.getData());
            }
            result[storedItem.getLocation()] = itemStack;
        }
        return result;
    }
}
