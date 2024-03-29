# grafananet-push

## Configuring Hubitat

First, the app must be installed.
Under "Apps Code" click "New App", then "Import" and use the following URL:
```
https://raw.githubusercontent.com/jmh045000/grafananet-push/main/grafananet-push.groovy
```
(Alternvatively you can copy/paste the code yourself.)

To use this plugin, you need to create an API key on grafana.net for your own account.
The "Connect Data" for custom HTTP metrics helps set that up.

![image](https://github.com/jmh045000/grafananet-push/assets/1126676/1e768ca0-8103-464b-86d7-d2fc4dabbdee)

Create a new token, name it something you can remember for this purpose.

![image](https://github.com/jmh045000/grafananet-push/assets/1126676/d77cb516-876a-4f0c-a45e-842258b99a56)

Copy the key that is created, this is the value in the `Grafana API Key` paramter.

![image](https://github.com/jmh045000/grafananet-push/assets/1126676/30f08bcf-4f64-4aa1-bfa2-5762badf37c1)

In the code examples, you're provided the `Grafana User ID`, `Grafana API Key`, and `Grafana Host` parameters, highlighted below.

![image](https://github.com/jmh045000/grafananet-push/assets/1126676/77807c6c-cf41-4b44-a132-d9af3eaf08c6)

Fill these in on the hubitat App configuration as below, and save the settings.

![image](https://github.com/jmh045000/grafananet-push/assets/1126676/583fd597-b1a2-4ae9-af15-54e59e111f8b)

Make sure that at least a device is enabled.
The optional preferences allow specifying behavior settings of the plugin, the defaults should be fine.

## Using the metrics in Grafana

The metrics are published as the attribute name reported by Hubitat.
The full list of possible attributes can be seen in the [Hubitat Documentation](https://docs2.hubitat.com/developer/driver/capability-list), although not all attributes are supported by this app.
The list of supported attributes are documented in the code [here](https://github.com/jmh045000/grafananet-push/blob/main/grafananet-push.groovy#L85) (the `encode` function in case the link is stale).
