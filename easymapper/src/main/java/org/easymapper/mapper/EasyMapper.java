package org.easymapper.mapper;

import org.apache.ibatis.annotations.*;
import org.apache.ibatis.session.RowBounds;

import java.util.List;

public interface EasyMapper<T> {
    @InsertProvider(type = SQLProvider.class, method = "insert")
    @Options(useGeneratedKeys = true, keyColumn = "id", keyProperty = "id", flushCache = Options.FlushCachePolicy.TRUE)
    int insert(T record);

    @SelectProvider(type = SQLProvider.class, method = "selectOne")
    T selectOne(T condition);

    @SelectProvider(type = SQLProvider.class, method = "select")
    List<T> selectAll(T condition);

    @SelectProvider(type = SQLProvider.class, method = "select")
    List<T> select(T condition, RowBounds rowBounds);

    @SelectProvider(type = SQLProvider.class, method = "count")
    int count(T condition);

    @SelectProvider(type = SQLProvider.class, method = "selectByExample")
    List<T> selectAllByExample(Example example);

    @SelectProvider(type = SQLProvider.class, method = "selectByExample")
    List<T> selectByExample(Example example, RowBounds rowBounds);

    @UpdateProvider(type = SQLProvider.class, method = "updateByPrimaryKey")
    @Options(flushCache = Options.FlushCachePolicy.TRUE)
    int updateByPrimaryKey(T record);

    @UpdateProvider(type = SQLProvider.class, method = "update")
    @Options(flushCache = Options.FlushCachePolicy.TRUE)
    int update(@Param("record") T record, @Param("condition") T condition);

    @UpdateProvider(type = SQLProvider.class, method = "updateByExample")
    @Options(flushCache = Options.FlushCachePolicy.TRUE)
    int updateByExample(@Param("record") T record, @Param("example") Example example);

    @DeleteProvider(type = SQLProvider.class, method = "delete")
    @Options(flushCache = Options.FlushCachePolicy.TRUE)
    int delete(T record);

    @DeleteProvider(type = SQLProvider.class, method = "deleteByExample")
    @Options(flushCache = Options.FlushCachePolicy.TRUE)
    int deleteByExample(Example example);
}