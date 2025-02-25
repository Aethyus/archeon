package com.mmodding.archeon.materials.armor;

import com.mmodding.archeon.init.ArcheonItems;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

public class LusonythArmor implements ArmorMaterial {

	public static final LusonythArmor INSTANCE = new LusonythArmor();

	@Override
	public int getDurability(EquipmentSlot slot) {
		return new int[] {13, 15, 16, 11} [slot.getEntitySlotId()] * 37;
	}

	@Override
	public int getProtectionAmount(EquipmentSlot slot) {
		return new int[] {3, 6, 8, 3} [slot.getEntitySlotId()];
	}

	@Override
	public int getEnchantability() {
		return 15;
	}

	@Override
	public SoundEvent getEquipSound() {
		return SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE;
	}

	@Override
	public Ingredient getRepairIngredient() {
		return Ingredient.ofItems(ArcheonItems.LUSONYTH_INGOT);
	}

	@Override
	public String getName() {
		return "lusonyth";
	}

	@Override
	public float getToughness() {
		return 3.0f;
	}

	@Override
	public float getKnockbackResistance() {
		return 0.1f;
	}
}
