/*

Copyright 2024

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

-------------------------------------------

Change history:

2.8    - Yves Mercier  - initial version
2.9    - ritchierich   - Added a distanceThreshold input and code to control how often events are logged

*/

import groovy.transform.Field

@Field static Map distanceOpts = [
	defaultValue: "0.2",
	defaultText: "On 0.2m change",
	options:[
		"0.2":"On 0.2m change",
		"0.5":"On 0.5m change",
		"1.0":"On 1.0m change",
		"1.5":"On 1.5m change",
		"-1.0":"log10 precision 1",
		"0.0":"log10 precision 0",
		"disable":"Disable"
	]
]

metadata
{
    definition(name: "HADB Generic Component Occupancy Sensor", namespace: "community", author: "community", importUrl: "https://raw.githubusercontent.com/ymerj/HE-HA-control/main/HADBgenericComponentOccupancySensor.groovy")
    {
        capability "Refresh"
        capability "Health Check"
        capability "PresenceSensor"
    }
    preferences {
        input name:"distanceThreshold", title:"Distance Reporting (default:${distanceOpts.defaultText})", type:"enum", options:distanceOpts.options , defaultValue:distanceOpts.defaultValue
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
    }
    attribute "distance", "number"
    attribute "room", "string"
    attribute "healthStatus", "enum", ["offline", "online"]
}

void updated() {
    log.info "Updated..."
    log.warn "description logging is: ${txtEnable == true}"
}

void installed() {
    log.info "Installed..."
    device.updateSetting("txtEnable",[type:"bool",value:true])
    refresh()
}

void parse(String description) { log.warn "parse(String description) not implemented" }

void parse(List<Map> description) {
    description.each {
        if (it.name in ["room"]) {
            if (txtEnable) log.info it.descriptionText
            sendEvent(it)
            def presence = (it.value == "not_home") ? "not present" : "present"
            def descriptionText = "presence was set to ${presence}"
            sendEvent(name: "presence", value: presence, descriptionText: descriptionText)
            if (txtEnable) log.info descriptionText
        }
        if (it.name in ["healthStatus"]) {
            if (txtEnable) log.info it.descriptionText
            sendEvent(it)
        }
        if (it.name in ["attributes"]) state.attributes = it.value
        if (it.name in ["distance"]) {
            if (txtEnable) log.info it.descriptionText
            def value = it.value.toDouble()
            def previousValue = device.currentValue("distance") ?: 0.0
            if (value == previousValue) return
            
            if (settings.distanceThreshold != "disable") {
                def distanceThreshold = (settings.distanceThreshold?:distanceOpts.defaultValue).toDouble()
                if (distanceThreshold <= 0.0) {
                    distanceThreshold = Math.abs(distanceThreshold)
                    if (Math.log10(value).round(distanceThreshold) == Math.log10(previousValue).round(distanceThreshold)) return
                } else if (Math.abs(value-previousValue) < distanceThreshold) {
                    return
                }
            }
            sendEvent(it)
        }
    }
}

void refresh() {
    parent?.componentRefresh(this.device)
}

void ping() {
    refresh()
}
