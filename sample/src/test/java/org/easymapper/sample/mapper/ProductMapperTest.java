package org.easymapper.sample.mapper;

import org.easymapper.sample.domain.Company;
import org.easymapper.sample.domain.Product;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ProductMapperTest extends BaseTest {
    @Test
    public void crudTest() {
        Company company = getCompany();
        companyMapper.create(company);
        productMapper.create(getProduct(company.getId()));
        productMapper.create(getProduct(company.getId()));
        List<Product> products = productMapper.findAll(rowBounds);
        assertThat(products).hasSize(2);
        assertThat(productMapper.countAll()).isEqualTo(2);
        Product product = products.get(0);
        assertThat(product.getCompanyName()).isEqualTo(company.getName());
        product.setName("new name");
        productMapper.update(product);
        product = productMapper.findById(product.getId());
        assertThat(product.getName()).isEqualTo("new name");
        assertThat(productMapper.findByIds(null)).isEmpty();
        List<Long> ids = new ArrayList<>();
        assertThat(productMapper.findByIds(ids)).isEmpty();
        ids.add(products.get(0).getId());
        ids.add(products.get(1).getId());
        assertThat(productMapper.findByIds(ids)).hasSize(2);
    }
}
