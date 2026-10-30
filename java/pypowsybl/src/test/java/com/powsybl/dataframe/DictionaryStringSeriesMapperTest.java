/**
 * Copyright (c) 2026, Elia Group (http://www.eliagroup.eu)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe;

import com.powsybl.dataframe.impl.DefaultDataframeHandler;
import com.powsybl.dataframe.impl.Series;
import com.powsybl.dataframe.update.DefaultUpdatingDataframe;
import com.powsybl.dataframe.update.TestStringSeries;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Nico Westerbeck {@literal <nico.westerbeck at 50hertz.com>}
 */
class DictionaryStringSeriesMapperTest {

    private record Item(String id, String kind, String name) {
    }

    private static final List<Item> ITEMS = List.of(new Item("a", "line", "A"), new Item("b", "line", null), new Item("c", "load", "C"),
            new Item("d", "line", "D"));

    @Test
    void testMapper() {
        DataframeMapper<List<Item>, Void> mapper = new DataframeMapperBuilder<List<Item>, Item, Void>()
                .itemsProvider(items -> items)
                .dictionaryStringsIndex("id", Item::id)
                .dictionaryStrings("kind", Item::kind)
                .dictionaryStrings("name", Item::name)
                .build();
        List<Series> series = new ArrayList<>();
        mapper.createDataframe(ITEMS, new DefaultDataframeHandler(series::add), new DataframeFilter(), null);

        assertThat(series).extracting(Series::getName).containsExactly("id", "kind", "name");
        Series id = series.get(0);
        assertThat(id.isIndex()).isTrue();
        assertThat(id.isDictionary()).isTrue();
        assertThat(id.getInts()).containsExactly(0, 1, 2, 3);
        assertThat(id.getDictionary()).containsExactly("a", "b", "c", "d");
        assertThat(id.getStrings()).containsExactly("a", "b", "c", "d");

        Series kind = series.get(1);
        assertThat(kind.isIndex()).isFalse();
        assertThat(kind.isDictionary()).isTrue();
        assertThat(kind.getInts()).containsExactly(0, 0, 1, 0);
        assertThat(kind.getDictionary()).containsExactly("line", "load");
        assertThat(kind.getStrings()).containsExactly("line", "line", "load", "line");

        // null is encoded as an empty string (what a null of a plain string column is read as on the python side)
        Series name = series.get(2);
        assertThat(name.getDictionary()).containsExactly("A", "", "C", "D");
        assertThat(name.getStrings()).containsExactly("A", "", "C", "D");
    }

    @Test
    void testMetadataAndUpdate() {
        List<String> updates = new ArrayList<>();
        DataframeMapper<List<Item>, Void> mapper = new DataframeMapperBuilder<List<Item>, Item, Void>()
                .itemsProvider(items -> items)
                .itemGetter((items, id) -> items.stream().filter(item -> item.id().equals(id)).findFirst().orElseThrow())
                .dictionaryStringsIndex("id", Item::id)
                .dictionaryStrings("kind", Item::kind, (item, v) -> updates.add(item.id() + "=" + v), false)
                .dictionaryStrings("name", Item::name)
                .build();
        List<SeriesMetadata> metadata = mapper.getSeriesMetadata();
        assertThat(metadata).extracting(SeriesMetadata::getName).containsExactly("id", "kind", "name");
        assertThat(metadata.get(0).isIndex()).isTrue();
        assertThat(metadata.get(0).getType()).isEqualTo(SeriesDataType.STRING);
        assertThat(metadata.get(1).isModifiable()).isTrue();
        assertThat(metadata.get(1).isDefaultAttribute()).isFalse();
        assertThat(metadata.get(1).getType()).isEqualTo(SeriesDataType.STRING);
        assertThat(metadata.get(2).isModifiable()).isFalse();

        DefaultUpdatingDataframe dataframe = new DefaultUpdatingDataframe(2);
        dataframe.addSeries("id", true, new TestStringSeries("b", "d"));
        dataframe.addSeries("kind", false, new TestStringSeries("load", "load"));
        mapper.updateSeries(ITEMS, dataframe);
        assertThat(updates).containsExactly("b=load", "d=load");

        DefaultUpdatingDataframe nameDataframe = new DefaultUpdatingDataframe(1);
        nameDataframe.addSeries("id", true, new TestStringSeries("a"));
        nameDataframe.addSeries("name", false, new TestStringSeries("X"));
        assertThatThrownBy(() -> mapper.updateSeries(ITEMS, nameDataframe))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Series 'name' is not modifiable.");
    }
}
