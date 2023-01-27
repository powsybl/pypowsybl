package com.powsybl.dataframe.network.adders;

import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.modification.topology.CreateBranchFeederBaysBuilder;
import com.powsybl.iidm.network.BranchAdder;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.ConnectablePosition;

import static com.powsybl.dataframe.network.adders.SeriesUtils.applyIfPresent;

public abstract class AbstractFeederBaysSeries {

    abstract AbstractBranchSeries createTypedSeries(UpdatingDataframe dataframe);

    public CreateBranchFeederBaysBuilder createBuilder(Network n, UpdatingDataframe dataframe) {
        AbstractBranchSeries series = createTypedSeries(dataframe);
        CreateBranchFeederBaysBuilder builder = new CreateBranchFeederBaysBuilder();
        for (int row = 0; row < dataframe.getRowCount(); row++) {
            BranchAdder<?> lAdder = series.create(n, row);
            builder.withBranchAdder(lAdder);
            applyIfPresent(dataframe.getStrings("busbar_section_id_1"), row, builder::withBbs1);
            applyIfPresent(dataframe.getStrings("busbar_section_id_2"), row, builder::withBbsId2);
            applyIfPresent(dataframe.getInts("position_order_1"), row, builder::withPositionOrder1);
            applyIfPresent(dataframe.getInts("position_order_2"), row, builder::withPositionOrder2);
            applyIfPresent(dataframe.getStrings("direction_1"), row, ConnectablePosition.Direction.class, builder::withDirection1);
            applyIfPresent(dataframe.getStrings("direction_2"), row, ConnectablePosition.Direction.class, builder::withDirection2);
        }
        return builder;
    }
}
