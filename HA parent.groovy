/*
HA integration
*
* Description:
* Allow control of HA devices.
*
* Required Information:
* Home Asisstant IP and Port number
* Home Assistant long term Access Token
*
* Features List:
*
* Licensing:
* Copyright 2021 Yves Mercier.
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
* in compliance with the License. You may obtain a copy of the License at: http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
* on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
* for the specific language governing permissions and limitations under the License.
*
* Version Control:
*
* Thank you(s):
*/

import groovy.json.JsonSlurper
import groovy.json.JsonOutput

metadata {
    definition (name: "HomeAssistant Hub Parent", namespace: "ymerj", author: "Yves Mercier") {
        capability "Initialize"

//        command "createChild", [[ name: "entity", type: "STRING", description: "HomeAssistant Entity ID" ]]
//        command "removeChild", [[ name: "entity", type: "STRING", description: "HomeAssistant Entity ID" ]]
//        command "closeConnection"
    }

    preferences {
        input ("ip", "text", title: "IP", description: "HomeAssistant IP Address", required: true)
        input ("port", "text", title: "Port", description: "HomeAssistant Port Number", required: true, defaultValue: "8123")
        input ("token", "text", title: "Token", description: "HomeAssistant Access Token", required: true)
        input ("logEnable", "bool", title: "Enable debug logging", defaultValue: true)
        input ("txtEnable", "bool", title: "Enable description text logging", defaultValue: true)        
    }
}

def logsOff(){
    log.warn("debug logging disabled...")
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}

def updated(){
    log.info("updated...")
    log.warn("debug logging is: ${logEnable == true}")
    log.warn("description logging is: ${txtEnable == true}")
    unschedule()
    if (logEnable) runIn(1800,logsOff)
    initialize()
}

def initialize() {
    log.info("initializing...")
    state.id = 2
    auth = '{"type":"auth","access_token":"' + "${token}" + '"}'
    evenements = '{"id":1,"type":"subscribe_events","event_type":"state_changed"}'
    try {
        interfaces.webSocket.connect("ws://${ip}:${port}/api/websocket")
        interfaces.webSocket.sendMessage("${auth}")
        interfaces.webSocket.sendMessage("${evenements}")
    } 
    catch(e) {
        log.error("initialize error: ${e.message}")
    }
}

def webSocketStatus(String status){
    if ((status == "status: open") || (status == "status: closing")) log.info("websocket ${status}")
    else {
        log.warn("WebSocket ${status}, trying to reconnect")
        runIn(10, initialise)
    }
}

def parse(String description) {
    if (logEnable) log.debug("parsed: $description")
    def reponse = null;
    try{
        reponse = new groovy.json.JsonSlurper().parseText(description)
        if (reponse.type != "event") return
        
        entity = reponse.event.data.entity_id
        domain = entity.tokenize(".")[0]
        friendly = reponse.event.data.new_state.attributes.friendly_name
        etat = reponse.event.data.new_state.state
        
        switch (domain) {
            case "switch":
                onOff(domain, entity, friendly, etat)
                break
        }
        return
    }  
    catch(e) {
        log.error("Parsing error: ${e}")
        return
    }
}

def onOff(domain, entity, friendly, etat) {
    if (etat == "on") childSwitchOn(domain, entity, friendly)
    if (etat == "off") childSwitchOff(domain, entity, friendly)
}

def childSwitchOn(domain, entity, friendly){
    def ch = createChild(domain, entity, friendly)
    ch.parse([[name:"switch", value:"on", descriptionText:"${ch.label} was turned on"]])
}

def childSwitchOff(domain, entity, friendly){
    def ch = createChild(domain, entity, friendly)
    ch.parse([[name:"switch", value:"off", descriptionText:"${ch.label} was turned off"]])
}

def createChild(domain, entity, friendly){
    String thisId = device.id
    def ch = getChildDevice("${thisId}-${entity}")
    if (!ch) {ch = addChildDevice("hubitat", "Generic Component Switch", "${thisId}-${entity}", [name: "${entity}", label: "${friendly}", isComponent: false])}
    return ch
}

def removeChild(entity){
    String thisId = device.id
    def ch = getChildDevice("${thisId}-${entity}")
    if (ch) {deleteChildDevice("${thisId}-${entity}")}
}
    
def componentOn(ch){
    if (logEnable) log.info("received on request from ${ch.label}")
    state.id = state.id + 1
    entity = ch.name
    domain = entity.tokenize(".")[0]
    messOn = JsonOutput.toJson([id: state.id, type: "call_service", domain: "${domain}", service: "turn_on", service_data: [entity_id: "${entity}"]])
    if (logEnable) log.debug("messOn = ${messOn}")
    interfaces.webSocket.sendMessage("${messOn}")
}

def componentOff(ch){
    if (logEnable) log.info("received off request from ${ch.label}")
    state.id = state.id + 1
    entity = ch.name
    domain = entity.tokenize(".")[0]
    messOff = JsonOutput.toJson([id: state.id, type: "call_service", domain: "${domain}", service: "turn_off", service_data: [entity_id: "${entity}"]])
    if (logEnable) log.debug("messOff = ${messOff}")
    interfaces.webSocket.sendMessage("${messOff}")
}

def componentRefresh(ch){
    if (logEnable) log.info("received refresh request from ${ch.label}")
}

def closeConnection() {
    if (logEnable) log.debug("Closing connection...")
    interfaces.webSocket.sendMessage('{"id":2,"type":"unsubscribe_events","subscription":1}')
    interfaces.webSocket.close()
}
