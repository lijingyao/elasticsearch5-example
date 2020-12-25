package com.lijingyao.es.upgrade.repository;


import com.lijingyao.es.upgrade.entity.ItemDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * https://docs.spring.io/spring-data/elasticsearch/docs/current/reference/html/
 * <p>
 * Created by lijingyao on 2018/1/15 11:03.
 */
@Repository
public interface ItemDocumentRepository extends ElasticsearchRepository<ItemDocument, String> {


}
