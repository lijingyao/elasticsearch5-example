package com.lijingyao.es.upgrade.repository;


import com.lijingyao.es.upgrade.entity.ItemDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * https://docs.spring.io/spring-data/elasticsearch/docs/current/reference/html/
 * <p>
 * Created by lijingyao on 2017/6/5 11:03.
 */
public interface CommodityDocumentRepository extends ElasticsearchRepository<ItemDocument, String> {

//    CommodityDocument findById(String id);

}
