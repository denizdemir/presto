/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.presto.hive;

import com.facebook.presto.spi.ColumnHandle;
import com.facebook.presto.spi.ConnectorTableLayoutHandle;
import com.facebook.presto.spi.TupleDomain;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

public final class HiveTableLayoutHandle
        implements ConnectorTableLayoutHandle
{
    private final String connectorId;
    private final HiveTableHandle hiveTableHandle;
    private final TupleDomain<ColumnHandle> constraint;
//    private final TupleDomain<ColumnHandle> undeterminedTupleDomain;

    @JsonCreator
    public HiveTableLayoutHandle(
            @JsonProperty String connectorId,
            @JsonProperty HiveTableHandle hiveTableHandle,
            @JsonProperty TupleDomain<ColumnHandle> constraint)
//            @JsonProperty TupleDomain<ColumnHandle> undeterminedTupleDomain,
//            @JsonProperty List<String> hivePartitionNames)
    {
        this.connectorId = requireNonNull(connectorId, "connectorId is null");
        this.hiveTableHandle = requireNonNull(hiveTableHandle, "tableName is null");
        this.constraint = requireNonNull(constraint, "constraint is null");
//        this.undeterminedTupleDomain = requireNonNull(undeterminedTupleDomain, "undeterminedTupleDomain is null");
//        this.hivePartitionNames = requireNonNull(hivePartitionNames, "hivePartitions is null");
    }

    @JsonProperty
    public String getConnectorId()
    {
        return connectorId;
    }

    @JsonProperty
    public HiveTableHandle getHiveTableHandle()
    {
        return hiveTableHandle;
    }

    @JsonProperty
    public TupleDomain<ColumnHandle> getConstraint()
    {
        return constraint;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(connectorId, hiveTableHandle, constraint);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final HiveTableLayoutHandle other = (HiveTableLayoutHandle) obj;
        return Objects.equals(this.connectorId, other.connectorId)
                && Objects.equals(this.hiveTableHandle, other.hiveTableHandle)
                && Objects.equals(this.constraint, other.constraint);
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper(this)
                .add("connectorId", connectorId)
                .add("tableHandle", hiveTableHandle)
                .add("constraint", constraint)
                .toString();
    }
}
