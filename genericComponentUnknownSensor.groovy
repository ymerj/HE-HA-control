/*

Copyright 2021

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

0.1.49 - mboisson - initial version
0.1.52 - Yves mercier - Add health check capability
0.1.56 - Yves Mercier - Refactored to handle null units
0.1.59 - Yves Mercier - Change healthStatus handling
2.9    - ritchierich  - Passes all attributes

*/

metadata
{
    definition(name: "Generic Component Unknown Sensor", namespace: "community", author: "community", importUrl: "https://raw.githubusercontent.com/ymerj/HE-HA-control/main/genericComponentUnknownSensor.groovy")
    {
        capability "Refresh"
        capability "Health Check"
    }
    preferences {
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
    }
    attribute "value", "number"
    attribute "valueStr", "string"
    attribute "unit", "string"
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
        if (it.name in ["unknown"]) {
            if (txtEnable) log.info it.descriptionText
            sendEvent(name: "value", value: it.value, unit: it.unit, descriptionText: it.descriptionText)
            sendEvent(name: "valueStr", value: it.value)
            sendEvent(name: "unit", value: it.unit ?: "none")
        }
        if (it.name in ["healthStatus"]) {
            if (txtEnable) log.info it.descriptionText
            sendEvent(it)
        }
        //All attributes passed from HA are available and can be leveraged by a custom driver
        if (it.name in ["attributes"]) {
            state.attributes = it.value
        }
    }
}

void refresh() {
    parent?.componentRefresh(this.device)
}

void ping() {
    refresh()
}
