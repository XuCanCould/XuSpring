package cn.xu.spring.jdbc.tx;

import cn.xu.spring.annotation.Transactional;
import cn.xu.spring.aop.AnnotationProxyBeanPostProcessor;

/**
 * created by Xu on 2024/5/24 16:27.
 */
public class TransactionalBeanPostProcessor extends AnnotationProxyBeanPostProcessor<Transactional> {
}
