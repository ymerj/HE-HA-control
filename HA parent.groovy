/*
* Home Assistant to Hubitat Integration
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
* 0.1.0  2021-02-05 Yves Mercier       Orinal version
* 0.1.1  2021-02-06 Dan Ogorchock      Added basic support for simple "Light" devices from Home Assistant using Hubitat Generic Component Dimmer driver
* 0.1.2  2021-02-06 tomw               Added handling for some binary_sensor subtypes based on device_class
* 0.1.3  2021-02-06 Dan Ogorchock      Bug Fixes 
* 0.1.4  2021-02-06 Yves Mercier       Added version number and import URL
* 0.1.5  2021-02-06 Dan Ogorchock      Added support for Temperature and Humidity Sensors
* 0.1.6  2021-02-06 Dan Ogorchock      Corrected open/closed for HA door events
* 0.1.7  2021-02-07 Dan Ogorchock      Corrected open/closed for HA window, garage_door, and opening per @smarthomeprimer
* 0.1.8  2021-02-07 Dan Ogorchock      Removed temperature and humidity workaround for missing device_class on some HA sensors.  
*                                      This can be corrected on the HA side via the Customize entity feature to add the missing device_class.
* 0.1.9  2021-02-07 tomw               More generic handling for "sensor" device_classes.  Added voltage device_class to "sensor".
* 0.1.10 2021-02-07 Dan Ogorchock      Refactored the translation from HA to HE to simplify the overall design
* 0.1.11 2021-02-07 Dan Ogorchock      Completed refactoring of Dimmer Switch support
* 0.1.12 2021-02-08 Dan Ogorchock      Fixed typo in log.info statement
* 0.1.13 2021-02-08 tomw               Added "community" namespace support for component drivers.  Added Pressure and Illuminance.
* 0.1.14 2021-02-10 Dan Ogorchock      Added support for Fan devices (used Lutron Fan Controller as test device.)
* 0.1.15 2021-02-13 tomw               Adjust websocket status handling to reconnect on both close and error conditions.
* 0.1.16 2021-02-14 tomw               Revert 0.1.15
* 0.1.17 2021-02-14 Dan Ogorchock      Improved webSocket reconnect logic
* 0.1.18 2021-02-14 tomw               Avoid reconnect loop on initialize
* 0.1.19 2021-02-16 Yves Mercier       Added Refresh handler
* 0.1.20 2021-02-16 Yves mercier       Refactored webSocketStatus
*
* Thank you(s):
*/

import groovy.json.JsonSlurper
import groovy.json.JsonOutput

metadata {
    definition (name: "HomeAssistant Hub Parent", namespace: "ymerj", author: "Yves Mercier", importUrl: "https://raw.githubusercontent.com/ymerj/HE-HA-control/main/HA%20parent.groovy") {
        capability "Initialize"

//        command "createChild", [[ name: "entity", type: "STRING", description: "HomeAssistant Entity ID" ]]
//        command "removeChild", [[ name: "entity", type: "STRING", description: "HomeAssistant Entity ID" ]]
//        command "closeConnection"        
//        command "deleteAllChildDevices"
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
    
    state.wasExpectedClose = true
    
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

def uninstalled() {
    log.info("uninstalled...")
    closeConnection()
    unschedule()
    deleteAllChildDevices()
}

def webSocketStatus(String status){
    if (logEnable) log.debug "webSocket ${status}"

    if ((status == "status: closing") && (state.wasExpectedClose)) {
        state.wasExpectedClose = false
        return
    } 
    else if(status == 'status: open') {
        log.info "websocket is open"
        // success! reset reconnect delay
        pauseExecution(1000)
        state.reconnectDelay = 1
        state.wasExpectedClose = false
    } 
    else {
        log.warn "WebSocket error, reconnecting."
        reconnectWebSocket()
    }
}

def reconnectWebSocket() {
    // first delay is 2 seconds, doubles every time
    state.reconnectDelay = (state.reconnectDelay ?: 1) * 2
    // don't let delay get too crazy, max it out at 10 minutes
    if(state.reconnectDelay > 600) state.reconnectDelay = 600

    //If the Home Assistant Hub is offline, give it some time before trying to reconnect
    runIn(state.reconnectDelay, initialize)
}


def parse(String description) {
    if (logEnable) log.debug("parse(): description = ${description}")
    def response = null;
    try{
        response = new groovy.json.JsonSlurper().parseText(description)
        if (response.type != "event") return
        
        def newVals = []
        def entity = response?.event?.data?.entity_id
        def domain = entity?.tokenize(".")?.getAt(0)
        def device_class = response?.event?.data?.new_state?.attributes?.device_class
        def friendly = response?.event?.data?.new_state?.attributes?.friendly_name
        newVals << response?.event?.data?.new_state?.state
        def mapping = null
        
        if (logEnable) log.debug "parse: domain: ${domain}, device_class: ${device_class}, entity: ${entity}, newVals: ${newVals}, friendly: ${friendly}"
        
        switch (domain) {
            case "fan":
                def speed = response?.event?.data?.new_state?.attributes?.speed
                newVals += speed
                mapping = translateDevices(domain, newVals)
                if (mapping) updateChildDevice(mapping, entity, friendly)
                break
            case "switch":
                mapping = translateDevices(domain, newVals)
                if (mapping) updateChildDevice(mapping, entity, friendly)
                break
            case "light":
                def level = response?.event?.data?.new_state?.attributes?.brightness
                if (level) level = Math.round((level.toInteger() * 100 / 255))
                newVals += level
                mapping = translateDevices(domain, newVals)
                if (!level) mapping.event.remove(1) //remove the level update since it is not provided with the HA 'off' event json data
                if (mapping) updateChildDevice(mapping, entity, friendly)
                break
            case "binary_sensor":
            case "sensor":
                mapping = translateDevices(device_class, newVals)
                if (mapping) updateChildDevice(mapping, entity, friendly)                
                break
            default:
                if (logEnable) log.info "No mapping exists for domain: ${domain}, device_class: ${device_class}.  Please contact devs to have this added."
        }
        return
    }  
    catch(e) {
        log.error("Parsing error: ${e}")
        return
    }
}

def translateDevices(device_class, newVals)
{
    def mapping =
        [
            door: [type: "Generic Component Contact Sensor",            event: [[name: "contact", value: newVals[0] == "on" ? "open":"closed", descriptionText:"Contact updated"]]],
            fan: [type: "Generic Component Fan Control",                event: [[name: "switch", value: newVals[0], descriptionText:"Fan switch changed to ${newVals[0]}"],[name: "speed", value: newVals[1], descriptionText:"Speed changed to ${newVals[1]}"]]],
            garage_door: [type: "Generic Component Contact Sensor",     event: [[name: "contact", value: newVals[0] == "on" ? "open":"closed", descriptionText:"Contact updated"]]],
            humidity: [type: "Generic Component Humidity Sensor",       event: [[name: "humidity", value: newVals[0], descriptionText:"Humidity changed to ${newVals[0]}"]]],
            illuminance: [type: "Generic Component Illuminance Sensor", event: [[name: "illuminance", value: newVals[0], descriptionText:"Illuminance changed to ${newVals[0]}"]], namespace: "community"],
            light: [type: "Generic Component Dimmer",                   event: [[name: "switch", value: newVals[0], descriptionText:"Switch changed to ${newVals[0]}"],[name: "level", value: newVals[1], descriptionText:"Level changed to ${newVals[1]}"]]],
            moisture: [type: "Generic Component Water Sensor",          event: [[name: "water", value: newVals[0] == "on" ? "wet":"dry", descriptionText:"Water updated"]]],
            motion: [type: "Generic Component Motion Sensor",           event: [[name: "motion", value: newVals[0] == "on" ? """active""":"""inactive""", descriptionText:"Motion updated"]]],
            opening: [type: "Generic Component Contact Sensor",         event: [[name: "contact", value: newVals[0] == "on" ? "open":"closed", descriptionText:"Contact updated"]]],
            presence: [type: "Generic Component Presence Sensor",       event: [[name: "presence", value: newVals[0] == "on" ? "present":"not present", descriptionText:"Presence updated"]]],
            pressure: [type: "Generic Component Pressure Sensor",       event: [[name: "pressure", value: newVals[0], descriptionText:"Pressure changed to ${newVals[0]}"]], namespace: "community"],
            switch: [type: "Generic Component Switch",                  event: [[name: "switch", value: newVals[0], descriptionText:"Switch changed to ${newVals[0]}"]]],
            temperature: [type: "Generic Component Temperature Sensor", event: [[name: "temperature", value: newVals[0], descriptionText:"Temperature changed to ${newVals[0]}"]]],
            voltage: [type: "Generic Component Voltage Sensor",         event: [[name: "voltage", value: newVals[0], descriptionText:"Voltage changed to ${newVals[0]}"]]],
            window: [type: "Generic Component Contact Sensor",          event: [[name: "contact", value: newVals[0] == "on" ? "open":"closed", descriptionText:"Contact updated"]]]
        ]

    return mapping[device_class]
}

def updateChildDevice(mapping, entity, friendly) {
    def ch = createChild(mapping.type, entity, friendly, mapping.namespace)
    if (!ch) {
        log.warn "Child type: ${mapping.type} not created for entity: ${entity}"
        return
    }
    else {
        ch.parse(mapping.event)
    }
}

def createChild(deviceType, entity, friendly, namespace = null)
{
    def ch = getChildDevice("${device.id}-${entity}")
    if (!ch) ch = addChildDevice(namespace ?: "hubitat", deviceType, "${device.id}-${entity}", [name: "${entity}", label: "${friendly}", isComponent: false])
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
    if (!ch.currentValue("level")) {
        messOn = JsonOutput.toJson([id: state.id, type: "call_service", domain: "${domain}", service: "turn_on", service_data: [entity_id: "${entity}"]])
    }
    else {
        messOn = JsonOutput.toJson([id: state.id, type: "call_service", domain: "${domain}", service: "turn_on", service_data: [entity_id: "${entity}", brightness_pct: "${ch.currentValue("level")}"]])        
    }
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

def componentSetLevel(ch, level, transition=1){
    if (logEnable) log.info("received setLevel request from ${ch.label}")
    if (level > 100) level = 100
    if (level < 0) level = 0

    //if a Fan device, special handling
    if (ch.currentValue("speed")) {
        switch (level.toInteger()) {
            case 0:
                componentSetSpeed(ch, "off")
            break
            case 1..33:
                componentSetSpeed(ch, "low")
            break
            case 34..66:
                componentSetSpeed(ch, "medium")
            break
            case 67..100:
                componentSetSpeed(ch, "high")
            break
            default:
                if (logEnable) log.info "No case defined for Fan setLevel(${level})"
        }
    } 
    else {        
        state.id = state.id + 1
        entity = ch.name
        domain = entity.tokenize(".")[0]
        messLevel = JsonOutput.toJson([id: state.id, type: "call_service", domain: "${domain}", service: "turn_on", service_data: [entity_id: "${entity}", brightness_pct: "${level}", transition: "${transition}"]])
        if (logEnable) log.debug("messLevel = ${messLevel}")
        interfaces.webSocket.sendMessage("${messLevel}")
    }
}

def componentSetSpeed(ch, speed) {
    if (logEnable) log.info("received setSpeed request from ${ch.label}, with speed = ${speed}")
    switch (speed) {
        case "off":
            //no change
        break
        case "low":
        case "medium-low":
            speed = "low"
        break
        case "on":
        case "auto":
        case "medium":
        case "medium-high":
            speed = "medium"
        break
        case "high":
            //no change
        break
        default:
            if (logEnable) log.info "No case defined for Fan setSpeed(${speed})"
    }
    state.id = state.id + 1
    entity = ch.name
    domain = entity.tokenize(".")[0]
    messOn = JsonOutput.toJson([id: state.id, type: "call_service", domain: "${domain}", service: "set_speed", service_data: [entity_id: "${entity}", speed: "${speed}"]])        
    if (logEnable) log.debug("messOn = ${messOn}")
    interfaces.webSocket.sendMessage("${messOn}")
}

def componentCycleSpeed(ch) {
    def newSpeed = ""
    switch (ch.currentValue("speed")) {
        case "off":
            speed = "low"
        break
        case "low":
        case "medium-low":
            speed = "medium"
        break
        case "medium":
        case "medium-high":
            speed = "high"
        break
        case "high":
            speed = "off"
        break
    }
    componentSetSpeed(ch, speed)
}

def componentRefresh(ch){
    if (logEnable) log.info("received refresh request from ${ch.label}")
    state.id = state.id + 1
    entity = ch.name
    domain = entity.tokenize(".")[0]
    messUpd = JsonOutput.toJson([id: state.id, type: "call_service", domain: "homeassistant", service: "update_entity", service_data: [entity_id: "${entity}"]])
    if (logEnable) log.debug("messUpd = ${messUpd}")
    interfaces.webSocket.sendMessage("${messUpd}")
}

def closeConnection() {
    if (logEnable) log.debug("Closing connection...")   
    interfaces.webSocket.sendMessage('{"id":2,"type":"unsubscribe_events","subscription":1}')
    interfaces.webSocket.close()
}

def deleteAllChildDevices() {
    log.info "Uninstalling all Child Devices"
    getChildDevices().each {
          deleteChildDevice(it.deviceNetworkId)
       }
}
