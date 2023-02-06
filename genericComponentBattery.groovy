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

0.1.39 - @BrenenP - initial version
0.1.52 - Yves mercier - Add health check capability

*/

metadata
{
    definition(name: "Generic Component Battery", namespace: "community", author: "community", importUrl: "https://raw.githubusercontent.com/ymerj/HE-HA-control/main/genericComponentBattery.groovy")
    {
        capability "Battery"
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
        if (it.name in ["battery"]) {
            if (txtEnable) log.info it.descriptionText
            it.value == "unavailable" ? offline() : online()
            sendEvent(it)
        }
    }
}

void refresh() {
    parent?.componentRefresh(this.device)
}

def offline() {
    sendEvent(name: "healthStatus", value: "offline")
}

def online() {
    sendEvent(name: "healthStatus", value: "online")
}

void ping() {
    refresh()
}
