# ETA prometheus exporter

The prometheus exporter uses chpro.eta.api to connect to eta heating system (ETA Heiztechnik GmbH) and collect data

## URL

By default the sever is listening to port `8080` the prometheus endpoint is mapped to `metrics`

`http://localhost:8080/metrics`

## Configuration

Configuration is done in application.yml

```
...

eta:
  prometheus-exporter:
    host: example.com
    metrics:
        -
          uri: /40/10201/0/0/12042
          type: counter | gauge
          name: name of the metric (if unit is given the name needs also to be suffixed with it)
          help: a detailed description for the metric
          label-names:
            - name1
            - name2
          label-values:
            - value_for_name1
            - value_for_name22
          unit: iso units (grams, celsius ..); status has special logic. It is calculated by (value - advTextOffset) additionally content of attribute text is added as label with name status (e.g. status = on)
        
...
```

### uri

The uri from xml without prefix `/usr/var` or `/usr/varinfo` e.g. `/40/10021/0/0/10990`.

### label-*

Static labels which should be added to the metric. Lists need to have same length

## Micronaut 3.7.4 Documentation

- [User Guide](https://docs.micronaut.io/3.7.4/guide/index.html)
- [API Reference](https://docs.micronaut.io/3.7.4/api/index.html)
- [Configuration Reference](https://docs.micronaut.io/3.7.4/guide/configurationreference.html)
- [Micronaut Guides](https://guides.micronaut.io/index.html)
---

- [Shadow Gradle Plugin](https://plugins.gradle.org/plugin/com.github.johnrengelman.shadow)