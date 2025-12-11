package me.alpha432.oyvey.features.modules.misc;

import me.alpha432.oyvey.event.impl.RotationEvent;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.manager.RotationManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class LookAtPlayers extends Module {
    private final Setting<Double> range = register(new Setting<>("Range", 20.0, 5.0, 50.0));
    private final Setting<Boolean> silent = register(new Setting<>("Silent", true, "Use silent rotations"));
    private final Setting<Float> speed = register(new Setting<>("Speed", 180.0f, 1.0f, 360.0f, "Rotation speed"));
    private final Setting<Boolean> clamp = register(new Setting<>("Clamp", true, "Clamp pitch to valid range"));
    private final Setting<Boolean> vertical = register(new Setting<>("Vertical", true, "Look at head"));
    
    private PlayerEntity currentTarget = null;
    private RotationManager rotationManager = RotationManager.getInstance();

    public LookAtPlayers() {
        super("LookAtPlayers", "Automatically looks at players", Category.MISC);
    }

    @Subscribe
    private void onRotation(RotationEvent event) {
        if (!isEnabled()) return;
        
        PlayerEntity target = findTarget();
        if (target == null) {
            currentTarget = null;
            return;
        }
        
        currentTarget = target;
        
        // Calculate angles
        Vec3d eyes = mc.player.getEyePos();
        Vec3d targetPos = vertical.getValue() ? target.getEyePos() : target.getPos().add(0, 1.0, 0);
        
        double diffX = targetPos.x - eyes.x;
        double diffY = targetPos.y - eyes.y;
        double diffZ = targetPos.z - eyes.z;
        
        double horizontalDist = Math.sqrt(diffX * diffX + diffZ * diffZ);
        
        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0f;
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, horizontalDist));
        
        yaw = MathHelper.wrapDegrees(yaw);
        if (clamp.getValue()) {
            pitch = MathHelper.clamp(pitch, -90.0f, 90.0f);
        }
        
        // Apply rotation
        if (silent.getValue()) {
            // Use your client's silent rotation system
            rotationManager.setTargetRotation(yaw, pitch, speed.getValue());
            event.setYaw(yaw);
            event.setPitch(pitch);
        } else {
            // Direct rotation
            mc.player.setYaw(yaw);
            mc.player.setPitch(pitch);
            mc.player.setHeadYaw(yaw);
            mc.player.setBodyYaw(yaw);
        }
    }

    private PlayerEntity findTarget() {
        PlayerEntity best = null;
        double bestDist = Double.MAX_VALUE;
        
        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof PlayerEntity) || entity == mc.player) continue;
            
            double dist = mc.player.distanceTo(entity);
            if (dist < range.getValue() && dist < bestDist) {
                best = (PlayerEntity) entity;
                bestDist = dist;
            }
        }
        
        return best;
    }

    @Override
    public void onDisable() {
        if (silent.getValue()) {
            rotationManager.reset();
        }
        currentTarget = null;
    }

    @Override
    public String getDisplayInfo() {
        return currentTarget != null ? 
            String.format("%.1f", mc.player.distanceTo(currentTarget)) : "None";
    }
}