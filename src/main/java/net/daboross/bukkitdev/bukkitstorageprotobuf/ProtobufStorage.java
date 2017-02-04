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

import java.util.Map;
import net.daboross.bukkitdev.bukkitstorageprotobuf.compiled.BlockStorage;
import org.bukkit.Color;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CommandBlock;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@SuppressWarnings("deprecation")
public class ProtobufStorage {

    private ProtobufStorage() {
    }

    public static BlockStorage.BlockArea encode(World world, int zeroX, int zeroY, int zeroZ, int xLength, int yLength, int zLength, boolean saveBlockInventories) {
        BlockStorage.BlockArea.Builder areaBuilder = BlockStorage.BlockArea.newBuilder();
        for (int y = 0; y < yLength; y++) {
            BlockStorage.BlockPlane.Builder planeBuilder = BlockStorage.BlockPlane.newBuilder();
            for (int x = 0; x < xLength; x++) {
                BlockStorage.BlockRow.Builder rowBuilder = BlockStorage.BlockRow.newBuilder();
                for (int z = 0; z < zLength; z++) {
                    Block block = world.getBlockAt(zeroX + x, zeroY + y, zeroZ + z);

                    BlockStorage.Block.Builder blockBuilder = BlockStorage.Block.newBuilder()
                            .setId(block.getTypeId())
                            .setData(block.getData());

                    if (saveBlockInventories) {
                        BlockState state = block.getState();
                        if (state instanceof InventoryHolder) {
                            ItemStack[] contents = ((InventoryHolder) state).getInventory().getContents();
                            blockBuilder.setInventory(encodeInventory(contents));
                        }
                        if (state instanceof CommandBlock) {
                            BlockStorage.BlockCommandData.Builder commandBuilder = BlockStorage.BlockCommandData.newBuilder();
                            String command = ((CommandBlock) state).getCommand();
                            commandBuilder.setCommand(command);
                            String name = ((CommandBlock) state).getName();
                            if (name != null) {
                                commandBuilder.setName(name);
                            }
                            blockBuilder.setCommand(commandBuilder);
                        }
                    }
                    rowBuilder.addBlock(blockBuilder);
                }
                planeBuilder.addRow(rowBuilder);
            }
            areaBuilder.addPlane(planeBuilder);
        }
        return areaBuilder.build();
    }

    @SuppressWarnings("deprecation")
    public static BlockStorage.BlockInventory encodeInventory(ItemStack[] contents) {
        BlockStorage.BlockInventory.Builder inventoryBuilder = BlockStorage.BlockInventory.newBuilder();
        inventoryBuilder.setLength(contents.length);

        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (stack == null) {
                continue;
            }
            BlockStorage.InventoryItem.Builder itemBuilder = BlockStorage.InventoryItem.newBuilder();
            // id and location
            itemBuilder.setId(stack.getTypeId()).setLocation(i);
            // amount
            itemBuilder.setAmount(stack.getAmount());
            // data
            byte data = stack.getData().getData();
            if (data != 0) {
                itemBuilder.setData(data);
            }
            // durability
            int durability = stack.getDurability();
            if (durability > 0) {
                itemBuilder.setDurability(durability);
            }
            // Item meta
            ItemMeta meta = stack.getItemMeta();
            // Item name
            String name = meta.getDisplayName();
            if (name != null) {
                itemBuilder.setName(name);
            }
            // Lore
            if (meta.hasLore()) {
                itemBuilder.addAllLore(meta.getLore());
            }
            // Enchantments
            if (meta.hasEnchants()) {
                Map<Enchantment, Integer> enchantmentMap = meta.getEnchants();
                for (Map.Entry<Enchantment, Integer> entry : enchantmentMap.entrySet()) {
                    itemBuilder.addEnchantment(BlockStorage.ItemEnchantment.newBuilder()
                            .setId(entry.getKey().getId())
                            .setLevel(entry.getValue()));
                }
            }
            // Custom potion data
            if (meta instanceof PotionMeta) {
                PotionMeta potionMeta = (PotionMeta) meta;
                if (potionMeta.hasCustomEffects()) {
                    for (PotionEffect effect : potionMeta.getCustomEffects()) {
                        itemBuilder.addExtraPotionEffects(BlockStorage.ExtraPotionEffect.newBuilder()
                                .setId(effect.getType().getId())
                                .setDuration(effect.getDuration())
                                .setAmplifier(effect.getAmplifier())
                                .setAmbient(effect.isAmbient())
                                .setParticles(effect.hasParticles()));
                    }
                }
            }
            // Catch-all potion transfer
            CrossPotions.extractData(stack).saveTo(itemBuilder);
            // Leather armor
            if (meta instanceof LeatherArmorMeta) {
                itemBuilder.setLeatherArmorColorRbg(((LeatherArmorMeta) meta).getColor().asRGB());
            }
            inventoryBuilder.addItem(itemBuilder);
        }
        return inventoryBuilder.build();
    }

    @SuppressWarnings("deprecation")
    public static ItemStack[] decodeInventory(BlockStorage.BlockInventory storedInventory) {
        ItemStack[] result = new ItemStack[storedInventory.getLength()];

        for (BlockStorage.InventoryItem storedItem : storedInventory.getItemList()) {
            ItemStack itemStack = new ItemStack(storedItem.getId());
            if (storedItem.hasAmount()) {
                itemStack.setAmount(storedItem.getAmount());
            }
            if (storedItem.hasData()) {
                itemStack.getData().setData((byte) storedItem.getData());
            }
            if (storedItem.hasDurability()) {
                itemStack.setDurability((short) storedItem.getDurability());
            }
            // This isn't down below because there isn't a boolean condition for this:
            // This may be needed even if storedItem does not have a main potion effect:
            // The potion could be stored as raw data and need to be transferred to modern data.
            CrossPotions.extractData(storedItem).applyTo(itemStack);

            boolean hasName = storedItem.hasName();
            boolean hasLore = storedItem.getLoreCount() > 0;
            boolean hasEnchants = storedItem.getEnchantmentCount() > 0;
            boolean hasArmorColor = storedItem.hasLeatherArmorColorRbg();
            boolean hasExtraPotionEffects = storedItem.getExtraPotionEffectsCount() > 0;
            if (hasName || hasLore || hasEnchants || hasArmorColor || hasExtraPotionEffects) {
                ItemMeta meta = itemStack.getItemMeta();
                if (hasName) {
                    meta.setDisplayName(storedItem.getName());
                }
                if (hasLore) {
                    meta.setLore(storedItem.getLoreList());
                }
                if (hasEnchants) {
                    for (BlockStorage.ItemEnchantment storedEnchant : storedItem.getEnchantmentList()) {
                        meta.addEnchant(Enchantment.getById(storedEnchant.getId()), storedEnchant.getLevel(), true);
                    }
                }
                if (hasExtraPotionEffects && meta instanceof PotionMeta) {
                    for (BlockStorage.ExtraPotionEffect effect : storedItem.getExtraPotionEffectsList()) {
                        ((PotionMeta) meta).addCustomEffect(new PotionEffect(PotionEffectType.getById(effect.getId()),
                                        effect.getDuration(), effect.getAmplifier(),
                                        effect.getAmbient(), effect.getParticles()),
                                true);
                    }
                }
                if (hasArmorColor && meta instanceof LeatherArmorMeta) {
                    ((LeatherArmorMeta) meta).setColor(Color.fromRGB(storedItem.getLeatherArmorColorRbg()));
                }
                itemStack.setItemMeta(meta);
            }
            result[storedItem.getLocation()] = itemStack;
        }
        return result;
    }
}
