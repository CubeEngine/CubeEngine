package org.cubeengine.module.vigil.data;

import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.data.persistence.DataSerializable;

public interface EnumSerializable extends DataSerializable {

    @Override
    default int contentVersion() {
        return 0;
    }

    @Override
    default DataContainer toContainer() {
        var container = DataContainer.createNew();
        if (this instanceof Enum<?> enumInstance) {
            container.set(DataQuery.of("enum"), enumInstance.name());
        }
        return container;
    }
}
