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
* Copyright 2021 tomw
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
* in compliance with the License. You may obtain a copy of the License at: http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
* on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
* for the specific language governing permissions and limitations under the License.
*
* Version Control:
* 0.1.22     2021-02-24 tomw               Optional configuration app to selectively filter out Home Assistant devices
* 0.1.23     2021-02-25 Dan Ogorchock      Switched logic from Exclude to Include to make more intuitive.  Sorted Device List.
* 0.1.32     2021-09-27 kaimyn             Add option to use HTTPS support in configuration app
*/

definition(
    name: "Home Assistant Device Bridge",
    namespace: "tomw",
    author: "tomw",
    description: "",
    category: "Convenience",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "")

preferences
{
    page(name: "mainPage1")
    page(name: "mainPage2")
}

def mainPage1()
{
    dynamicPage(name: "mainPage1", title: "", install: false, uninstall: true)
    {
        section("Home Assistant Device Bridge")
        {
            input ("ip", "text", title: "Home Assistant IP Address", description: "HomeAssistant IP Address", required: true)
            input ("port", "text", title: "Home Assistant Port", description: "HomeAssistant Port Number", required: true, defaultValue: "8123")
            input ("token", "text", title: "Home Assistant Long-Lived Access Token", description: "HomeAssistant Access Token", required: true)
            input name: "secure", type: "bool", title: "Require secure connection", defaultValue: false, required: true
            input name: "ignoreSSLIssues", type: "bool", title: "Ignore SSL Issues", defaultValue: false, required: true
            input name: "enableLogging", type: "bool", title: "Enable debug logging?", defaultValue: false, required: true
            
        }
        section("Please note, it may take some time to retrieve all entities from Home Assistant.")
        {
            href(page: "mainPage2", title: "<b>Discover and select devices</b>", description: "Query Home Assistant for all currently configured devices.  Please select which entities to Import to Hubitat.", params: [runDiscovery : true])
        }
        section("App Name") {
            label title: "Optionally assign a custom name for this app", required: false
        }        
    }
}

def mainPage2(params)
{
    dynamicPage(name: "mainPage2", title: "", install: true, uninstall: true)
    {
        if(params?.runDiscovery)
        {
            state.entityList = [:]            
            def domain
            // query HA to get entity_id list
            def resp = httpGetExec(genParamsMain("states"))
            logDebug("states response = ${resp?.data}")
            
            if(resp?.data)
            {
                resp.data.each
                {
                    domain = it.entity_id?.tokenize(".")?.getAt(0)
                    if(["fan", "switch", "light", "binary_sensor", "sensor", "device_tracker", "cover"].contains(domain))
                    {
                        state.entityList.put(it.entity_id, "${it.attributes?.friendly_name} (${it.entity_id})")
                    }
                }
                state.entityList = state.entityList.sort { it.value }
            }
        }
        
//        section
//        {
//            paragraph "<b>Discovered devices:</b> ${(!state?.entityList?.isEmpty() && state?.entityList) ? state.entityList.toString() : "none"}"
//        }
        
        section
        {
            input name: "includeList", type: "enum", title: "Select any devices to <b>include</b> from Home Assistant Device Bridge", options: state.entityList, required: true, multiple: true
            input name: "selectAll", type: "bool", title: "Select all devices?", defaultValue: false, submitOnChange: true
            input name: "clearAll", type: "bool", title: "De-select all devices?", defaultValue: false, submitOnChange: true
        }
        
        section
        {
            href(page: "mainPage1", title: "<b>Return to previous page</b>", description: "")
        }
        
        if(selectAll)
        {
            app.updateSetting("includeList", state.entityList.keySet().toList())
            app.updateSetting("selectAll", false)            
        }
        
        if(clearAll)
        {
            app.updateSetting("includeList", [])
            app.updateSetting("clearAll", false)
        }
    }
}

def logDebug(msg)
{
    if(enableLogging)
    {
        log.debug "${msg}"
    }
}

def installed()
{
    def ch = getChildDevice("HE-HA-control")
    if(!ch)
    {
        ch = addChildDevice("ymerj", "HomeAssistant Hub Parent", "HE-HA-control", [name: "Home Assistant Device Bridge", label: "Home Assistant Device Bridge (${ip})", isComponent: false])
    }
    
    if(ch)
    {
        // propoagate our settings to the child
        ch.updateSetting("ip", ip)
        ch.updateSetting("port", port)
        ch.updateSetting("token", token)
        ch.updateSetting("secure", secure)

        ch.updated()
    }
}

def uninstalled()
{
    deleteChildren()
}

def deleteChildren()
{
    for(child in getChildDevices())
    {
        deleteChildDevice(child.getDeviceNetworkId())
    }
}

def updated()
{
    installed()
}

def genParamsMain(suffix, body = null)
{
    def params =
        [
            uri: getBaseURI() + suffix,
            headers:
            [
                'Authorization': "Bearer ${token}",
                'Content-Type': "application/json"
            ],
            ignoreSSLIssues: ignoreSSLIssues
        ]
    
    if(body)
    {
        params['body'] = body
    }
 
    return params
}

def getBaseURI()
{
    if(secure) return "https://${ip}:${port}/api/"
    return "http://${ip}:${port}/api/"
}

def httpGetExec(params, throwToCaller = false)
{
    logDebug("httpGetExec(${params})")
    
    try
    {
        def result
        httpGet(params)
        { resp ->
            if (resp)
            {
                //logDebug("resp.data = ${resp.data}")
                result = resp
            }
        }
        return result
    }
    catch (Exception e)
    {
        logDebug("httpGetExec() failed: ${e.message}")
        //logDebug("status = ${e.getResponse().getStatus().toInteger()}")
        if(throwToCaller)
        {
            throw(e)
        }
    }
}
