/* Copyright 2018 Telstra Open Source
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.openkilda.northbound.dto.flows;

import org.openkilda.messaging.info.event.IslChangeType;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonSerialize
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LinksDto {

    private long speed;

    @JsonProperty("available_bandwidth")
    private long availableBandwidth;

    @JsonProperty("state")
    protected IslChangeType state;

    private List<PathDto> path;

    /**
     * With the advent of link_props, a link can have zero or more extra props. This field will
     * be used to store that information. Technically, it'll store all unrecognized keys .. unless
     * we add a filter somewhere.
     */
    Map<String, String> otherFields = new HashMap<>();

    public long getSpeed() {
        return speed;
    }

    public void setSpeed(long speed) {
        this.speed = speed;
    }

    public long getAvailableBandwidth() {
        return availableBandwidth;
    }

    public void setAvailableBandwidth(long availableBandwidth) {
        this.availableBandwidth = availableBandwidth;
    }

    public List<PathDto> getPath() {
        return path;
    }

    public void setPath(List<PathDto> path) {
        this.path = path;
    }

    public IslChangeType getState() {
        return state;
    }

    public void setState(IslChangeType state) {
        this.state = state;
    }

    // Capture all other fields that Jackson do not match other members
    @JsonAnyGetter
    public Map<String, String> otherFields() {
        return otherFields;
    }

    /**
     * This will store any unrecognized key in the json object.
     * There are some values that may not be necessary, and/or we'd prefer to hide .. like clazz
     */
    @JsonAnySetter
    public void setOtherField(String name, String value) {
        otherFields.put(name, value);
    }

}
