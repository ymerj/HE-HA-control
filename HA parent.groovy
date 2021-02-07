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
* 0.1.0 2021-02-05 Yves Mercier       Orinal version
* 0.1.1 2021-02-06 Dan Ogorchock      Added basic support for simple "Light" devices from Home Assistant using Hubitat Generic Component Dimmer driver
* 0.1.2 2021-02-06 tomw               Added handling for some binary_sensor subtypes based on device_class
* 0.1.3 2021-02-06 Dan Ogorchock      Bug Fixes 
* 0.1.4 2021-02-06 ymerj              Added version number and import URL
* 0.1.5 2021-02-06 Dan Ogorchock      Added support for Temperature and Humidity Sensors
* 0.1.6 2021-02-06 Dan Ogorchock      Corrected open/closed for HA door events
* 0.1.7 2021-02-07 Dan Ogorchock      Corrected open/closed for HA window, garage_door, and opening per @smarthomeprimer
* 0.1.8 2021-02-07 Dan Ogorchock      Removed temperature and humidity workaround for missing device_class on some HA sensors.  
                                      This can be corrected on the HA side via the Customize entity feature to add the missing device_class.
* 0.1.9 2021-02-06 tomw               More generic handling for "sensor" device_class subdomains.  Added voltage device_class to "sensor".
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
    if ((status == "status: open") || (status == "status: closing")) log.info("websocket ${status}")
    else {
        log.warn("WebSocket ${status}, trying to reconnect")
        runIn(10, initialize)
    }
}

def parse(String description) {
    if (logEnable) log.debug("parsed: $description")
    def response = null;
    try{
        response = new groovy.json.JsonSlurper().parseText(description)
        if (response.type != "event") return
        
        def entity = response?.event?.data?.entity_id
        def domain = entity?.tokenize(".")?.getAt(0)
        def subdomain = response?.event?.data?.new_state?.attributes?.device_class
        def friendly = response?.event?.data?.new_state?.attributes?.friendly_name
        def etat = response?.event?.data?.new_state?.state
        
        if (logEnable) log.debug "parse: domain: ${domain}, subdomain: ${subdomain}, entity: ${entity}, etat: ${etat}, friendly: ${friendly}"
        
        switch (domain) {
            case "switch":
                onOff(domain, entity, friendly, etat)
                break
            case "light":
                def level = response?.event?.data?.new_state?.attributes?.brightness
                if (level) {
                    level = level.toInteger()
                }
                onOffDim(domain, entity, friendly, etat, level)
                break
            default:
                if (subdomain) sendChildEvent(domain, subdomain, entity, friendly, etat)
        }
        return
    }  
    catch(e) {
        log.error("Parsing error: ${e}")
        return
    }
}

def onOff(domain, entity, friendly, etat) {
    if ((etat == "on") || (etat == "off")) {
        def ch = createChild(domain, null, entity, friendly)
        ch.parse([[name: "switch", value: etat, descriptionText:"${ch.label} was turned ${etat}"]])
    }
}

def onOffDim(domain, entity, friendly, etat, level) {
    
    onOff(domain, entity, friendly, etat)
    
    if (level) {
        def ch = createChild(domain, null, entity, friendly)
        level = (level * 100 / 255)
        level = Math.round(level) 
        ch.parse([[name:"level", value: level, descriptionText:"${ch.label} level set to ${level}"]])
    }
}

def sendChildEvent(domain, subdomain, entity, friendly, etat)
{
    def ch = createChild(domain, subdomain, entity, friendly)
    if (!ch) {
        log.info "Child not created for domain: ${domain}, subdomain: ${subdomain}, entity: ${entity}"
        return
    }
    
    def mapping
    def value
    
    switch(domain)
    {
        case "binary_sensor":
            mapping = translateBinarySensorTypes(subdomain)
            value = mapping?.attributes?.states?."${etat}" ?: "${etat}"
            break
        case "sensor":
            mapping = translateSensorTypes(subdomain, etat)
            value = mapping?.attributes?.value ?: "${etat}"
            break
    }
    if ((mapping) || (domain != "binary_sensor")) {
        def name =  mapping?.attributes?.name ?: subdomain        
        //log.info "name: ${name}, value: ${value}"
        ch.parse([[name: name, value: value, descriptionText:"${ch.label} updated"]])    
    }
}

def createChild(domain, subdomain, entity, friendly)
{
    String thisId = device.id
    def ch = getChildDevice("${thisId}-${entity}")
    if (!ch)
    {
        def deviceType
        switch(domain)
        {
            case "switch":
                deviceType = "Generic Component Switch"
                break
            case "light":
                deviceType = "Generic Component Dimmer"
                break
            case "binary_sensor":
                deviceType = translateBinarySensorTypes(subdomain).type
                break
            case "sensor":
                deviceType = translateSensorTypes(device_class, null)
            break
            default:
                return null
        }

        ch = addChildDevice("hubitat", deviceType, "${thisId}-${entity}", [name: "${entity}", label: "${friendly}", isComponent: false])
    }
    
    return ch
}

def translateBinarySensorTypes(device_class)
{
    def mapping =
        [
            door: [type: "Generic Component Contact Sensor", attributes: [name: "contact", states: [on: "open", off: "closed"]]],
            garage_door: [type: "Generic Component Contact Sensor", attributes: [name: "contact", states: [on: "open", off: "closed"]]],
            moisture: [type: "Generic Component Water Sensor", attributes: [name: "water", states: [on: "wet", off: "dry"]]],
            motion: [type: "Generic Component Motion Sensor", attributes: [name: "motion", states: [on: "active", off: "inactive"]]],
            opening: [type: "Generic Component Contact Sensor", attributes: [name: "contact", states: [on: "open", off: "closed"]]],
            presence: [type: "Generic Component Presence Sensor", attributes: [name: "presence", states: [on: "present", off: "not present"]]],
            window: [type: "Generic Component Contact Sensor", attributes: [name: "contact", states: [on: "open", off: "closed"]]]
        ]
    return mapping[device_class]
}

def translateSensorTypes(device_class, etat = null)
{
    def mapping =
        [
            humidity: [type: "Generic Component Humidity Sensor", attributes: [name: "humidity", value: etat?.toFloat()?.toString()]],
            temperature: [type: "Generic Component Temperature Sensor", attributes: [name: "temperature", value: etat?.toFloat()?.toString()]],
            voltage: [type: "Generic Component Voltage Sensor", attributes: [name: "voltage", value: etat?.toFloat()?.toString()]]
        ]
    return mapping[device_class]
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
    state.id = state.id + 1
    entity = ch.name
    domain = entity.tokenize(".")[0]
    messLevel = JsonOutput.toJson([id: state.id, type: "call_service", domain: "${domain}", service: "turn_on", service_data: [entity_id: "${entity}", brightness_pct: "${level}", transition: "${transition}"]])
    if (logEnable) log.debug("messLevel = ${messLevel}")
    interfaces.webSocket.sendMessage("${messLevel}")
}

def componentRefresh(ch){
    if (logEnable) log.info("received refresh request from ${ch.label}")
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
