package com.lijingyao.es.upgrade;

import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * Created by lijingyao on 2018/1/19 21:00.
 */
@Configuration
@EnableElasticsearchRepositories(basePackages = "com.lijingyao.es.upgrade.repository")
@ComponentScan(basePackages = { "com.lijingyao.es" })
public class SearchConfig {


    private static final Logger logger = LoggerFactory.getLogger(SearchConfig.class);


//    @Value("${elasticsearch.port}")
//    private int esPort;
//
//    @Value("${elasticsearch.clustername}")
//    private String esClusterName;
//
//    @Value("#{'${elasticsearch.hosts:localhost}'.split(',')}")
//    private List<String> hosts = new ArrayList<>();

    @Bean
    RestHighLevelClient client() {
        ClientConfiguration clientConfiguration = ClientConfiguration.builder()
                .connectedTo("localhost:9200")
                .build();
        RestHighLevelClient client =  RestClients.create(clientConfiguration).rest();
        logger.info("success config es7");
        return client;
    }

    @Bean
    public ElasticsearchOperations elasticsearchTemplate() {
        return new ElasticsearchRestTemplate(client());
    }


}
