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

*/

metadata
{
    definition(name: "Generic Component Unknown Sensor", namespace: "community", author: "community", importUrl: "https://raw.githubusercontent.com/ymerj/HE-HA-control/main/genericComponentUnknownSensor.groovy")
    {
        capability "Refresh"
    }
    preferences {
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
    }
    attribute "value", "number"
    attribute "valueStr", "string"
    attribute "unit", "string"
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

void updateAttr(String aKey, aValue, String aUnit = ""){
    sendEvent(name:aKey, value:aValue, unit:aUnit)
}

void parse(String description) { log.warn "parse(String description) not implemented" }

void parse(List<Map> description) {
    description.each {
        if (it.name in ["unknown"]) {
            if (txtEnable) log.info it.descriptionText
            updateAttr("value", it.value, it.unit_of_measurement)
            updateAttr("valueStr", it.value, it.unit_of_measurement)
            updateAttr("unit", it.unit_of_measurement)
        }
    }
}

void refresh() {
    parent?.componentRefresh(this.device)
}
