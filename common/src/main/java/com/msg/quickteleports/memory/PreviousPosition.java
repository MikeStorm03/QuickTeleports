package com.msg.quickteleports.memory;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class PreviousPosition {

	private Vec3 previousPos;
    private ResourceKey<Level> previousDimension;

    public PreviousPosition(double posX, double posY, double posZ, ResourceKey<Level> dimension) {
        this.previousPos = new Vec3(posX, posY, posZ);
        this.previousDimension = dimension;
    }

    public double getX() {
        return this.previousPos.x;
    }
    
    public double getY() {
        return this.previousPos.y;
    }
    
    public double getZ() {
        return this.previousPos.z;
    }

    public ResourceKey<Level> getDimension(){
        return this.previousDimension;
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("previous_x", previousPos.x);
        tag.putDouble("previous_y", previousPos.y);
        tag.putDouble("previous_z", previousPos.z);
        tag.putString("previous_dimension", previousDimension.identifier().toString());
        return tag;
    }

    public static PreviousPosition fromTag(CompoundTag tag) {
        double x = tag.getDouble("previous_x").get();
        double y = tag.getDouble("previous_y").get();
        double z = tag.getDouble("previous_z").get();
        ResourceKey<Level> dimension = ResourceKey.create(
                Registries.DIMENSION,
                Identifier.parse(tag.getString("previous_dimension").get())
            );
        return new PreviousPosition(x, y, z, dimension);
    }
}