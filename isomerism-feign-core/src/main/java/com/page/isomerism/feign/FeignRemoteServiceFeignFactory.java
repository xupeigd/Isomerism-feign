package com.page.isomerism.feign;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 手动装配Feign Client的Bean
 *
 * @author page.xee
 * @date 2018/8/23
 */
@Service
@ConditionalOnProperty(
        value = {"isomerism.feign.hystrix.enabled"},
        havingValue = "false",
        matchIfMissing = true
)
public final class FeignRemoteServiceFeignFactory
        extends AbstractRemoteServiceFeignFactory {

}
