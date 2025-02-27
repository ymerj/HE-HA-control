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
2.7    - Yves Mercier - initial version
2.15   - Yves Mercier - refactored to reflect breaking changes
*/

metadata
{
    definition(name: "HADB Generic Component Event", namespace: "community", author: "community", importUrl: "https://raw.githubusercontent.com/ymerj/HE-HA-control/main/HADBgenericComponentEvent.groovy")
    {
        capability "Refresh"
        capability "PushableButton"
        // capability "DoubleTapableButton"
        // capability "HoldableButton"
        // capability "ReleasableButton"
        capability "Health Check"
    }
    preferences 
    {
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
    }
    attribute "timestamp", "string"
    attribute "eventType", "string"
    attribute "eventList", "enum"
    attribute "healthStatus", "enum", ["offline", "online"]
}

void updated() {
    log.info "Updated..."
    log.warn "description logging is ${txtEnable == true}, button event is ${pushRequired == true}"
}

void installed() {
    log.info "Installed..."
    device.updateSetting("txtEnable",[type:"bool",value:true])
    updated()
    refresh()
}

void parse(String description) { log.warn "parse(String description) not implemented" }

void parse(List<Map> description) {
    description.each {
        if (txtEnable) log.info it.descriptionText
        sendEvent(it)
    }
}

def push(bn = 1) {
    log.warn "Button not pushable"
}

def hold(bn = 1) {
    log.warn "Button not holdable"
}

def doubleTap(bn = 1) {
    log.warn "Button not doubletapable"
}

def release(bn = 1) {
    log.warn "Button not releasable"
}
void refresh() {
    parent?.componentRefresh(this.device)
}

def ping() {
    refresh()
}
