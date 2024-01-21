/*

Copyright 2023

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

0.1.58 - Yves Mercier - initial version
0.1.59 - Yves Mercier - Change healthStatus handling
0.1.63 - Yves Mercier - Added Actuator capability

*/

metadata
{
    definition(name: "Generic Component Number", namespace: "community", author: "community", importUrl: "https://raw.githubusercontent.com/ymerj/HE-HA-control/main/genericComponentNumber.groovy")
    {
        capability "Actuator"
        capability "Refresh"
        capability "Health Check"
        
        command "setNumber", [[ name: "newValue", type: "NUMBER", description: "Define new value" ]]
    }
    preferences {
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
    }
    attribute "number", "number"
    attribute "unit", "string"
    attribute "minimum", "number"
    attribute "maximum", "number"
    attribute "step", "number"
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
        if (it.name in ["number"]){
            sendEvent(name: "unit", value: it.unit ?: "none")
        }
        if (it.name in ["number", "minimum", "maximum", "step", "healthStatus"]) {
            if (txtEnable) log.info it.descriptionText
            sendEvent(it)
        }
    }
}

void setNumber(newValue) {
    parent?.componentSetNumber(this.device, newValue)
}

void refresh() {
    parent?.componentRefresh(this.device)
}

void ping() {
    refresh()
}
