definition(
    name: "Push to Grafana.net",
    namespace: "james41235",
    author: "James Hall",
    description: "Pushes data to Grafana.net",
    category: "Observability",
    iconUrl: "",
    iconX2Url: ""
)

preferences {
    section("Grafana Configuration") {
        input name: "grafanaHost", type: "text", title: "Grafana Host", required: true
        input name: "grafanaUserId", type: "text", title: "Grafana User ID", required: true
        input name: "grafanaApiKey", type: "text", title: "Grafana API Key", required: true, noAutoComplete: true
    }
    section("Devices") {
        input name: "devices", type: "capability.*", title: "Devices to monitor", required: true, multiple: true
    }
    section(hideable: true, hidden: true, "Optional Preferences") {
        input name: "batchTime", type: "number", title: "Time to wait for event batching (seconds)", defaultValue: 10
        input name: "batchSize", type: "number", title: "Max size of queue before pushing a batch", defaultValue: 100
        input name: "pollingInterval", type: "number", title: "Polling Interval (minutes)", defaultValue: 5
    }
}

def installed() {
    log.debug "installed()"
    updated()
}

def updated() {
    log.debug "updated()"
    pollingInteval = pollingInterval.toInteger()
    unsubscribe()

    // Subscribe to all events for all enabled devices and set up our poll
    devices.each { device ->
        device.getCapabilities().each { capability ->
            capability.attributes.each { attribute ->
                subscribe(device, attribute.name, "handleEvent")
            }
        }
    }
    runIn(pollingInterval*60, "poll")

    // Set up our batch queue
    state.batchQueue = []
}

def uninstalled() {
    log.debug "uninstalled()"
    unsubscribe()
    unschedule("poll")
}

def handleEvent(evt) {
    device = evt.getDevice()
    enqueue(encode(device, evt.name, evt.value))
}

def poll() {
    devices.each { device ->
        device.getCapabilities().each { capability ->
            switch (capability.name) {
                case "Button":
                case "DoubleTapableButton":
                case "HoldableButton":
                case "PushableButton":
                case "ReleasableButton":
                    // Skip certain capabilities in the poll, as "current state" doesn't
                    // make as much sense.
                    break
                default:
                    capability.attributes.each { attribute ->
                        enqueue(encode(device, attribute.name, device.currentValue(attribute.name)))
                    }
                    break
            }
        }
    }
    runIn(pollingInterval*60, "poll")
}

def encode(device, attributeName, attributeValue) {
    deviceName = encodeStringForInflux(device.getName())
    deviceRoom = encodeStringForInflux(device.roomName)
    deviceDisplayName = encodeStringForInflux(device.getDisplayName())
    source = encodeStringForInflux(getLocation().getHub().name)
    // now() returns milliseconds, influx line protocol uses nanoseconds
    timestamp = now() * 1000000
    switch (attributeName) {
        case "humidity":
        case "temperature":
        case "numberOfButtons":
        case "pushed":
        case "held":
        case "released":
        case "battery"
            return "${encodeStringForInflux(attributeName)},deviceName=${deviceName},displayName=${deviceDisplayName},room=${deviceRoom},source=${source} value=${attributeValue} ${timestamp}"
    }
}

def enqueue(metric) {
    if (metric == null || metric == "") {
        // If we're passed a null or empty metric just return
        // We could prevent this in encode or the callers above, but this is common enough.
        return
    }
    state.batchQueue.add(metric)

    unschedule("sendBatch")
    if (state.batchQueue.size() < batchSize) {
        runIn(batchTime, "sendBatch")
    }
    else {
        sendBatch()
    }
}

def sendBatch() {
    API_KEY = "${grafanaUserId}:${grafanaApiKey}"
    GRAFANA_URI = "https://${grafanaHost}/api/v1/push/influx/write"

    body = state.batchQueue.join('\n')
    state.batchQueue = []

    log.debug "Sending metrics to ${GRAFANA_URI}"

    httpPost(
        uri: GRAFANA_URI,
        headers: [
            "Authorization": "Bearer ${API_KEY}",
            "Content-Type": "text/plain"
        ],
        body: body,
        { response -> 
            log.debug "Grafana response (${response.getStatus()}): ${response.getData()}"
        })
}

def encodeStringForInflux(str) {
    if (str == null) {
        return 'null'
    }

    str = str.replaceAll(" ", "\\\\ ") // Escape spaces.
    str = str.replaceAll(",", "\\\\,") // Escape commas.
    str = str.replaceAll("=", "\\\\=") // Escape equal signs.
    str = str.replaceAll("\"", "\\\\\"") // Escape double quotes.

    return str
}