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
0.1.52- Yves Mercier - added button capability for use as trigger with RM
*/

metadata
{
    definition(name: "Generic Component TimeStamp Sensor", namespace: "community", author: "community", importUrl: "https://raw.githubusercontent.com/ymerj/HE-HA-control/main/genericComponentTimeStampSensor.groovy")
    {
        capability "Refresh"
        capability "PushableButton"
    }
    preferences 
    {
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
    }
    attribute "timestamp", "string"
    attribute "date", "string"
    attribute "health", "string"
}

void updated() {
    log.info "Updated..."
    log.warn "description logging is: ${txtEnable == true}"
    sendEvent(name: "numberOfButtons", value: 1, displayed: false)
}

void installed() {
    log.info "Installed..."
    device.updateSetting("txtEnable",[type:"bool",value:true])
    refresh()
}

void parse(String description) { log.warn "parse(String description) not implemented" }

void parse(List<Map> description) {
    description.each {
        if (it.name in ["timestamp"]) {
            if (txtEnable) log.info it.descriptionText
            sendEvent(it)
            scheduleFutureBtnPush(it.value)
        }
    }
}

def push(bn = 1) {
    sendEvent(name: "pushed", value: bn, descriptionText: "${device.label} timestamp reached", isStateChange: true)
}

def scheduleFutureBtnPush(future) {
    def activation = Date.parse("yyyy-MM-dd'T'HH:mm:ssXXX", future)
    sendEvent(name: "date", value: activation)
    runOnce(activation, push, [overwrite: true])
}   
    
void refresh() {
    parent?.componentRefresh(this.device)
}
