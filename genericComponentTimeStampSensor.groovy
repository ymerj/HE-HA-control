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
0.1.51- Yves Mercier - initial version
0.1.52- Yves Mercier - added button and health capability
*/

metadata
{
    definition(name: "Generic Component TimeStamp Sensor", namespace: "community", author: "community", importUrl: "https://raw.githubusercontent.com/ymerj/HE-HA-control/main/genericComponentTimeStampSensor.groovy")
    {
        capability "Refresh"
        capability "PushableButton"
        capability "Health Check"
    }
    preferences 
    {
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
        input name: "pushRequired", type: "bool", title: "Enable pushed button event at the time reported", defaultValue: true
    }
    attribute "timestamp", "string"
    attribute "date", "string"
    attribute "healthStatus", "enum", ["offline", "online"]
}

void updated() {
    log.info "Updated..."
    log.warn "description logging is: ${txtEnable == true}"
    sendEvent(name: "numberOfButtons", value: 1, displayed: false)
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
        if (it.name in ["timestamp"]) {
            if (txtEnable) log.info it.descriptionText
            it.value == "unavailable" ? offline() : online()
            sendEvent(it)
            if (pushRequired) scheduleFutureBtnPush(it.value)
        }
    }
}

def push(bn = 1) {
    sendEvent(name: "pushed", value: bn, descriptionText: "${device.label} timestamp reached", isStateChange: true)
}

def scheduleFutureBtnPush(future) {
    try {
        def activation = toDateTime(future)
        sendEvent(name: "date", value: activation)
        runOnce(activation, push, [overwrite: true])
    }
    catch(e) {
        log.error("Error: ${e}")
        sendEvent(name: "date", value: "invalid")
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

def ping() {
    refresh()
}
