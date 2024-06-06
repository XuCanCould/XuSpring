package aop.metric;

import cn.xu.spring.annotation.Component;
import cn.xu.spring.aop.AnnotationProxyBeanPostProcessor;

@Component
public class MetricProxyBeanPostProcessor extends AnnotationProxyBeanPostProcessor<Metric> {

}
