package danger.orespawn.dangerzone.effect;

import danger.orespawn.dangerzone.OreSpawnDangerZone;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** DangerZone's baby state: a quarter the size, a sixteenth the bite, a quarter the armour. */
public class ShrunkEffect extends MobEffect {
    public ShrunkEffect() {
        super(MobEffectCategory.NEUTRAL, 0xB8E0FF);
        addAttributeModifier(Attributes.SCALE, OreSpawnDangerZone.id("shrunk_scale"), -0.75, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(Attributes.ATTACK_DAMAGE, OreSpawnDangerZone.id("shrunk_damage"), -0.9375, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(Attributes.ARMOR, OreSpawnDangerZone.id("shrunk_armor"), -0.75, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
