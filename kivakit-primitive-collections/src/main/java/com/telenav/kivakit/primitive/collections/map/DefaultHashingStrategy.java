////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// © 2011-2021 Telenav, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package com.telenav.kivakit.primitive.collections.map;

import com.telenav.kivakit.core.value.count.Count;
import com.telenav.kivakit.core.value.count.Estimate;
import com.telenav.kivakit.core.value.level.Percent;
import com.telenav.kivakit.primitive.collections.PrimitiveCollection;
import com.telenav.kivakit.primitive.collections.lexakai.DiagramPrimitiveMap;
import com.telenav.lexakai.annotations.UmlClassDiagram;

@UmlClassDiagram(diagram = DiagramPrimitiveMap.class)
public final class DefaultHashingStrategy implements HashingStrategy
{
    public static HashingStrategy DEFAULT = of(Estimate._1024);

    public static Percent defaultMaximumOccupancy()
    {
        return Percent.of(70);
    }

    public static DefaultHashingStrategy of(Estimate capacity)
    {
        return of(capacity, defaultMaximumOccupancy());
    }

    public static DefaultHashingStrategy of(Estimate capacity, Percent maximumOccupancy)
    {
        return new DefaultHashingStrategy(capacity, maximumOccupancy);
    }

    /** The capacity to allocate */
    private Estimate recommendedSize;

    /** The threshold at which we should resize */
    private Count rehashThreshold;

    /** The maximum occupancy, used internally when increasing capacity */
    private Percent maximumOccupancy;

    private DefaultHashingStrategy()
    {
    }

    private DefaultHashingStrategy(Estimate capacity, Percent maximumOccupancy)
    {
        this.maximumOccupancy = maximumOccupancy;
        recommendedSize = capacity.nextPrime();
        rehashThreshold = recommendedSize.percent(maximumOccupancy).asCount();
    }

    @Override
    public Percent maximumOccupancy()
    {
        return maximumOccupancy;
    }

    @Override
    public Estimate recommendedSize()
    {
        return recommendedSize.asEstimate();
    }

    @Override
    public Count rehashThreshold()
    {
        return rehashThreshold;
    }

    @Override
    public String toString()
    {
        return "[DefaultHashingStrategy capacity = " + recommendedSize + ", threshold = " + rehashThreshold + "]";
    }

    @Override
    public HashingStrategy withCapacity(Estimate capacity)
    {
        return new DefaultHashingStrategy(capacity, maximumOccupancy);
    }

    @Override
    public HashingStrategy withIncreasedCapacity()
    {
        return withCapacity(Estimate.estimate(PrimitiveCollection.increasedCapacity(recommendedSize.asInt())));
    }
}
