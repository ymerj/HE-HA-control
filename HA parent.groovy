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
* 0.1.21 2021-02-22 Yves mercier       Reinstated CloseConnection command. Added connection status on device page.
* 0.1.22 2021-02-24 tomw               Changes to support optional device filtering.  For use with haDeviceBridgeConfiguration.groovy.
* 0.1.23 2021-02-25 Dan Ogorchock      Switched from Exclude List to Include List
* 0.1.24 2021-03-07 Yves Mercier       Added device label in event description
* 0.1.25 2021-03-18 Dan Ogorchock      Updated for recent Hass Fan handling changes (use percentages to set speeds instead of deprecated speed names)
* 0.1.26 2021-04-02 DongHwan Suh       Added partial support for Color temperature, RGB, RGBW lights
*                                      (Manually updating the device type to the corresponding one is required in Hubitat. Only statuses of level and switch are shown in Hubitat.)
* 0.1.27 2021-04-11 Yves Mercier       Added option for secure connection
* 0.1.28 2021-04-14 Dan Ogorchock      Improved Fan Device handling
* 0.1.29 2021-04-17 Dan Ogorchock      Added support for Smoke Detector Binary Sensor
* 0.1.30 2021-08-10 tomw               Added support for device_tracker as Presence Sensor
* 0.1.31 (dev) 2021-09-05 tomw               Added support for cover as Garage Door Opener
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
        command "closeConnection"        
//        command "deleteAllChildDevices"
        
        attribute "Connection", "string"
    }

    preferences {
        input ("ip", "text", title: "IP", description: "HomeAssistant IP Address", required: true)
        input ("port", "text", title: "Port", description: "HomeAssistant Port Number", required: true, defaultValue: "8123")
        input ("token", "text", title: "Token", description: "HomeAssistant Long-Lived Access Token", required: true)
        input ("secure", "bool", title: "Require secure connection (https)", defaultValue: false)
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
    
    closeConnection()
    
    state.id = 2
    def connectionType = "ws"
    if (secure) connectionType = "wss"
    auth = '{"type":"auth","access_token":"' + "${token}" + '"}'
    evenements = '{"id":1,"type":"subscribe_events","event_type":"state_changed"}'
    try {
        interfaces.webSocket.connect("${connectionType}://${ip}:${port}/api/websocket", ignoreSSLIssues: true)
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
        sendEvent(name: "Connection", value: "Closed")
        return
    } 
    else if(status == 'status: open') {
        log.info "websocket is open"
        // success! reset reconnect delay
        pauseExecution(1000)
        state.reconnectDelay = 1
        state.wasExpectedClose = false
        sendEvent(name: "Connection", value: "Open")
    } 
    else {
        log.warn "WebSocket error, reconnecting."
        sendEvent(name: "Connection", value: "Reconnecting")
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
        
        // check whether we have a parent, and search its includeList for devices to process
        def includeList = getParent()?.includeList
        if(includeList && !includeList?.contains(entity)) return
        
        def domain = entity?.tokenize(".")?.getAt(0)
        def device_class = response?.event?.data?.new_state?.attributes?.device_class
        def friendly = response?.event?.data?.new_state?.attributes?.friendly_name
        newVals << response?.event?.data?.new_state?.state
        def mapping = null
        
        if (logEnable) log.debug "parse: domain: ${domain}, device_class: ${device_class}, entity: ${entity}, newVals: ${newVals}, friendly: ${friendly}"
        
        switch (domain) {
            case "fan":
                def speed = response?.event?.data?.new_state?.attributes?.speed
                def percentage = response?.event?.data?.new_state?.attributes?.percentage
                switch (percentage.toInteger()) {
                    case 0: 
                        speed = "off"
                        break
                    case 25: 
                        speed = "low"
                        break
                    case 50: 
                        speed = "medium"
                        break
                    case 75: 
                        speed = "medium-high"
                        break
                    case 100: 
                        speed = "high"
                    default:
                        if (logEnable) log.info "Invalid fan percentage received - ${percentage}"
                }
                newVals += speed
                newVals += percentage
                mapping = translateDevices(domain, newVals, friendly)
                if (mapping) updateChildDevice(mapping, entity, friendly)
                break
            case "device_tracker":
            case "switch":            
                mapping = translateDevices(domain, newVals, friendly)
                if (mapping) updateChildDevice(mapping, entity, friendly)      
                break
            case "light":
                def level = response?.event?.data?.new_state?.attributes?.brightness
                if (level) level = Math.round((level.toInteger() * 100 / 255))
                newVals += level
                mapping = translateDevices(domain, newVals, friendly)
                if (!level) mapping.event.remove(1) //remove the level update since it is not provided with the HA 'off' event json data
                if (mapping) updateChildDevice(mapping, entity, friendly)
                break
            case "cover":
                def opening = response?.event?.data?.new_state?.attributes?.is_opening
                def closing = response?.event?.data?.new_state?.attributes?.is_closing
                def closed = response?.event?.data?.new_state?.attributes?.is_close
                
                if(closed != "None") { newVals[1] = (closed ? "closed" : "open") }
                if(opening) { newVals[1] = "opening" }
                if(closing) { newVals[1] = "closing" }
                break
            case "binary_sensor":
            case "sensor":
                mapping = translateDevices(device_class, newVals, friendly)
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

def translateDevices(device_class, newVals, friendly)
{
    def mapping =
        [
            door: [type: "Generic Component Contact Sensor",            event: [[name: "contact", value: newVals[0] == "on" ? "open":"closed", descriptionText:"${friendly} is updated"]]],
            fan: [type: "Generic Component Fan Control",                event: [[name: "switch", value: newVals[0], descriptionText:"${friendly} was turn ${newVals[0]}"],[name: "speed", value: newVals[1], descriptionText:"${friendly} speed was set to ${newVals[1]}"],[name: "level", value: newVals[2], descriptionText:"${friendly} level was set to ${newVals[2]}"]]],
            garage_door: [type: "Generic Component Contact Sensor",     event: [[name: "contact", value: newVals[0] == "on" ? "open":"closed", descriptionText:"${friendly} is updated"]]],
            humidity: [type: "Generic Component Humidity Sensor",       event: [[name: "humidity", value: newVals[0], descriptionText:"${friendly} humidity is ${newVals[0]}"]]],
            illuminance: [type: "Generic Component Illuminance Sensor", event: [[name: "illuminance", value: newVals[0], descriptionText:"${friendly} illuminance is ${newVals[0]}"]], namespace: "community"],
            light: [type: "Generic Component Dimmer",                   event: [[name: "switch", value: newVals[0], descriptionText:"${friendly} was turn ${newVals[0]}"],[name: "level", value: newVals[1], descriptionText:"${friendly} level was set to ${newVals[1]}"]]],
            moisture: [type: "Generic Component Water Sensor",          event: [[name: "water", value: newVals[0] == "on" ? "wet":"dry", descriptionText:"${friendly} is updated"]]],
            motion: [type: "Generic Component Motion Sensor",           event: [[name: "motion", value: newVals[0] == "on" ? """active""":"""inactive""", descriptionText:"${friendly} is updated"]]],
            opening: [type: "Generic Component Contact Sensor",         event: [[name: "contact", value: newVals[0] == "on" ? "open":"closed", descriptionText:"${friendly} is updated"]]],
            presence: [type: "Generic Component Presence Sensor",       event: [[name: "presence", value: newVals[0] == "on" ? "present":"not present", descriptionText:"${friendly} is updated"]], namespace: "community"],
            pressure: [type: "Generic Component Pressure Sensor",       event: [[name: "pressure", value: newVals[0], descriptionText:"${friendly} pressure is ${newVals[0]}"]], namespace: "community"],
            smoke: [type: "Generic Component Smoke Detector",           event: [[name: "smoke", value: newVals[0] == "on" ? "detected":"clear", descriptionText:"${friendly} is updated"]]],
            switch: [type: "Generic Component Switch",                  event: [[name: "switch", value: newVals[0], descriptionText:"${friendly} was turn ${newVals[0]}"]]],
            temperature: [type: "Generic Component Temperature Sensor", event: [[name: "temperature", value: newVals[0], descriptionText:"${friendly} temperature is ${newVals[0]}"]]],
            voltage: [type: "Generic Component Voltage Sensor",         event: [[name: "voltage", value: newVals[0], descriptionText:"${friendly} voltage is ${newVals[0]}"]]],
            window: [type: "Generic Component Contact Sensor",          event: [[name: "contact", value: newVals[0] == "on" ? "open":"closed", descriptionText:"${friendly} is updated"]]],
            device_tracker: [type: "Generic Component Presence Sensor", event: [[name: "presence", value: newVals[0] == "home" ? "present":"not present", descriptionText:"${friendly} is updated"]], namespace: "community"],
            cover: [type: "Generic Component Garage Door Control",      event: [[name: "door", value: newVals[1] ?: "unknown", descriptionText:"${friendly} was turn ${newVals[1]}"]], namespace: "community"]
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
            case 1..25:
                componentSetSpeed(ch, "low")
            break
            case 26..50:
                componentSetSpeed(ch, "medium")
            break
            case 51..75:
                componentSetSpeed(ch, "medium-high")
            break
            case 76..100:
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

def componentSetColor(ch, color, transition=1){
    if (logEnable) log.info("received setColor request from ${ch.label}")
    
    state.id = state.id + 1
    entity = ch.name
    domain = entity.tokenize(".")[0]
    convertedHue = Math.round(color.hue * 360/100)
    messHSL = JsonOutput.toJson([id: state.id, type: "call_service", domain: "${domain}", service: "turn_on", service_data: [entity_id: "${entity}", brightness_pct: "${color.level}", hs_color: ["${convertedHue}", "${color.saturation}"], transition: "${transition}"]])
    if (logEnable) log.debug("messHSL = ${messHSL}")
    interfaces.webSocket.sendMessage("${messHSL}")
}

def componentSetColorTemperature(ch, colortemperature, transition=1){
    if (logEnable) log.info("received setColorTemperature request from ${ch.label}")
    
    state.id = state.id + 1
    entity = ch.name
    domain = entity.tokenize(".")[0]
    messCT = JsonOutput.toJson([id: state.id, type: "call_service", domain: "${domain}", service: "turn_on", service_data: [entity_id: "${entity}", kelvin: "${colortemperature}", transition: "${transition}"]])
    if (logEnable) log.debug("messCT = ${messCT}")
    interfaces.webSocket.sendMessage("${messCT}")
}

def componentSetSpeed(ch, speed) {
    if (logEnable) log.info("received setSpeed request from ${ch.label}, with speed = ${speed}")
    int percentage = 0
    entity = ch.name
    domain = entity.tokenize(".")[0]
    switch (speed) {
        case "on":
            state.id = state.id + 1
            messOn = JsonOutput.toJson([id: state.id, type: "call_service", domain: "${domain}", service: "turn_on", service_data: [entity_id: "${entity}"]])
            interfaces.webSocket.sendMessage("${messOn}")
            break
        case "off":
            state.id = state.id + 1    
            messOff = JsonOutput.toJson([id: state.id, type: "call_service", domain: "${domain}", service: "turn_off", service_data: [entity_id: "${entity}"]])
            interfaces.webSocket.sendMessage("${messOff}")
            break
        case "low":
        case "medium-low":
            percentage = 25
            break
        case "auto":
        case "medium":
            percentage = 50
            break
        case "medium-high":
            percentage = 75
            break
        case "high":
            percentage = 100
            break
        default:
            if (logEnable) log.info "No case defined for Fan setSpeed(${speed})"
    }

    if (percentage != 0) {
        state.id = state.id + 1
        messOn = JsonOutput.toJson([id: state.id, type: "call_service", domain: "${domain}", service: "turn_on", service_data: [entity_id: "${entity}", percentage: "${percentage}"]])
        interfaces.webSocket.sendMessage("${messOn}")
    }
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
            speed = "medium-high"
            break
        case "medium-high":
            speed = "high"
            break
        case "high":
            speed = "off"
            break
    }
    componentSetSpeed(ch, speed)
}

void componentClose(ch) {
    operateCover(ch, "close")
}

void componentOpen(ch) {
    operateCover(ch, "open")
}

void operateCover(ch, op)
{
    if (logEnable) log.info("received ${op} request from ${ch.label}")
    state.id = state.id + 1
    entity = ch.name
    domain = entity.tokenize(".")[0]
    messOff = JsonOutput.toJson([id: state.id, type: "call_service", domain: "${domain}", service: (op == "open") ? "open_cover" : "close_cover", service_data: [entity_id: "${entity}"]])
    if (logEnable) log.debug("messOff = ${messOff}")
    interfaces.webSocket.sendMessage("${messOff}")
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
    state.wasExpectedClose = true
    interfaces.webSocket.close()
}

def deleteAllChildDevices() {
    log.info "Uninstalling all Child Devices"
    getChildDevices().each {
          deleteChildDevice(it.deviceNetworkId)
       }
}
