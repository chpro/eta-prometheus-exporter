
package chpro.eta.proexp.metrics;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chpro.eta.api.client.ClientService;
import chpro.eta.api.client.data.Eta;
import chpro.eta.proexp.metrics.config.MetricConfig;
import chpro.eta.proexp.metrics.config.MetricConfig.Metric;
import io.micronaut.context.annotation.Value;
import io.prometheus.client.Collector;
import io.prometheus.client.Collector.MetricFamilySamples.Sample;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class MetricsCollector extends Collector {

    private static final String UNIT_STATUS = "status";

    private static final Logger LOG = LoggerFactory.getLogger(MetricsCollector.class);

    @Value("${eta.prometheus-exporter.host}")
    protected String host;

    @Inject
    protected MetricConfig metrics;

    @Inject
    ClientService clientService;

    @Override
    public List<MetricFamilySamples> collect() {
        LOG.info(String.format("Starting to collect %d metrics", metrics.getMetrics().size()));
        return metrics.getMetrics().stream().map(metric -> createFamilySample(metric)).toList();
    }

    protected MetricFamilySamples createFamilySample(Metric metricConfig) {
        // copy metric config because we may change labels
        metricConfig = new Metric(metricConfig);
        String help = metricConfig.getHelp();
        String name = metricConfig.getName();
        String unit = metricConfig.getUnit();
        List<String> labelNames = metricConfig.getLabelNames();
        List<String> labelValues = metricConfig.getLabelValues();
        
        Collector.Type type = Collector.Type.valueOf(metricConfig.getType().toUpperCase());

        double value = getValue(metricConfig);
        LOG.debug("Colllecting metric {}", metricConfig);

        switch (type) {
        case GAUGE:
        case COUNTER: {
            List<Sample> samples = Collections.singletonList(new Sample(name, labelNames, labelValues, value));
            MetricFamilySamples metric = new MetricFamilySamples(name, unit, type, help, samples);
            return metric;
        }
        case GAUGE_HISTOGRAM:
        case HISTOGRAM:
        case SUMMARY:
        case STATE_SET:
        case INFO:
        case UNKNOWN:
        default:
            throw new RuntimeException("Type not implemented yet " + type);

        }
    }

    protected double getValue(Metric metricConfig) {
        try {
            Eta eta = clientService.getUserVar(InetAddress.getByName(host), metricConfig.getUri());
            chpro.eta.api.client.data.uservar.Value valueXml = eta.getValue();
            LOG.trace("Processing xml data: ", eta.toString());
            BigDecimal divisor = valueXml.getScaleFactor() == 0 ? BigDecimal.ONE : BigDecimal.valueOf(valueXml.getScaleFactor());
            BigDecimal value = BigDecimal.valueOf(Double.parseDouble(valueXml.getValue()));
            
            String unit = metricConfig.getUnit();
            if (UNIT_STATUS.equalsIgnoreCase(unit)) {
                metricConfig.getLabelNames().add(UNIT_STATUS);
                metricConfig.getLabelValues().add(valueXml.getStrValue());
                return value.subtract(BigDecimal.valueOf(Long.parseLong(valueXml.getAdvTextOffset()))).doubleValue();
            } else {
                return value.divide(divisor).doubleValue();
            }
        } catch (Exception e) {
            LOG.error("Was not able to get value of " + metricConfig, e);
            return 0.0d;
        }

    }

    @Override
    @PostConstruct
    public <T extends Collector> T register() {
        LOG.info("Registering metrics collector " + this.getClass().getName());
        return super.register();
    }

}
