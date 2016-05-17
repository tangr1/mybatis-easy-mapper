package com.github.tangr1.easymapper;

import org.apache.ibatis.annotations.*;
import org.apache.ibatis.session.RowBounds;
import com.github.tangr1.easymapper.internal.SQLProvider;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface EasyMapper<T> {
    @InsertProvider(type = SQLProvider.class, method = "insert")
    @Options(useGeneratedKeys = true, keyColumn = "id", keyProperty = "id", flushCache = Options.FlushCachePolicy.TRUE)
    int insert(T record);

    @SelectProvider(type = SQLProvider.class, method = "selectOne")
    T selectOne(@Param("condition") T condition);

    @SelectProvider(type = SQLProvider.class, method = "select")
    List<T> select(@Param("condition") T condition, RowBounds rowBounds);

    @SelectProvider(type = SQLProvider.class, method = "select")
    List<T> selectPageable(@Param("condition") T condition, @Param("pageable") Pageable pageable);

    @SelectProvider(type = SQLProvider.class, method = "count")
    int count(@Param("condition") T condition);

    @SelectProvider(type = SQLProvider.class, method = "selectByCriteria")
    List<T> selectByCriteria(@Param("criteria") Criteria criteria, RowBounds rowBounds);

    @SelectProvider(type = SQLProvider.class, method = "selectByCriteria")
    List<T> selectPageableByCriteria(@Param("criteria") Criteria criteria, @Param("pageable") Pageable pageable);

    @SelectProvider(type = SQLProvider.class, method = "countByCriteria")
    int countByCriteria(@Param("criteria") Criteria criteria);

    @UpdateProvider(type = SQLProvider.class, method = "updateByPrimaryKey")
    @Options(flushCache = Options.FlushCachePolicy.TRUE)
    int updateByPrimaryKey(@Param("record") T record);

    @UpdateProvider(type = SQLProvider.class, method = "update")
    @Options(flushCache = Options.FlushCachePolicy.TRUE)
    int update(@Param("record") T record, @Param("condition") T condition);

    @UpdateProvider(type = SQLProvider.class, method = "updateByCriteria")
    @Options(flushCache = Options.FlushCachePolicy.TRUE)
    int updateByCriteria(@Param("record") T record, @Param("criteria") Criteria criteria);

    @DeleteProvider(type = SQLProvider.class, method = "delete")
    @Options(flushCache = Options.FlushCachePolicy.TRUE)
    int delete(@Param("condition") T condition);

    @DeleteProvider(type = SQLProvider.class, method = "deleteByCriteria")
    @Options(flushCache = Options.FlushCachePolicy.TRUE)
    int deleteByCriteria(@Param("criteria") Criteria criteria);
}