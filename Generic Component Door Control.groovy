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

0.1.32 - tomw - initial version
0.1.40 - tomw - Added ContactSensor emulation
0.1.52 - Yves Mercier - Add health check capability
0.1.59 - Yves Mercier - Change healthStatus handling
2.2    - Yves Mercier - Modified from garage door to use with door type entities

*/

metadata
{
    definition(name: "Generic Component Door Control", namespace: "community", author: "community", importUrl: "https://raw.githubusercontent.com/ymerj/HE-HA-control/main/genericComponentDoorControl.groovy")
    {
        capability "ContactSensor"
        capability "DoorControl"
        capability "Refresh"
        capability "Health Check"
    }
    preferences {
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
    }
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
        if (it.name in ["door"]) {
            if (txtEnable) log.info it.descriptionText
            sendEvent(it)
            // emulate contact sensor that mirrors door state
            // note: any status other than "closed" will be treated as "open". 
            sendEvent(name: "contact", value: (it.value == "closed") ? "closed" : "open")
        }
        if (it.name in ["healthStatus"]) {
            if (txtEnable) log.info it.descriptionText
            sendEvent(it)
        }
    }
}

void refresh() {
    parent?.componentRefresh(this.device)
}

void close() {
    parent?.componentClose(this.device)
}

void open() {
    parent?.componentOpen(this.device)
}

void ping() {
    refresh()
}
