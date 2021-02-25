/*

* 0.1.22     2021-02-24 tomw               Entity filtering (in work)
* 0.1.221    2021-02-24 tomw               Usability via friendly_name on filtering and select/clear all
* 0.1.222    2021-02-24 tomw               Polishing usability
* 0.1.223    2021-02-24 tomw               Cosmetic changes and configure driver parameters from app

 */

definition(
    name: "Home Assistant Device Bridge configuration",
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
        section
        {
            input ("ip", "text", title: "IP", description: "HomeAssistant IP Address", required: true)
            input ("port", "text", title: "Port", description: "HomeAssistant Port Number", required: true, defaultValue: "8123")
            input ("token", "text", title: "Token", description: "HomeAssistant Access Token", required: true)
            input name: "enableLogging", type: "bool", title: "Enable debug logging?", defaultValue: false, required: true
        }
        section
        {
            href(page: "mainPage2", title: "<b>Discover and select devices</b>", description: "Query Home Assistant for all currently configured devices, and indicate which to exclude from Hubitat.", params: [runDiscovery : true])
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
            //def resp = [data: [[entity_id: "fan.ABC123", attributes: [friendly_name: "friendly ABC"]], [entity_id: "fan.ABC124", attributes: [friendly_name: "friendly ABC"]], [entity_id: "fan.ABC125", attributes: [friendly_name: "friendly ABC"]]]]
            logDebug("states response = ${resp?.data}")
            
            if(resp?.data)
            {
                resp.data.each
                {
                    domain = it.entity_id?.tokenize(".")?.getAt(0)
                    if(["fan", "switch", "light", "binary_sensor", "sensor"].contains(domain))
                    {
                        state.entityList.put(it.entity_id, "${it.attributes?.friendly_name} (${it.entity_id})")
                    }
                }
            }
        }
        
        section
        {
            paragraph "<b>Discovered devices:</b> ${(!state?.entityList?.isEmpty() && state?.entityList) ? state.entityList.toString() : "none"}"
        }
        
        section
        {
            input name: "excludeList", type: "enum", title: "Select any devices to <b>exclude</b> from Home Assistant Device Bridge", options: state.entityList, required: false, multiple: true
            input name: "selectAll", type: "bool", title: "Select all devices?", defaultValue: false, submitOnChange: true
            input name: "clearAll", type: "bool", title: "De-select all devices?", defaultValue: false, submitOnChange: true
        }
        
        section
        {
            href(page: "mainPage1", title: "<b>Return to previous page</b>", description: "")
        }
        
        if(selectAll)
        {
            app.updateSetting("excludeList", state.entityList.keySet().toList())
            app.updateSetting("selectAll", false)            
        }
        
        if(clearAll)
        {
            app.updateSetting("excludeList", [])
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
        ch = addChildDevice("ymerj", "HomeAssistant Hub Parent", "HE-HA-control", [name: "Home Assistant Device Bridge", label: "Home Assistant Device Bridge", isComponent: false])
    }
    
    if(ch)
    {
        // propoagate our settings to the child
        ch.updateSetting("ip", ip)
        ch.updateSetting("port", port)
        ch.updateSetting("token", token)

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
        ]
    
    if(body)
    {
        params['body'] = body
    }
 
    return params
}

def getBaseURI()
{
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
