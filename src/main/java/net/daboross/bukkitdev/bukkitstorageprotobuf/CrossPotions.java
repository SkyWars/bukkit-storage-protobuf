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

import net.daboross.bukkitdev.bukkitstorageprotobuf.compiled.BlockStorage;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import java.util.logging.Level;

/**
 * Class to manage potions across multiple Minecraft versions.
 */
public class CrossPotions {
    public static final boolean modernApiSupported;
    private static final NoPotionData NO_POTION_DATA = new NoPotionData();

    static {
        boolean tempSupported;
        try {
            Class.forName("org.bukkit.potion.PotionData");
            tempSupported = true;
        } catch (ClassNotFoundException ignored) {
            tempSupported = false;
        }
        modernApiSupported = tempSupported;
    }


    public static CrossPotionData extractData(ItemStack item) {
        if (modernApiSupported) {
            return getDataModernApi(item);
        } else {
            return getDataRawData(item);
        }
    }


    private static CrossPotionData getDataModernApi(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof PotionMeta) {
            return new ModernPotionData(item.getType(), (PotionMeta) meta);
        } else {
            return NO_POTION_DATA;
        }
    }

    @SuppressWarnings("deprecation")
    private static CrossPotionData getDataRawData(ItemStack item) {
        if (item.getType() == Material.POTION) {
            return new RawDataPotionData(item.getData().getData());
        } else {
            return NO_POTION_DATA;
        }
    }

    @SuppressWarnings("deprecation")
    public static CrossPotionData extractData(BlockStorage.InventoryItem storedItem) {
        if (storedItem.hasMainPotionEffect()) {
            BlockStorage.MainPotionEffect storedEffect = storedItem.getMainPotionEffect();
            CrossPotionType type;
            try {
                type = CrossPotionType.valueOf(storedEffect.getName());
            } catch (IllegalArgumentException ignored) {
                ProtobufStatic.debug("Stored arena contained unknown potion name: %s", storedEffect.getName());
                type = CrossPotionType.WATER;
            }
            return new ModernPotionData(type, storedEffect.getExtended(), storedEffect.getUpgraded(),
                    storedEffect.getSplash(), storedEffect.getLingering());
        } else {
            if (modernApiSupported) {
                if (storedItem.getId() == Material.POTION.getId()
                        || storedItem.getId() == Material.SPLASH_POTION.getId()
                        || storedItem.getId() == Material.LINGERING_POTION.getId()) {
                    // Convert old data into new data in prepartion for applying:
                    return new ModernPotionData((byte) storedItem.getData());
                } else {
                    return NO_POTION_DATA;
                }
            } else if (storedItem.getId() == Material.POTION.getId()) {
                // Data is stored as old data, and modern data isn't supported.
                // Just use old/raw data.
                return new RawDataPotionData((byte) storedItem.getData());
            } else {
                return NO_POTION_DATA;
            }
        }
    }

    public interface CrossPotionData {
        void applyTo(ItemStack item);

        void saveTo(BlockStorage.InventoryItem.Builder builder);
    }

    public static class ModernPotionData implements CrossPotionData {
        private final CrossPotionType potionType;
        private final boolean extended;
        private final boolean upgraded;
        private final boolean splash;
        private final boolean lingering;

        /**
         * Creates a modern potion data storage from the PotionMeta of an existing item. This method could take a plain
         * ItemStack, but in most cases when it's actually used it is more useful to be checking if meta instanceof PotionMeta
         * before the ModernPotionData is constructed, so the constructor has this form instead.
         *
         * @param meta The potion metadata to store as modern cross data storage.
         */
        public ModernPotionData(Material type, PotionMeta meta) {
            PotionData data = meta.getBasePotionData();
            this.potionType = CrossPotionType.fromBukkit(data.getType());
            this.extended = data.isExtended();
            this.upgraded = data.isUpgraded();
            switch (type) {
                case POTION:
                    this.splash = false;
                    this.lingering = false;
                    break;
                case SPLASH_POTION:
                    this.splash = true;
                    this.lingering = false;
                    break;
                case LINGERING_POTION:
                    this.splash = false;
                    this.lingering = true;
                    break;
                default:
                    // What the heck? Still, we should have sensible defaults.
                    this.splash = false;
                    this.lingering = false;
                    ProtobufStatic.debug("Unknown potion type used to create ModernPotionData: %s", type);
                    break;
            }
        }

        public ModernPotionData(CrossPotionType potionType, boolean extended, boolean upgraded, boolean splash, boolean lingering) {
            this.potionType = potionType;
            this.extended = extended;
            this.upgraded = upgraded;
            this.splash = splash;
            this.lingering = lingering && !splash; // This class has a fail-last kind of standpoint.
            // If the user tries to do something wrong, do the closest thing which is right.
        }

        /**
         * Converts old raw data potion storage to modern potion data.
         *
         * @param rawData The raw item data, from an item on a pre-1.9 server.
         */
        public ModernPotionData(int rawData) {
            // This is some byte manipulation to get from old data to modern data.
            CrossPotionType type;
            // first four bits determine type
            switch (rawData & 15) {
                case 1:
                    type = CrossPotionType.REGEN;
                    break;
                case 2:
                    type = CrossPotionType.SPEED;
                    break;
                case 3:
                    type = CrossPotionType.FIRE_RESISTANCE;
                    break;
                case 4:
                    type = CrossPotionType.POISON;
                    break;
                case 5:
                    type = CrossPotionType.INSTANT_HEAL;
                    break;
                case 6:
                    type = CrossPotionType.NIGHT_VISION;
                    break;
                case 8:
                    type = CrossPotionType.WEAKNESS;
                    break;
                case 9:
                    type = CrossPotionType.STRENGTH;
                    break;
                case 10:
                    type = CrossPotionType.SLOWNESS;
                    break;
                case 11:
                    type = CrossPotionType.JUMP;
                    break;
                case 12:
                    type = CrossPotionType.INSTANT_DAMAGE;
                    break;
                case 13:
                    type = CrossPotionType.WATER_BREATHING;
                    break;
                case 14:
                    type = CrossPotionType.INVISIBILITY;
                    break;
                default:
                    switch (rawData) {
                        case 0:
                            type = CrossPotionType.WATER;
                            break;
                        case 16:
                            type = CrossPotionType.AWKWARD;
                            break;
                        case 32:
                            type = CrossPotionType.THICK;
                            break;
                        case 64:
                        case 8192:
                            type = CrossPotionType.MUNDANE;
                            break;
                        default:
                            // This is an invalid potion! Make it water.
                            type = CrossPotionType.WATER;
                    }
            }
            boolean upgraded = (rawData & 32) == 32;
            boolean extended = (rawData & 64) == 64;
            // Technically we should check `((rawData & 8192) == 8192)` to detect a non-splash potion, but
            // since splash is only recorded as a boolean in the new API, we can get away with this.
            boolean splash = (rawData & 16384) == 16384;
            this.potionType = type;
            this.upgraded = upgraded && type.upgradeable;
            this.extended = extended && type.extendable;
            this.splash = splash;
            this.lingering = false; // Lingering potions didn't exist pre-1.9 (when this data format was valid).
        }

        @Override
        public void applyTo(ItemStack item) {
            if (modernApiSupported) {
                applyToModernApi(item);
            } else {
                applyToRawData(item);
            }
        }

        @Override
        public void saveTo(BlockStorage.InventoryItem.Builder item) {
            BlockStorage.MainPotionEffect.Builder potionBuilder = BlockStorage.MainPotionEffect.newBuilder();
            potionBuilder.setName(this.potionType.toString());
            potionBuilder.setExtended(this.extended);
            potionBuilder.setUpgraded(this.upgraded);
            potionBuilder.setSplash(this.splash);
            potionBuilder.setLingering(this.lingering);
            item.setMainPotionEffect(potionBuilder);
        }

        private void applyToModernApi(ItemStack item) {
            if (splash) {
                item.setType(Material.SPLASH_POTION);
            } else if (lingering) {
                item.setType(Material.LINGERING_POTION);
            } else {
                item.setType(Material.POTION);
            }
            ItemMeta bukkitMeta = item.getItemMeta();
            Validate.isTrue(bukkitMeta instanceof PotionMeta, "Cannot apply potion to non-potion item.");
            PotionMeta meta = (PotionMeta) item.getItemMeta();
            PotionData bukkitData = new PotionData(potionType.toBukkit(), extended, upgraded);
            meta.setBasePotionData(bukkitData);
            item.setItemMeta(meta);
        }

        @SuppressWarnings("deprecation")
        private void applyToRawData(ItemStack item) {
            if (item.getType() != Material.POTION) {
                throw new IllegalArgumentException("Cannot apply potion to non-potion item");
            }
            int rawData = 0;
            // type:
            switch (potionType) {
                case REGEN:
                    rawData |= 1;
                    break;
                case SPEED:
                    rawData |= 2;
                    break;
                case FIRE_RESISTANCE:
                    rawData |= 3;
                    break;
                case POISON:
                    rawData |= 4;
                    break;
                case INSTANT_HEAL:
                    rawData |= 5;
                    break;
                case NIGHT_VISION:
                    rawData |= 6;
                    break;
                case WEAKNESS:
                    rawData |= 8;
                    break;
                case STRENGTH:
                    rawData |= 9;
                    break;
                case SLOWNESS:
                    rawData |= 10;
                    break;
                case JUMP:
                    rawData |= 11;
                    break;
                case INSTANT_DAMAGE:
                    rawData |= 12;
                    break;
                case WATER_BREATHING:
                    rawData |= 13;
                    break;
                case INVISIBILITY:
                    rawData |= 14;
                    break;
                // special cases:
                case WATER:
                    rawData = 0;
                    break;
                case AWKWARD:
                    rawData = 16;
                    break;
                case THICK:
                    rawData = 32;
                    break;
                case MUNDANE:
                    if (extended) {
                        rawData = 64;
                    } else {
                        rawData = 8192;
                    }
                    break;
                case UNCRAFTABLE:
                    rawData |= 64 + 32;
                    break;
                default:
                    ProtobufStatic.debug("Couldn't find old-data equivalent for new-data potion type! %s", potionType);
                    break;
            }
            boolean upgraded = (rawData & 32) == 32;
            boolean extended = (rawData & 64) == 64;
            // Technically we should check `((rawData & 8192) == 8192)` to detect a non-splash potion, but
            // since splash is only recorded as a boolean in the new API, we can get away with this.
            boolean splash = (rawData & 16384) == 16384;
            if (upgraded) {
                rawData |= 32;
            }
            if (extended) {
                rawData |= 64;
            }
            if (!(potionType == CrossPotionType.WATER
                    || potionType == CrossPotionType.AWKWARD
                    || potionType == CrossPotionType.THICK
                    || potionType == CrossPotionType.MUNDANE)) {
                if (splash) {
                    rawData |= 16384;
                } else {
                    // The special cases can't be this.
                    rawData |= 8192;
                }
            }

            MaterialData data = item.getData();
            data.setData((byte) rawData);
            item.setData(data);
        }

    }

    /**
     * This is an enum that is available on all server versions, which mimics the Bukkit
     * {@link org.bukkit.potion.PotionType} class.
     */
    public enum CrossPotionType {
        UNCRAFTABLE(false, false),
        WATER(false, false),
        MUNDANE(false, false),
        THICK(false, false),
        AWKWARD(false, false),
        NIGHT_VISION(false, true),
        INVISIBILITY(false, true),
        JUMP(true, true),
        FIRE_RESISTANCE(false, true),
        SPEED(true, true),
        SLOWNESS(false, true),
        WATER_BREATHING(false, true),
        INSTANT_HEAL(true, false),
        INSTANT_DAMAGE(true, false),
        POISON(true, true),
        REGEN(true, true),
        STRENGTH(true, true),
        WEAKNESS(false, true),
        LUCK(false, false);

        public final boolean upgradeable;
        public final boolean extendable;

        CrossPotionType(boolean upgradeable, boolean extendable) {
            this.upgradeable = upgradeable;
            this.extendable = extendable;
        }

        public static CrossPotionType fromBukkit(PotionType bukkitType) {
            switch (bukkitType) {
                case UNCRAFTABLE:
                    return CrossPotionType.UNCRAFTABLE;
                case WATER:
                    return CrossPotionType.WATER;
                case MUNDANE:
                    return CrossPotionType.MUNDANE;
                case THICK:
                    return CrossPotionType.THICK;
                case AWKWARD:
                    return CrossPotionType.AWKWARD;
                case NIGHT_VISION:
                    return CrossPotionType.NIGHT_VISION;
                case INVISIBILITY:
                    return CrossPotionType.INVISIBILITY;
                case JUMP:
                    return CrossPotionType.JUMP;
                case FIRE_RESISTANCE:
                    return CrossPotionType.FIRE_RESISTANCE;
                case SPEED:
                    return CrossPotionType.SPEED;
                case SLOWNESS:
                    return CrossPotionType.SLOWNESS;
                case WATER_BREATHING:
                    return CrossPotionType.WATER_BREATHING;
                case INSTANT_HEAL:
                    return CrossPotionType.INSTANT_HEAL;
                case INSTANT_DAMAGE:
                    return CrossPotionType.INSTANT_DAMAGE;
                case POISON:
                    return CrossPotionType.POISON;
                case REGEN:
                    return CrossPotionType.REGEN;
                case STRENGTH:
                    return CrossPotionType.STRENGTH;
                case WEAKNESS:
                    return CrossPotionType.WEAKNESS;
                case LUCK:
                    return CrossPotionType.LUCK;
                default:
                    Bukkit.getLogger().log(Level.WARNING, "[SkyWars] Failed to find potion type for {0}", bukkitType);
                    return CrossPotionType.WATER;
            }
        }

        public PotionType toBukkit() {
            switch (this) {
                case UNCRAFTABLE:
                    return PotionType.UNCRAFTABLE;
                case WATER:
                    return PotionType.WATER;
                case MUNDANE:
                    return PotionType.MUNDANE;
                case THICK:
                    return PotionType.THICK;
                case AWKWARD:
                    return PotionType.AWKWARD;
                case NIGHT_VISION:
                    return PotionType.NIGHT_VISION;
                case INVISIBILITY:
                    return PotionType.INVISIBILITY;
                case JUMP:
                    return PotionType.JUMP;
                case FIRE_RESISTANCE:
                    return PotionType.FIRE_RESISTANCE;
                case SPEED:
                    return PotionType.SPEED;
                case SLOWNESS:
                    return PotionType.SLOWNESS;
                case WATER_BREATHING:
                    return PotionType.WATER_BREATHING;
                case INSTANT_HEAL:
                    return PotionType.INSTANT_HEAL;
                case INSTANT_DAMAGE:
                    return PotionType.INSTANT_DAMAGE;
                case POISON:
                    return PotionType.POISON;
                case REGEN:
                    return PotionType.REGEN;
                case STRENGTH:
                    return PotionType.STRENGTH;
                case WEAKNESS:
                    return PotionType.WEAKNESS;
                case LUCK:
                    return PotionType.LUCK;
                default:
                    // This should never happen, but return water just in case.
                    return PotionType.WATER;
            }
        }
    }

    @SuppressWarnings("deprecation")
    public static class RawDataPotionData implements CrossPotionData {

        private final byte data;

        public RawDataPotionData(byte data) {
            // We should only be using this class if modern data isn't supported!
            Validate.isTrue(!modernApiSupported);
            this.data = data;
        }

        @Override
        public void applyTo(ItemStack item) {
            MaterialData materialData = item.getData();
            materialData.setData(data);
            item.setData(materialData);
        }

        @Override
        public void saveTo(BlockStorage.InventoryItem.Builder builder) {
            builder.setData(data);
        }
    }

    /**
     * Represents an item with no potion data.
     * <p>
     * This is here for compatibility purposes, so one can do things like:
     * `CrossPotions.extractData(stack).saveTo(itemBuilder);`
     */
    public static class NoPotionData implements CrossPotionData {

        @Override
        public void applyTo(ItemStack item) {
        }

        @Override
        public void saveTo(BlockStorage.InventoryItem.Builder builder) {
        }
    }
}
