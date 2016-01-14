package com.cout970.magneticraft.block;

/**
 * Created by cout970 on 18/12/2015.
 */
public class BlockOreBase extends BlockBase {

    private String oreDictName;

    public BlockOreBase(String oreDictName) {
        this.oreDictName = oreDictName;
    }

    @Override
    public String getBlockName() {
        return oreDictName;
    }
}
