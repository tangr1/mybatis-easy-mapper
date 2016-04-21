package org.easymapper.sample.mapper;

import org.apache.ibatis.annotations.*;
import org.apache.ibatis.session.RowBounds;
import org.easymapper.annotation.ProviderConfig;
import org.easymapper.mapper.CommonSQLProvider;
import org.easymapper.sample.domain.Product;

import java.util.List;
import java.util.Map;

public interface ProductMapper {

    @InsertProvider(type = SQLProvider.class, method = "create")
    @Options(useGeneratedKeys = true, keyColumn = "id", keyProperty = "id", flushCache = Options.FlushCachePolicy.TRUE)
    int create(Product value);

    @UpdateProvider(type = SQLProvider.class, method = "update")
    @Options(flushCache = Options.FlushCachePolicy.TRUE)
    int update(Product value);

    @SelectProvider(type = SQLProvider.class, method = "findAll")
    List<Product> findAll(RowBounds rowBounds);

    @SelectProvider(type = SQLProvider.class, method = "countAll")
    int countAll();

    @SelectProvider(type = SQLProvider.class, method = "findById")
    Product findById(Long id);

    @SelectProvider(type = SQLProvider.class, method = "findByIds")
    List<Product> findByIds(@Param("ids") List<Long> ids);

    @SelectProvider(type = SQLProvider.class, method = "findByMultiFields")
    List<Product> findByMultiFields(@Param("select") Map<String, Object> select, RowBounds rowBounds);

    @SelectProvider(type = SQLProvider.class, method = "findByMultiFields")
    List<Product> findByMultiFieldsWithSort(@Param("select") Map<String, Object> select, @Param("sort") String sort,
                                            @Param("direction") String direction, RowBounds rowBounds);

    @SelectProvider(type = SQLProvider.class, method = "countByMultiFields")
    int countByMultiFields(@Param("select") Map<String, Object> select);

    @DeleteProvider(type = SQLProvider.class, method = "deleteById")
    @Options(flushCache = Options.FlushCachePolicy.TRUE)
    int deleteById(Long id);

    @ProviderConfig(entity = Product.class)
    class SQLProvider extends CommonSQLProvider {
    }
}
