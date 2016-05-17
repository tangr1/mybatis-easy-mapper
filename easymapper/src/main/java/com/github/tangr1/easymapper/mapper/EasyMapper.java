package com.github.tangr1.easymapper.mapper;

import org.apache.ibatis.annotations.*;
import org.apache.ibatis.session.RowBounds;
import com.github.tangr1.easymapper.mapper.internal.SQLProvider;

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

    @SelectProvider(type = SQLProvider.class, method = "selectByCriteria")
    List<T> selectAllByCriteria(@Param("criteria") Criteria criteria);

    @SelectProvider(type = SQLProvider.class, method = "selectByCriteria")
    List<T> selectByCriteria(@Param("criteria") Criteria criteria, RowBounds rowBounds);

    @UpdateProvider(type = SQLProvider.class, method = "updateByPrimaryKey")
    @Options(flushCache = Options.FlushCachePolicy.TRUE)
    int updateByPrimaryKey(T record);

    @UpdateProvider(type = SQLProvider.class, method = "update")
    @Options(flushCache = Options.FlushCachePolicy.TRUE)
    int update(@Param("record") T record, @Param("condition") T condition);

    @UpdateProvider(type = SQLProvider.class, method = "updateByCriteria")
    @Options(flushCache = Options.FlushCachePolicy.TRUE)
    int updateByCriteria(@Param("record") T record, @Param("criteria") Criteria criteria);

    @DeleteProvider(type = SQLProvider.class, method = "delete")
    @Options(flushCache = Options.FlushCachePolicy.TRUE)
    int delete(T record);

    @DeleteProvider(type = SQLProvider.class, method = "deleteByCriteria")
    @Options(flushCache = Options.FlushCachePolicy.TRUE)
    int deleteByCriteria(@Param("criteria") Criteria criteria);
}