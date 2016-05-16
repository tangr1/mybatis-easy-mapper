MyBatis Easy Mapper
----

MyBatis在写mapper的时候，需要大量手写的XML，个人一直不太喜欢，因为毕竟大家是写Java而不是写XML，其维护性和扩展性都比较差。MyBatis官方有两种办法去解决这个问题：

* 使用[MyBatis Generator](www.mybatis.org/generator/)去自动生成XML
* 使用[Mybatis SQL Builder](http://www.mybatis.org/mybatis-3/statement-builders.html)通过Java代码写SQL

前者虽然很方便无需手写，但毕竟还是XML，而且每次改动都要重新生成；后者功能相对简单，只提供了几个关键的SQL函数，所以使用者还是需要写很多拼接SQL的boilerplate代码。

目前有一款比较流行的[通用mapper](https://github.com/abel533/Mapper)，个人也是在写了一些代码后找到了这款mapper，和自己的很多想法都不谋而合，而这款mapper也很强大，单表该有的功能都有了。本来也是准备直接用这个mapper，但目前的项目要支持多表join和其他一些需求，因此把这款mapper改造了一番：

* 加上多表Join支持
* 大幅度简化，原来的版本有几种使用方式，这里只支持一种最常见的。原来支持多种数据库，这里也只支持MySQL。
