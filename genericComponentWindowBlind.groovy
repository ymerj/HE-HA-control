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

0.1.43 - tomw - Initial Release
0.1.52 - Yves Mercier - Add health check capability
0.1.59 - Yves Mercier - Change healthStatus handling
2.2    - Yves Mercier - Modified from shade to use with blind type entities
2.9    - Yves Mercier - Add windowShade attribute

*/

metadata
{
    definition(name: "Generic Component Window Blind", namespace: "community", author: "community", importUrl: "https://raw.githubusercontent.com/ymerj/HE-HA-control/main/genericComponentWindowBlind.groovy")
    {
        capability "WindowBlind"
        capability "Refresh"
        capability "Health Check"
    }
    preferences {
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
    }
    attribute "healthStatus", "enum", ["offline", "online"]
    attribute "windowShade", "enum", ["opening", "partially open", "closed", "open", "closing", "unknown"]
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
        if (it.name in ["position", "windowBlind", "windowShade", "tilt", "healthStatus"]) {
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

void setPosition(position) {
    parent?.componentSetPosition(this.device, position)
}

void startPositionChange(direction) {
    parent?.componentStartPositionChange(this.device, direction)
}

void stopPositionChange() {
    parent?.componentStopPositionChange(this.device)
}

void setTiltLevel(tilt) {
    parent?.componentSetTiltLevel(this.device, tilt)
}

void ping() {
    refresh()
}
