# elasticsearch5-example
upgrade of elasticsearch from 2.x to 5.x.with Spring Data Elasticsearch 3.0.2 and SpringBoot2

# SpringData3.x以及SpringBoot2集成Elasticsearch5.x

## 说明 
关于如何在SpringBoot 1的版本中集成Elasticsearch 2.x可以参考前文[Elasticsearch实践（二）在Springboot微服务中集成搜索服务](http://blog.csdn.net/lijingyao8206/article/details/78614536)。2017年底，SpringData项目终于更新了Elasticsearch5.x版本的对应release版本：**3.0.2.RELEASE**。本文结合一个本地的示例，对于ES版本升级进行简单介绍。目前ES已经出到了6.x版本。但是SpringData项目的更新速度一直比较慢。目前比较适合集成的版本就是**3.0.2.RELEASE**。同时，对应的Springboot也需要升级到2.x。     
因为Elasticsearch，以及其周边的相关平台都是强版本依赖的，所以升级的过程也会需要升级其他相关组件。本文主要介绍使用Docker容器来部署Elasticsearch5.x集群。                 

## Elasticsearch5.x以及2.x版本对比

### 5.x版本和2.x版本的对比     
Elasticsearch的5.x相当于3.x。之所以从2一跃跳到5，Elastic体系内还有logstash、Kibana,beats等产品。为了统一各产品版本，所以直接将Elasticsearch的版本从2提升到5。          
5.x版本提供了许多新的特性，并且基于Lucene6.x。下面简单列举一些升级的特性： 

#### 性能方面 
* 磁盘空间可以节省近一半    
* 索引时间减少近50%       
* 查询性能提升近30%
* 支持IPV6
  
  性能的具体数据可以查看[Elasticsearch性能监控](https://benchmarks.elastic.co/index.html)。elasricsearch性能的提升，主要是Lucene6版本之后的很多底层结构的优化。Lucene6使用[Block K-D trees](https://www.elastic.co/blog/lucene-points-6.0)数据结构来构建索引。BKD Trees是一种可以动态扩展的KD-tree结构。详细的解释可以参考这篇论文[Bkd-tree: A Dynamic Scalable kd-tree](http://www.madalgo.au.dk/~large/Papers/bkdsstd03.ps)。         
    
#### 功能新增

**1. 新增[Shrink  API](https://www.elastic.co/guide/en/elasticsearch/reference/master/indices-shrink-index.html)**   
Elasticsearch2.x的版本，在创建索引时指定了shard数，并且不支持修改。如果要改变shard数，只能重建索引。5.x新增的Shrink接口，可将分片数进行收缩成它的因数，如果原有的shard数=15，可以收缩成5个或者3个又或者1个。       
**2. 新增[Rollover API](https://www.elastic.co/guide/en/elasticsearch/reference/master/indices-rollover-index.html)**    
Rollover API对于日志类型的索引提供了友好的创建和管理。比如通过   
     
```
POST /my_alias/_rollover/my_new_index_name
{
  "conditions": {
    "max_age":   "7d",
    "max_docs":  1000,
    "max_size": "5gb"
  }
}
```     
可以给索引设置rollover规则：索引文档不超过1000个、最多保存7天的数据、每个索引文件不超过5G，超过限制会自动创建新的索引文件别名，如logs-2018.01.25-000002。       
**3. 新增[Reindex](https://www.elastic.co/guide/en/elasticsearch/reference/5.4/docs-reindex.html)**     
2.x版本的ES的索引重建一直是很麻烦的事情。5.x提供的Reindex可以直接在搜索集群中对数据进行重建。如下可以直接修改mapping。        

```      
curl -XPOST 'localhost:9200/_reindex?pretty' -H 'Content-Type: application/json' -d'
{
  "source": {
    "index": "twitter"
  },
  "dest": {
    "index": "new_twitter"
  }
}
' 
```     

Elasticsearch5.x还增加了[Task Manager](https://www.elastic.co/guide/en/elasticsearch/reference/current/tasks.html)、[Ingest Node](https://www.elastic.co/guide/en/elasticsearch/reference/master/ingest.html)等。

#### 其他特性


* Mapping变更中，String的类型的映射，被替换为两种： [text/keyword](https://www.elastic.co/guide/en/elasticsearch/reference/5.5/breaking_50_mapping_changes.html)。           
text：类似于全文形式，包括被分析。  
keyword：属于字符串类型的精确搜索。  
* 集群节点的配置方面，在节点启动时就会校验配置，如Max File Descriptors，Memory Lock, Virtual Memory等的验证,会启动抛出异常，降低后期稳定性的风险。同时更新配置时更加严格和保证原子性，如果其中一项失败，那个整个都会更新请求都会失败。      
* 插件方面，Delete-by-query和Update-by-query重新增加回core。2.x时被移除，以至于需要手动安装插件，5.x的插件构建在Reindex机制之上，已经可以直接使用了。    
* 允许现有parent类型新增child类型。详见[Allow adding additional child types referring to the same parent #17956](https://github.com/elastic/elasticsearch/issues/17956)。        
   
## 使用Docker部署Elasticsearch5.x 

### 使用Elastic官方镜像
Elasticsearch官方的镜像基于Centos，并且内置了X-Pack。安装过程可以参考[官方教程-5.6](https://www.elastic.co/guide/en/elasticsearch/reference/5.6/docker.html). 官方5.x版本的Docker镜像内置了X-pack。选择正确的版本即可。   

### 自定义Dockerfile安装ik分词器插件     
Docker官方的ES镜像[Dockerhub-ES](https://hub.docker.com/_/elasticsearch/)也是基于Elastic官方的[基础镜像](https://www.docker.elastic.co/)。对于[IK](https://github.com/medcl/elasticsearch-analysis-ik) 插件版本需要严格对应Elasticsearch的版本。    
分词器等插件的安装，可以直接基于官方的镜像，在Dockerfile中重新build自己的镜像。如下示例5.5.0版本Elasticsearch的Dockerfile：

```
FROM elasticsearch:5.5.0
RUN sed -i 's/deb.debian.org/mirrors.ustc.edu.cn/g' /etc/apt/sources.list
RUN apt-get update && apt-get install zip
RUN mkdir -p /usr/share/elasticsearch/plugins/ik
RUN cd /usr/share/elasticsearch/plugins/ik && wget https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v5.5.0/elasticse    arch-analysis-ik-5.5.0.zip && unzip elasticsearch-analysis-ik-5.5.0.zip

```    
编写好Dockerfile之后，再运行docker build即可。               
## SpringDataElasticsearch以及SpringBoot集成  

### SpringDataElasticsearch集成

#### SpringDataElasticsearch版本选择
Elasticsearch是强版本依赖的。相信所有在折腾过Elasticsearch的DevOps都被各种插件版本、关联Logstash,Kibana等的版本、集成SpringData相关版本弄得晕头转向。目前SpringDataElasticsearch更新了新的支持ES5.x的版本。本文示例Elasticsearch升级到版本**5.5**对应SpringDataElasticsearch版本**3.0.2.RELEASE**。使用Gradle构建的项目添加：    
     
```
compile 'org.springframework.data:spring-data-elasticsearch:3.0.2.RELEASE' 
```     
#### 升级注意
因为ES 5.x中的Mapping改变，所以原有的**@Field(type = FieldType.string)**类型的索引映射需要替换成**@Field(type = FieldType.keyword)**或者**@Field(type = FieldType.text)**。     
对于index原来的analyzed/not_analyzed/no也有相应的改变。keyword,text对于index只接受true/false值。分别代替not_analyzed/no。SpringDataEs中，index默认=true。      
所以原有的索引字段，需要根据索引特征进行修改，否则会编译错误。如果要精确搜索就用keyword，否则用text。如果不需要通过该字段进行查询，则index设置false即可。              

#### 引入log4j三方包  
服务中需要显示log4j-core, log4j-api包。否则会启动异常。

```
    ext.log4jCore = "org.apache.logging.log4j:log4j-core:2.10.0"
    ext.log4jApi = "org.apache.logging.log4j:log4j-api:2.10.0"  
```  

#### ClassNotFound-SimpleElasticsearchMappingContext 
需要显示引入spring-data-commons。SimpleElasticsearchMappingContext 依赖的**org.springframework.data.mapping.model.Property**需要spring-data-commons的2.x版本  

```
ext.springDataCommon = "org.springframework.data:spring-data-commons:2.0.2.RELEASE"   
```

#### 分词器相关报错  
如果在2.x升级到5.x的过程。使用分词器的document的索引会引起异常：    

```
failed to load elasticsearch nodes : org.elasticsearch.index.mapper.MapperParsingException: analyzer [ik] not found for field
```
解决方式：先关掉相关的索引，然后修改对应的settings的analyzer，最后再开启索引。示例代码如下：             

```

curl -XPOST '127.0.0.1:9200/items/_close?pretty'



curl -XPUT '127.0.0.1:9200/items/_settings' -d '{
    "analysis": {
        "analyzer": {
            "ik": {
                "type": "custom",
                "tokenizer": "ik_smart"
            }
        }
    }
}'


curl -XPOST '127.0.0.1:9200/items/_open?pretty'
```     

### Springboot2及相关插件升级        

#### Gradle升级到4.4.1    

使用Gradle构建工程的项目，如果是4.2以下版本的也需要升级。因为Springboot2.x的plugin需要gradle 4.2以上的版本。否则启动时会报错。           
如果idea在build工程时还是报错"Could not get unknown property 'projectConfiguration' for DefaultProjectDependency"，可以更新最新版的idea。支持更高级别的gradle。    
目前的idea版本信息：
```
IntelliJ IDEA 2017.3.3 (Community Edition)
Build #IC-173.4301.25, built on January 16, 2018
JRE: 1.8.0_152-release-1024-b11 x86_64 
```    
更新后问题解决。具体issue: [gradle-2936](https://github.com/gradle/gradle/issues/2936)     
#### SpringBoot2.x               

SpringBoot1.5.x的版本不支持ElasticSearch 5.x。所以需要升级项目到Spring Boot 2。目前Spring Boot 2只有milestone版本。本文示例选择了2.0.0.M2。           
相应的 SpringBootGradle 插件也需升级到2.0.0.M2,版本示例见：[spring-boot-s-new-gradle-plugin](https://spring.io/blog/2017/04/05/spring-boot-s-new-gradle-plugin)   

#### SpringCore升级到5.0.x

相应的，原有工程的Spring版本也需要升级到5.x版本。本示例升级到了5.0.2.RELEASE。   


#### SpringCloud升级
如果项目中使用了[Spring cloud](http://projects.spring.io/spring-cloud/)。也需要随着Springboot升级到符合的版本如Eureka,Feign,Ribbon 可以对应到: 2.0.0.M2。      


文章链接[SpringData3.x以及SpringBoot2集成Elasticsearch5.x](http://blog.csdn.net/lijingyao8206/article/details/79170183)


