package de.cubeisland.cubeengine.log.storage;

import org.bukkit.Material;
import org.bukkit.block.BlockState;

import de.cubeisland.cubeengine.core.util.matcher.Match;
import de.cubeisland.cubeengine.log.Log;

public class BlockData
{
    public final Material material;
    public final Byte data;

    public BlockData(BlockState blockState)
    {
        this(blockState.getType(), blockState.getRawData());
    }

    public BlockData(Material material)
    {
        this(material,null);
    }

    public BlockData(Material material,Byte data)
    {
        this.material = material;
        this.data = data;
    }

    @Override
    public String toString()
    {
        //Match.material() //TODO getPrettyName
        return material + ":" + data;
    }
}
