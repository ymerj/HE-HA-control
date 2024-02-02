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
* 0.1.31 2021-09-23 tomw               Added support for Power sensor
* 0.1.33 2021-09-28 tomw               Added support for cover as Garage Door Opener
* 0.1.34 2021-11-24 Yves Mercier       Added event type: digital or physical (in that case, from Hubitat or from Home Assistant).	
* 0.1.35 2021-12-01 draperw            Added support for locks
* 0.1.36 2021-12-14 Yves Mercier       Improved event type
* 0.1.37 2021-12-26 gabriel_kpk        Added support for Climate domain
* 0.1.38 2021-12-29                    Improved Climate support, Code cleanup, Minor decription fixes
* 0.1.39 2022-01-19 BrenenP            Added support for additional sensors
* 0.1.40 2022-02-23 tomw               Added support for Energy sensor
* 0.1.41 2022-03-08 Yves Mercier       Validate Fan speed
* 0.1.42 2022-04-02 tomw               Added support for input_boolean
* 0.1.43 2022-05-10 tomw               Added support for Curtain device_class
* 0.1.44 2022-05-15 tomw               Added support for Shade device_class
* 0.1.46 2022-07-04 tomw               Advanced configuration - manual add/remove of devices; option to disable filtering; unused child cleanup
* 0.1.47 2022-11-03 mboisson           Added support for Carbon Dioxide, Radon, and Volatile Organic Compounds sensors
* 0.1.48 2022-11-14 Yves Mercier       Added minimal RGB light support (no CT)
* 0.1.49 2022-11-16 mboisson           Sensor units and support for "unknown" sensor types
* 0.1.50 2022-12-06 Yves Mercier       Improved support for lights and added option to ignore unavailable state
* 0.1.51 2023-01-30 Yves Mercier       Added support for "unknown" binary sensor and timestamp sensor
* 0.1.53 2023-02-19 Yves Mercier       Fix a typo and refine support for lights (CT)
* 0.1.54 2023-03-02 Yves Mercier       Added support for light effects
* 0.1.55 2023-05-27 Yves Mercier       Added support for pm2.5
* 0.1.56 2023-06-12 Yves Mercier       Modified various sensor units handling
* 0.1.57 2023-07-18 Yves Mercier       By default map unsuported sensors to unknown
* 0.1.58 2023-07-27 Yves Mercier       Add support for number entity
* 0.1.59 2023-08-13 Yves Mercier       Remove unsupported states and change how health status is reported.
* 0.1.60 2013-12-31 mboisson           Added support for air quality parts
* 0.1.61 2024-01-02 Yves Mercier       Add alternate RGBW implementation + add handling of unknown state.
* 0.1.62 2024-01-10 Yves Mercier       Add input_number support
* 2.0	 2024-01-20 Yves Mercier       Introduce entity subscription model
* 2.1	 2024-01-30 Yves Mercier       Improve climate support
* 2.2    2024-02-01 Yves Mercier       Add support for door types, blind types and moisture
*/

import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import groovy.json.JsonBuilder

metadata {
    definition (name: "HomeAssistant Hub Parent", namespace: "ymerj", author: "Yves Mercier", importUrl: "https://raw.githubusercontent.com/ymerj/HE-HA-control/main/HA%20parent.groovy") {
        capability "Initialize"

        command "closeConnection"        

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

def removeChild(entity){
    String thisId = device.id
    def ch = getChildDevice("${thisId}-${entity}")
    if (ch) {deleteChildDevice("${thisId}-${entity}")}
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
    def subscriptionsList = device.getDataValue("filterList")
    if(subscriptionsList == null) return
    evenements = '{"id":1,"type":"subscribe_trigger","trigger":{"platform":"state","entity_id":"' + subscriptionsList + '"}}'
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
	def newState = response?.event?.variables?.trigger?.to_state
	if (newState?.state?.toLowerCase() == "unknown") return
        
        def origin = "physical"
        if (newState?.context?.user_id) origin = "digital"
        
        def newVals = []
        def entity = response?.event?.variables?.trigger?.entity_id        
        def domain = entity?.tokenize(".")?.getAt(0)
        def device_class = newState?.attributes?.device_class
        def friendly = newState?.attributes?.friendly_name

        newVals << newState?.state
        def mapping = null
        
        if (logEnable) log.debug "parse: domain: ${domain}, device_class: ${device_class}, entity: ${entity}, newVals: ${newVals}, friendly: ${friendly}"
        
        switch (domain) {
            case "fan":
                def speed = newState?.attributes?.speed?.toLowerCase()
                choices =  ["low","medium-low","medium","medium-high","high","auto"]
                if (!(choices.contains(speed)))
                    {
                    if (logEnable) log.info "Invalid fan speed received - ${speed}"
                    speed = null
                    }
                def percentage = newState?.attributes?.percentage
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
                mapping = translateDevices(domain, newVals, friendly, origin)
                if (!speed) mapping.event.remove(1)
                if (mapping) updateChildDevice(mapping, entity, friendly)
                break
            case "cover":
                // we need to get "current_position" out of the state, if it's there
                //   note that any additional attributes will use different offsets in the translateCovers mapping
                def pos = newState?.attributes?.current_position?.toInteger()
                newVals += pos
                def tilt = newState?.attributes?.current_tilt_position?.toInteger() // or perhaps newState?.attributes?.current_cover_tilt_position?.toInteger()
                newVals += tilt
                switch (device_class)
                {
                   case {it in ["blind","shutter","window"]}:
                       device_class = "blind"
                       break
                   case {it in ["curtain","shade"]}:
                       device_class = "shade"
                       break
                   case "garage":
                       break
                   default:
                       device_class = "door"
                }
                mapping = translateCovers(device_class, newVals, friendly, origin)
                if (mapping) updateChildDevice(mapping, entity, friendly)
                break
            case "lock":
                mapping = translateDevices(domain, newVals, friendly, origin)
                if (mapping) updateChildDevice(mapping, entity, friendly)
                break
            case "device_tracker":
                mapping = translateDevices(domain, newVals, friendly, origin)
                if (mapping) updateChildDevice(mapping, entity, friendly)
                break
            case "switch":
            case "input_boolean":
                mapping = translateDevices(domain, newVals, friendly, origin)
                if (mapping) updateChildDevice(mapping, entity, friendly)
                break
            case "light":
                def level = newState?.attributes?.brightness
                if (level) level = Math.round((level.toInteger() * 100 / 255))
                newVals += level
                def hue = newState?.attributes?.hs_color?.getAt(0)
                if (hue) hue = Math.round(hue.toInteger() * 100 / 360)
                def sat = newState?.attributes?.hs_color?.getAt(1)
                if (sat) sat = Math.round(sat.toInteger())
                def ct = newState?.attributes?.color_temp
                if (ct) ct = Math.round(1000000/ct)
                def effectsList = []
                effectsList = newState?.attributes?.effect_list
                def effectName = newState?.attributes?.effect
                def lightType = []
                lightType = newState?.attributes?.supported_color_modes
                if ((lightType.intersect(["hs", "rgb"])) && (lightType.contains("color_temp"))) lightType += "rgbw"
                if (effectsList) lightType += "rgbwe"
                switch (lightType)
                    {
                    case {it.intersect(["rgbwe"])}:
                        device_class = "rgbwe"
                        newVals += [hue, sat, ct, effectsList, effectName]
                        break
                    case {it.intersect(["rgbww", "rgbw"])}:
                        device_class = "rgbw"
                        newVals += [hue, sat, ct]
                        break
                    case {it.intersect(["hs", "rgb"])}:
                        device_class = "rgb"
                        newVals += [hue, sat]
                        break
                    case {it.intersect(["color_temp"])}:
                        device_class = "ct"
                        newVals += ct
                        break
                    default:
                        device_class = "dimmer"
                    }
                mapping = translateLight(device_class, newVals, friendly, origin)
                if (newVals[0] == "off") //remove updates not provided with the HA 'off' event json data
                   {
                   for(int i in (mapping.event.size - 1)..1) 
                       {
                       mapping.event.remove(i)
                       }  
                    }
                if (mapping) updateChildDevice(mapping, entity, friendly)
                break
            case "binary_sensor":
                mapping = translateBinarySensors(device_class, newVals, friendly, origin)
                if (mapping) updateChildDevice(mapping, entity, friendly)
                break
            case "input_number":
            case "number":
                def minimum = newState?.attributes?.min
                def maximum = newState?.attributes?.max
                def step = newState?.attributes?.step
                def unit = newState?.attributes?.unit_of_measurement
                newVals += [unit, minimum, maximum, step]
                mapping = translateDevices(domain, newVals, friendly, origin)
                if (mapping) updateChildDevice(mapping, entity, friendly)
                break
            case "sensor":
                def unit_of_measurement = newState?.attributes?.unit_of_measurement
		
                // if there is no device_class, we need to infer from the units
                if ((!device_class) && (unit_of_measurement in ["Bq/m³","pCi/L"])) device_class = "radon"
                newVals << unit_of_measurement
                mapping = translateSensors(device_class, newVals, friendly, origin)
                if (mapping) updateChildDevice(mapping, entity, friendly)
                break
            case "climate":
                def thermostat_mode = newState?.state
                def current_temperature = newState?.attributes?.current_temperature
                def hvac_action = newState?.attributes?.hvac_action
                def fan_mode = newState?.attributes?.fan_mode
                def target_temperature = newState?.attributes?.temperature
                def target_temp_high = newState?.attributes?.target_temp_high
                def target_temp_low = newState?.attributes?.target_temp_low
                def hvac_modes = []
                hvac_modes = newState?.attributes?.hvac_modes
                hvac_modes = hvac_modes.minus(["auto"])
                if (hvac_modes.contains("heat_cool")) hvac_modes = hvac_modes - "heat_cool" + "auto"
                hvac_modes = hvac_modes.intersect(["auto", "off", "heat", "emergency heat", "cool"])
                hvac_modes = new groovy.json.JsonBuilder(hvac_modes).toString()
                switch (fan_mode)
                {
                    case "off":
                        thermostat_mode = "off"
                        break
                    case "auto":
                        break
                    default:
                    	fan_mode = "on"
                }
                switch (thermostat_mode)
                {
                    case "fan_only":
                        fan_mode = "circulate"
                        break
                    case "heat_cool":
                        thermostat_mode = "auto"
                        break
                    case "dry":
                    case "auto":
                       return
                }
                switch (hvac_action)
                {
                    case "off":
                        hvac_action = "idle"
                        break
                    case "fan":
                        hvac_action = "fan only"
                        break
                    case "preheating":
                        hvac_action = "pending heat"
                        break
                }
                newVals = [thermostat_mode, current_temperature, hvac_action, fan_mode, target_temperature, target_temp_high, target_temp_low, hvac_modes]
                mapping = translateDevices(domain, newVals, friendly, origin)
		
                // remove updates possibly not provided with the HA 'off' event json data
                // perhaps not needed for climate entity
                if (newVals[0] == "off")
                   {
                   for(int i in (mapping.event.size - 1)..3) 
                       {
                       mapping.event.remove(i)
                       }  
                    }
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

def translateBinarySensors(device_class, newVals, friendly, origin)
{
    def mapping =
        [
            door: [type: "Generic Component Contact Sensor",            event: [[name: "contact", value: newVals[0] == "on" ? "open":"closed", descriptionText: "${friendly} is ${newVals[0] == 'on' ? 'open':'closed'}"]]],
            garage_door: [type: "Generic Component Contact Sensor",     event: [[name: "contact", value: newVals[0] == "on" ? "open":"closed", descriptionText:"${friendly} is ${newVals[0] == 'on' ? 'open':'closed'}"]]],
            lock: [type: "Generic Component Contact Sensor",            event: [[name: "contact", value: newVals[0] == "on" ? "open":"closed", descriptionText:"${friendly} is ${newVals[0] == 'on' ? 'unlocked':'locked'}"]]],
            moisture: [type: "Generic Component Water Sensor",          event: [[name: "water", value: newVals[0] == "on" ? "wet":"dry", descriptionText:"${friendly} is ${newVals[0] == 'on' ? 'wet':'dry'}"]]],
            motion: [type: "Generic Component Motion Sensor",           event: [[name: "motion", value: newVals[0] == "on" ? """active""":"""inactive""", descriptionText:"${friendly} is ${newVals[0] == 'on' ? 'active':'inactive'}"]]],
            occupancy: [type: "Generic Component Motion Sensor",        event: [[name: "motion", value: newVals[0] == "on" ? """active""":"""inactive""", descriptionText:"${friendly} is ${newVals[0] == 'on' ? 'active':'inactive'}"]]],
            moving: [type: "Generic Component Acceleration Sensor",     event: [[name: "acceleration", value: newVals[0] == "on" ? """active""":"""inactive""", descriptionText:"${friendly} is ${newVals[0] == 'on' ? 'active':'inactive'}"]]],
            opening: [type: "Generic Component Contact Sensor",         event: [[name: "contact", value: newVals[0] == "on" ? "open":"closed", descriptionText:"${friendly} is ${newVals[0] == 'on' ? 'open':'closed'}"]]],
            presence: [type: "Generic Component Presence Sensor",       event: [[name: "presence", value: newVals[0] == "on" ? "present":"not present", descriptionText:"${friendly} is ${newVals[0] == 'on' ? 'present':'not present'}"]], namespace: "community"],
            smoke: [type: "Generic Component Smoke Detector",           event: [[name: "smoke", value: newVals[0] == "on" ? "detected":"clear", descriptionText:"${friendly} is ${newVals[0] == 'on' ? 'detected':'clear'}"]]],
            vibration: [type: "Generic Component Acceleration Sensor",  event: [[name: "acceleration", value: newVals[0] == "on" ? """active""":"""inactive""", descriptionText:"${friendly} is ${newVals[0] == 'on' ? 'active':'inactive'}"]]],
            unknown: [type: "Generic Component Unknown Sensor",         event: [[name: "unknown", value: newVals[0], descriptionText:"${friendly} is ${newVals[0]}"]], namespace: "community"],
            window: [type: "Generic Component Contact Sensor",          event: [[name: "contact", value: newVals[0] == "on" ? "open":"closed", descriptionText:"${friendly} is ${newVals[0] == 'on' ? 'open':'closed'}"]]],
        ]
    if (!mapping[device_class]) device_class = "unknown"
    return mapping[device_class]
}

def translateSensors(device_class, newVals, friendly, origin)
{
    def mapping =
        [
            humidity: [type: "Generic Component Humidity Sensor",             event: [[name: "humidity", value: newVals[0], unit: newVals[1] ?: "%", descriptionText:"${friendly} humidity is ${newVals[0]} ${newVals[1] ?: '%'}"]]],
            moisture: [type: "Generic Component Humidity Sensor",             event: [[name: "humidity", value: newVals[0], unit: newVals[1] ?: "%", descriptionText:"${friendly} humidity is ${newVals[0]} ${newVals[1] ?: '%'}"]]],
            illuminance: [type: "Generic Component Illuminance Sensor",       event: [[name: "illuminance", value: newVals[0], unit: newVals[1] ?: "lx", descriptionText:"${friendly} illuminance is ${newVals[0]} ${newVals[1] ?: 'lx'}"]], namespace: "community"],
            battery: [type: "Generic Component Battery",                      event: [[name: "battery", value: newVals[0], unit: newVals[1] ?: "%", descriptionText:"${friendly} battery is ${newVals[0]} ${newVals[1] ?: '%'}"]], namespace: "community"],
            power: [type: "Generic Component Power Meter",                    event: [[name: "power", value: newVals[0], unit: newVals[1] ?: "W", descriptionText:"${friendly} power is ${newVals[0]} ${newVals[1] ?: 'W'}"]]],
            pressure: [type: "Generic Component Pressure Sensor",             event: [[name: "pressure", value: newVals[0], unit: newVals[1] ?: "", descriptionText:"${friendly} pressure is ${newVals[0]} ${newVals[1] ?: ''}"]], namespace: "community"],
            carbon_dioxide: [type: "Generic Component Carbon Dioxide Sensor", event: [[name: "carbonDioxide", value: newVals[0], unit: newVals[1] ?: "ppm", descriptionText:"${friendly} carbon_dioxide is ${newVals[0]} ${newVals[1] ?: 'ppm'}"]], namespace: "community"],
            volatile_organic_compounds_parts: [type: "Generic Component Volatile Organic Compounds Sensor",
                                                                              event: [[name: "voc", value: newVals[0], unit: newVals[1] ?: "ppb", descriptionText:"${friendly} volatile_organic_compounds_parts is ${newVals[0]} ${newVals[1] ?: 'ppb'}"]], namespace: "community"],
            volatile_organic_compounds: [type: "Generic Component Volatile Organic Compounds Sensor",
                                                                              event: [[name: "voc", value: newVals[0], unit: newVals[1] ?: "µg/m³", descriptionText:"${friendly} volatile_organic_compounds is ${newVals[0]} ${newVals[1] ?: 'µg/m³'}"]], namespace: "community"],
            radon: [type: "Generic Component Radon Sensor",                   event: [[name: "radon", value: newVals[0], unit: newVals[1], descriptionText:"${friendly} radon is ${newVals[0]} ${newVals[1]}"]], namespace: "community"],
            temperature: [type: "Generic Component Temperature Sensor",       event: [[name: "temperature", value: newVals[0], unit: newVals[1] ?: "°", descriptionText:"${friendly} temperature is ${newVals[0]} ${newVals[1] ?: '°'}"]]],
            voltage: [type: "Generic Component Voltage Sensor",               event: [[name: "voltage", value: newVals[0], unit: newVals[1] ?: "V", descriptionText:"${friendly} voltage is ${newVals[0]} ${newVals[1] ?: 'V'}"]]],
            energy: [type: "Generic Component Energy Meter",                  event: [[name: "energy", value: newVals[0], unit: newVals[1] ?: "kWh", descriptionText:"${friendly} energy is ${newVals[0]} ${newVals[1] ?: 'kWh'}"]]],
            unknown: [type: "Generic Component Unknown Sensor",               event: [[name: "unknown", value: newVals[0], unit: newVals[1] ?: "", descriptionText:"${friendly} value is ${newVals[0]} ${newVals[1] ?: ''}"]], namespace: "community"],
            timestamp: [type: "Generic Component TimeStamp Sensor",           event: [[name: "timestamp", value: newVals[0], descriptionText:"${friendly} time is ${newVals[0]}"]], namespace: "community"],
            pm25: [type: "Generic Component pm25 Sensor",                     event: [[name: "pm25", value: newVals[0], unit: newVals[1] ?: "µg/m³", descriptionText:"${friendly} pm2.5 is ${newVals[0]} ${newVals[1] ?: 'µg/m³'}"]], namespace: "community"],
        ]
    if (!mapping[device_class]) device_class = "unknown"
    return mapping[device_class]
}

def translateCovers(device_class, newVals, friendly, origin)
{
    def mapping =
        [
            shade: [type: "Generic Component Window Shade",             event: [[name: "windowShade", value: newVals[0] ?: "unknown", type: origin, descriptionText:"${friendly} was turned ${newVals[0]} [${origin}]"],[name: "position", value: (null != newVals?.getAt(1)) ? newVals[1] : "unknown", type: origin, descriptionText:"${friendly} was turned ${newVals[1]} [${origin}]"]], namespace: "community"],
            garage: [type: "Generic Component Garage Door Control",     event: [[name: "door", value: newVals[0] ?: "unknown", type: origin, descriptionText:"${friendly} was turned ${newVals[0]} [${origin}]"]], namespace: "community"],
            door: [type: "Generic Component Door Control",              event: [[name: "door", value: newVals[0] ?: "unknown", type: origin, descriptionText:"${friendly} was turned ${newVals[0]} [${origin}]"]], namespace: "community"],
            blind: [type: "Generic Component Window Blind",             event: [[name: "windowBlind", value: newVals[0] ?: "unknown", type: origin, descriptionText:"${friendly} was turned ${newVals[0]} [${origin}]"],[name: "position", value: newVals[1] ?: "unknown", type: origin, descriptionText:"${friendly} position was set to ${newVals[1] ?: "unknown"} [${origin}]"],[name: "tilt", value: newVals[2] ?: "unknown", type: origin, descriptionText:"${friendly} tilt was set to ${newVals[2] ?: "unknown"} [${origin}]"]], namespace: "community"],
        ]
    return mapping[device_class]
}

def translateDevices(domain, newVals, friendly, origin)
{
    def mapping =
        [
            fan: [type: "Generic Component Fan Control",                event: [[name: "switch", value: newVals[0], type: origin, descriptionText:"${friendly} was turned ${newVals[0]} [${origin}]"],[name: "speed", value: newVals[1], type: origin, descriptionText:"${friendly} speed was set to ${newVals[1]} [${origin}]"],[name: "level", value: newVals[2], type: origin, descriptionText:"${friendly} level was set to ${newVals[2]} [${origin}]"]]],
            switch: [type: "Generic Component Switch",                  event: [[name: "switch", value: newVals[0], type: origin, descriptionText:"${friendly} was turned ${newVals[0]} [${origin}]"]]],
            device_tracker: [type: "Generic Component Presence Sensor", event: [[name: "presence", value: newVals[0] == "home" ? "present":"not present", descriptionText:"${friendly} is updated"]], namespace: "community"],
            lock: [type: "Generic Component Lock",                      event: [[name: "lock", value: newVals[0] ?: "unknown", type: origin, descriptionText:"${friendly} was turned ${newVals[0]} [${origin}]"]]],
            climate: [type: "Generic Component Thermostat",             event: [[name: "thermostatMode", value: newVals[0], descriptionText: "${friendly} is set to ${newVals[0]}"],[name: "temperature", value: newVals[1], descriptionText: "${friendly}'s current temperature is ${newVals[1]} degree"],[name: "thermostatOperatingState", value: newVals[2], descriptionText: "${friendly}'s mode is ${newVals[2]}"],[name: "thermostatFanMode", value: newVals[3], descriptionText: "${friendly}'s fan is set to ${newVals[3]}"],[name: "thermostatSetpoint", value: newVals[4], descriptionText: "${friendly}'s temperature is set to ${newVals[4]} degree"],[name: "coolingSetpoint", value: newVals[5] ?: newVals[4], descriptionText: "${friendly}'s cooling temperature is set to ${newVals[5] ?: newVals[4]} degrees"],[name: "heatingSetpoint", value: newVals[6] ?: newVals[4], descriptionText: "${friendly}'s heating temperature is set to ${newVals[6] ?: newVals[4]} degrees"],[name: "supportedThermostatModes", value: newVals[7], descriptionText: "${friendly} supportedThermostatModes were set to ${newVals[7]}"]]],
            input_boolean: [type: "Generic Component Switch",           event: [[name: "switch", value: newVals[0], type: origin, descriptionText:"${friendly} was turned ${newVals[0]} [${origin}]"]]],
            input_number: [type: "Generic Component Number",            event: [[name: "number", value: newVals[0], unit: newVals[1] ?: "", type: origin, descriptionText:"${friendly} was set to ${newVals[0]} ${newVals[1] ?: ''} [${origin}]"],[name: "minimum", value: newVals[2], descriptionText:"${friendly} minimum value is ${newVals[2]}"],[name: "maximum", value: newVals[3], descriptionText:"${friendly} maximum value is ${newVals[3]}"],[name: "step", value: newVals[4], descriptionText:"${friendly} step is ${newVals[4]}"]], namespace: "community"],
            number: [type: "Generic Component Number",                  event: [[name: "number", value: newVals[0], unit: newVals[1] ?: "", type: origin, descriptionText:"${friendly} was set to ${newVals[0]} ${newVals[1] ?: ''} [${origin}]"],[name: "minimum", value: newVals[2], descriptionText:"${friendly} minimum value is ${newVals[2]}"],[name: "maximum", value: newVals[3], descriptionText:"${friendly} maximum value is ${newVals[3]}"],[name: "step", value: newVals[4], descriptionText:"${friendly} step is ${newVals[4]}"]], namespace: "community"],
        ]
    return mapping[domain]
}

def translateLight(device_class, newVals, friendly, origin)
{
    def mapping =
        [
            rgbwe: [type: "Generic Component RGBW Light Effects",       event: [[name: "switch", value: newVals[0], type: origin, descriptionText:"${friendly} was turned ${newVals[0]} [${origin}]"],[name: "level", value: newVals[1], type: origin, descriptionText:"${friendly} level was set to ${newVals[1]}"],[name: "hue", value: newVals[2], descriptionText:"${friendly} hue was set to ${newVals[2]}"],[name: "saturation", value: newVals[3], descriptionText:"${friendly} saturation was set to ${newVals[3]}"],[name: "colorTemperature", value: newVals[4] ?: 'emulated', descriptionText:"${friendly} color temperature was set to ${newVals[4] ?: 'emulated'}°K"],[name: "lightEffects", value: newVals[5]],[name: "effectName", value: newVals[6] ?: "none", descriptionText:"${friendly} effect was set to ${newVals[6] ?: 'none'}"]]],
            rgbw: [type: "Generic Component RGBW",                      event: [[name: "switch", value: newVals[0], type: origin, descriptionText:"${friendly} was turned ${newVals[0]} [${origin}]"],[name: "level", value: newVals[1], type: origin, descriptionText:"${friendly} level was set to ${newVals[1]}"],[name: "hue", value: newVals[2], descriptionText:"${friendly} hue was set to ${newVals[2]}"],[name: "saturation", value: newVals[3], descriptionText:"${friendly} saturation was set to ${newVals[3]}"],[name: "colorTemperature", value: newVals[4], descriptionText:"${friendly} color temperature was set to ${newVals[4]}°K"]]],
            rgb: [type: "Generic Component RGB",                        event: [[name: "switch", value: newVals[0], type: origin, descriptionText:"${friendly} was turned ${newVals[0]} [${origin}]"],[name: "level", value: newVals[1], type: origin, descriptionText:"${friendly} level was set to ${newVals[1]}"],[name: "hue", value: newVals[2], descriptionText:"${friendly} hue was set to ${newVals[2]}"],[name: "saturation", value: newVals[3], descriptionText:"${friendly} saturation was set to ${newVals[3]}"]]],
            ct: [type: "Generic Component CT",                          event: [[name: "switch", value: newVals[0], type: origin, descriptionText:"${friendly} was turned ${newVals[0]} [${origin}]"],[name: "level", value: newVals[1], type: origin, descriptionText:"${friendly} level was set to ${newVals[1]}"],[name: "colorTemperature", value: newVals[2], descriptionText:"${friendly} color temperature was set to ${newVals[2]}°K"]]],
            dimmer: [type: "Generic Component Dimmer",                  event: [[name: "switch", value: newVals[0], type: origin, descriptionText:"${friendly} was turned ${newVals[0]} [${origin}]"],[name: "level", value: newVals[1], type: origin, descriptionText:"${friendly} level was set to ${newVals[1]} [${origin}]"]]],
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
        if (mapping.event[0].value == "unavailable") mapping.event = [[name: "healthStatus", value: "offline", descriptionText:"${friendly} is offline"]]
        else mapping.event += [name: "healthStatus", value: "online", descriptionText:"${friendly} is online"]
        ch.parse(mapping.event)
    }
}

def createChild(deviceType, entity, friendly, namespace = null)
{
    def ch = getChildDevice("${device.id}-${entity}")
    if (!ch) ch = addChildDevice(namespace ?: "hubitat", deviceType, "${device.id}-${entity}", [name: "${entity}", label: "${friendly}", isComponent: false])
    return ch
}

def componentOn(ch){
    if (logEnable) log.info("received on request from ${ch.label}")

    if (!ch.currentValue("level") || ch.hasCapability("LightEffects")) {
        data = [:]
    }
    else {
        data = [brightness_pct: "${ch.currentValue("level")}"]
    }
    
    executeCommand(ch, "turn_on", data)
}

def componentOff(ch){
    if (logEnable) log.info("received off request from ${ch.label}")
    
    if(ch.getSupportedAttributes().contains("thermostatMode"))
    {
        // since componentOff() is not unique across Hubitat device types, catch this special case
        componentOffTStat(ch)
        return
    }

    data = [:]
    executeCommand(ch, "turn_off", data)
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
        data = [brightness_pct: "${level}", transition: "${transition}"]
        executeCommand(ch, "turn_on", data)
    }
}

def componentSetColor(ch, color, transition=1)
    {
    if (logEnable) log.info("received setColor request from ${ch.label}")

    convertedHue = Math.round(color.hue * 360/100)
    
    data = [brightness_pct: "${color.level}", hs_color: ["${convertedHue}", "${color.saturation}"], transition: "${transition}"]
    executeCommand(ch, "turn_on", data)
}

def componentSetColorTemperature(ch, colortemperature, level, transition=1)
    {
    if (logEnable) log.info("received setColorTemperature request from ${ch.label}")

    if (!level) level = ch.currentValue("level")
    if (!transition) transition = 1

    data = [brightness_pct: "${level}", color_temp_kelvin: "${colortemperature}", transition: "${transition}"]
    executeCommand(ch, "turn_on", data)
}

def componentSetHue(ch, hue, transition=1)
    {
    if (logEnable) log.info("received setHue request from ${ch.label}")
    
    convertedHue = Math.round(hue * 360/100)
    
    data = [brightness_pct: "${ch.currentValue("level")}", hs_color: ["${convertedHue}", "${ch.currentValue("saturation")}"], transition: "${transition}"]
    executeCommand(ch, "turn_on", data)
}

def componentSetSaturation(ch, saturation, transition=1)
    {
    if (logEnable) log.info("received setSaturation request from ${ch.label}")
    
    convertedHue = Math.round(ch.currentValue("hue") * 360/100)
    
    data = [brightness_pct: "${ch.currentValue("level")}", hs_color: ["${convertedHue}", "${saturation}"], transition: "${transition}"]
    executeCommand(ch, "turn_on", data)
}

def componentSetEffect(ch, effectNumber)
    {
    if (logEnable) log.info("received setEffect request from ${ch.label}")

    def effectsList = ch.currentValue("lightEffects")?.tokenize(',[]')
    def max = effectsList.size()
    effectNumber = effectNumber.toInteger()
    effectNumber = (effectNumber < 1) ? 1 : ((effectNumber > max) ? max : effectNumber)   
        
    data = [effect: effectsList[effectNumber - 1].trim()]
    executeCommand(ch, "turn_on", data)
}

def componentSetNextEffect(ch) {
    log.warn "setNextEffect not implemented"
}

def componentSetPreviousEffect(ch) {
    log.warn "setPreviousEffect not implemented"
}

def componentSetSpeed(ch, speed) {
    if (logEnable) log.info("received setSpeed request from ${ch.label}, with speed = ${speed}")
    int percentage = 0
    switch (speed) {
        case "on":
            data = [:]
            executeCommand(ch, "turn_on", data)
            break
        case "off":
            data = [:]
            executeCommand(ch, "turn_off", data)
            break
        case "low":
        case "medium-low":
            data = [percentage: "25"]
            executeCommand(ch, "turn_on", data)
            break
        case "auto":
        case "medium":
            data = [percentage: "50"]
            executeCommand(ch, "turn_on", data)
            break
        case "medium-high":
            data = [percentage: "75"]
            executeCommand(ch, "turn_on", data)
            break
        case "high":
            data = [percentage: "100"]
            executeCommand(ch, "turn_on", data)
            break
        default:
            if (logEnable) log.info "No case defined for Fan setSpeed(${speed})"
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

void operateCover(ch, op) {
    if (logEnable) log.info("received ${op} request from ${ch.label}")

    service = op + "_cover"
    data = [:]
    executeCommand(ch, service, data)
}

void componentSetPosition(ch, pos) {
    
    executeCommand(ch, "set_cover_position", [position: pos])
}

void componentSetTiltLevel(ch, tilt) {
    
    executeCommand(ch, "set_cover_tilt_position", [position: tilt])
}

void componentStartPositionChange(ch, dir) {
    
    if(["open", "close"].contains(dir)) {
        operateCover(ch, dir)
    }
}

void componentStopPositionChange(ch) {
    
    operateCover(ch, "stop")
}

void componentLock(ch) {
    operateLock(ch, "lock")
}

void componentUnlock(ch) {
    operateLock(ch, "unlock")
}

void operateLock(ch, op)
{
    if (logEnable) log.info("received ${op} request from ${ch.label}")

    data = [:]
    executeCommand(ch, op, data)
}

def componentSetNumber(ch, newValue) {
    if (logEnable) log.info("received set number to ${newValue} request from ${ch.label}")
    newValue = Math.round(newValue / ch.currentValue("step")) * ch.currentValue("step")
    if (newValue < ch.currentValue("minimum")) newValue = ch.currentValue("minimum")
    if (newValue > ch.currentValue("maximum")) newValue = ch.currentValue("maximum")
    data = [value: newValue]
    executeCommand(ch, "set_value", data)
}

def componentRefresh(ch){
    if (logEnable) log.info("received refresh request from ${ch.label}")
    // special handling since domain is fixed 
    entity = ch.name
    messUpd = JsonOutput.toJson([id: state.id, type: "call_service", domain: "homeassistant", service: "update_entity", service_data: [entity_id: entity]])
    state.id = state.id + 1
    if (logEnable) log.debug("messUpd = ${messUpd}")
    interfaces.webSocket.sendMessage("${messUpd}")
}

def componentSetThermostatMode(ch, thermostatmode){
    if (logEnable) log.info("received setThermostatMode request from ${ch.label}")

    switch(thermostatmode)
	{
	case "auto":
	    data = [target_temp_high: ch.currentValue("coolingSetpoint"), target_temp_low: ch.currentValue("heatingSetpoint"), hvac_mode: "heat_cool"]
	    service = "set_temperature"
        break
	case "emergencyHeat":
	    thermostatmode = "heat"
	case "heat":
	case "cool":
	    data = [temperature: ch.currentValue("thermostatSetpoint"), hvac_mode: thermostatmode]
	    service = "set_temperature"
	break
	case "off":
	    data =  [hvac_mode: thermostatmode]
	    service = "set_hvac_mode"
	break
	}
    executeCommand(ch, service, data)
}

def componentSetCoolingSetpoint(ch, temperature){
    if (logEnable) log.info("received setCoolingSetpoint request from ${ch.label}")
    
    tmode = ch.currentValue("thermostatMode")
    if (logEnable) log.info("thermostatMode is ${tmode}")
	
    if (tmode == "auto") {
        data = [target_temp_high: temperature, target_temp_low: ch.currentValue("heatingSetpoint"), hvac_mode: "heat_cool"]
	}
    else {
	if (tmode == "emergencyHeat") tmode = "heat"
	data = [temperature: temperature, hvac_mode: tmode]
	}
    executeCommand(ch, "set_temperature", data)
}

def componentSetHeatingSetpoint(ch, temperature) {
    if (logEnable) log.info("received setHeatingSetpoint request from ${ch.label}")

    tmode = ch.currentValue("thermostatMode")
    if (logEnable) log.info("thermostatMode is ${tmode}")
	
    if (tmode == "auto") {
	data = [target_temp_high: ch.currentValue("coolingSetpoint"), target_temp_low: temperature, hvac_mode: "heat_cool"]
    }
    else {
	if (tmode == "emergencyHeat") tmode = "heat"
	data = [temperature: temperature, hvac_mode: tmode]
    }
    executeCommand(ch, "set_temperature", data)
}

def componentSetThermostatFanMode(ch, fanmode) {
    if (logEnable) log.info("received fanmode request from ${ch.label}")

    if (fanmode == "circulate") {
        data = [hvac_mode: "fan_only"]
        executeCommand(ch, "set_hvac_mode", data)
    }
    else {    
        data = [fan_mode: fanmode]
        executeCommand(ch, "set_fan_mode", data)
    }
}

def componentAuto(ch)
{
    componentSetThermostatMode(ch, "auto")
}

def componentCool(ch)
{
    componentSetThermostatMode(ch, "cool")
}

def componentEmergencyHeat(ch)
{
    componentSetThermostatMode(ch, "emergencyHeat")
}

def componentFanAuto(ch)
{
    componentSetThermostatMode(ch, "auto")
}

def componentFanCirculate(ch)
{
    componentSetThermostatFanMode(ch, "circulate")
}

def componentFanOn(ch)
{
    componentSetThermostatFanMode(ch, "on")
}

def componentHeat(ch)
{
    componentSetThermostatMode(ch, "heat")
}

def componentOffTStat(ch)
{
    componentSetThermostatMode(ch, "off")
}

def componentStartLevelChange(ch)
{
    log.warn ("Start level change not supported")
}

def componentStopLevelChange(ch)
{
    log.warn ("Stop level change not supported")
}

def closeConnection() {
    if (logEnable) log.debug("Closing connection...")   
    state.wasExpectedClose = true
    interfaces.webSocket.close()
}

def executeCommand(ch, service, data)
{    
    entity = ch?.name
    domain = entity?.tokenize(".")[0]

    messUpd = [id: state.id, type: "call_service", domain: domain, service: service, service_data : [entity_id: entity] + data]
    state.id = state.id + 1

    messUpdStr = JsonOutput.toJson(messUpd)
    if (logEnable) log.debug("messUpdStr = ${messUpdStr}")
    interfaces.webSocket.sendMessage(messUpdStr)    
}

def deleteAllChildDevices() {
    log.info "Uninstalling all Child Devices"
    getChildDevices().each {
          deleteChildDevice(it.deviceNetworkId)
       }
}
